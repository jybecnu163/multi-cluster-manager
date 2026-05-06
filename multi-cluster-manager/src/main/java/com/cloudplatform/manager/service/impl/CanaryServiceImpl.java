package com.cloudplatform.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cloudplatform.manager.dto.CanaryTaskCreateRequest;
import com.cloudplatform.manager.dto.CanaryTaskDetail;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.integration.CanaryNginxIntegration;
import com.cloudplatform.manager.integration.KubernetesClientFactory;
import com.cloudplatform.manager.mapper.ClusterMapper;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import com.cloudplatform.manager.service.ApprovalService;
import com.cloudplatform.manager.service.AuditService;
import com.cloudplatform.manager.service.CanaryProxyService;
import com.cloudplatform.manager.service.CanaryService;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class CanaryServiceImpl implements CanaryService {
    @Autowired
    private DeploymentTaskMapper taskRepository;
    @Autowired
    private ServiceInstanceMapper serviceInstanceRepository;
    @Autowired
    private KubernetesClientFactory clientFactory;
    @Autowired
    private CanaryNginxIntegration nginxIntegration;
    @Autowired
    private ApprovalService approvalService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private CanaryProxyService proxyService;
    @Value("${app.canary.auto-pause.error-rate-threshold:5}")
    private int errorRateThreshold;
    @Value("${app.canary.auto-pause.min-qps:10}")
    private int minQps;
    @Autowired
    private ClusterMapper clusterMapper;

    @Transactional
    @Override
    public CanaryTaskDetail createTask(CanaryTaskCreateRequest request, Long userId) {
        // 1. 校验服务存在及权限（略，权限由Controller控制）
        ServiceInstance service = serviceInstanceRepository.selectById(request.getServiceInstanceId());
        if (service == null) throw new BusinessException("Service not found", 404);

        // 2. 创建任务记录
        DeploymentTask task = new DeploymentTask();
        task.setServiceInstanceId(request.getServiceInstanceId());
        task.setTaskType("canary");
        task.setStatus("internal_test");
        task.setCurrentStage(0);

        task.setStrategyJson(JSON.toJSONString(request.getStrategy()));
        task.setTargetImage(request.getTargetImage());
        task.setCreatedBy(userId);
        task.setApprovalTimeoutHours(24);
        task.setCreatedAt(Instant.now());

        taskRepository.insert(task);

        // 3. 创建金丝雀 Deployment（无流量）
        createCanaryDeployment(service, request.getTargetImage(), request.getStrategy().getCanaryReplicas());

        // 4. 审计日志
        auditService.log("CREATE_CANARY_TASK", "DeploymentTask", task.getId(), Map.of("image", request.getTargetImage()));

        return toDetail(task, 0);
    }

    @Override
    public CanaryTaskDetail getTaskDetail(Long taskId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        int currentWeight = getCurrentCanaryWeight(task);
        return toDetail(task, currentWeight);
    }

    @Transactional
    @Override
    public void promoteStage(Long taskId, String stage, Long userId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        if (task.getStatus().equals("paused")) throw new BusinessException("Task is paused, resume first", 400);
        if (task.getStatus().equals("rolled_back") || task.getStatus().equals("success") || task.getStatus().equals("failed"))
            throw new BusinessException("Task already finished", 400);

        switch (stage) {
            case "internal_tested":
                // 内部测试完成，标记为 internal_tested，但无需推进，仅前端记录
                break;
            case "request_traffic":
                if (!task.getStatus().equals("internal_test"))
                    throw new BusinessException("Invalid stage", 400);
                // 若自动审批
                CanaryTaskCreateRequest.CanaryStrategy strategy = parseStrategy(task);
                if (Boolean.TRUE.equals(strategy.getAutoApproveTraffic())) {
                    // 直接进入小流量
                    applyTrafficWeight(task, 5);
                    task.setStatus("traffic_5");
                    task.setCurrentStage(3);
                } else {
                    // 创建审批单
                    Long approvalId = approvalService.createApproval(taskId, userId, "部门主管", task.getApprovalTimeoutHours());
                    task.setStatus("waiting_approval");
                    task.setCurrentStage(2);
                    // 存储 approvalId 可在扩展字段
                }
                break;
            case "promote_25":
                if (!task.getStatus().equals("traffic_5"))
                    throw new BusinessException("Not in traffic_5 stage", 400);
                applyTrafficWeight(task, 25);
                task.setStatus("traffic_25");
                task.setCurrentStage(4);
                break;
            case "promote_100":
                if (!task.getStatus().equals("traffic_25"))
                    throw new BusinessException("Not in traffic_25 stage", 400);
                applyTrafficWeight(task, 100);
                task.setStatus("traffic_100");
                task.setCurrentStage(5);
                // 全量后，将稳定版更新为新镜像
                promoteStableDeployment(task);
                // 删除金丝雀 Deployment
                deleteCanaryDeployment(task);
                task.setStatus("success");
                task.setCompletedAt(Instant.now());
                break;
            default:
                throw new BusinessException("Unknown stage", 400);
        }
        taskRepository.updateById(task);
        auditService.log("PROMOTE_CANARY_STAGE", "DeploymentTask", taskId, Map.of("stage", stage));
    }

    @Transactional
    @Override
    public void rollback(Long taskId, Long userId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        // 回滚：删除金丝雀 Deployment，恢复稳定版（如果已覆盖则重新部署旧镜像）
        deleteCanaryDeployment(task);
        // 关闭 canary 注解
        removeCanaryWeight(task);
        task.setStatus("rolled_back");
        task.setCompletedAt(Instant.now());
        taskRepository.updateById(task);
        auditService.log("ROLLBACK_CANARY", "DeploymentTask", taskId, null);
    }

    @Transactional
    @Override
    public void resume(Long taskId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        if (!task.getStatus().equals("paused")) throw new BusinessException("Task not paused", 400);
        // 恢复从之前的阶段继续（例如 traffic_5）
        task.setStatus(task.getStatus().equals("paused") ? task.getStatus().replace("paused", "traffic_5") : task.getStatus());
        taskRepository.updateById(task);
    }

    @Override
    public void autoPauseIfErrorRateHigh(Long taskId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) return;
        if (!(task.getStatus().equals("traffic_5") || task.getStatus().equals("traffic_25"))) return;
        double errorRate = queryErrorRateFromPrometheus(task);
        long qps = queryQpsFromPrometheus(task);
        if (qps >= minQps && errorRate > errorRateThreshold) {
            // 自动暂停
            task.setStatus("paused");
            taskRepository.updateById(task);
            // 发送通知（略）
            auditService.log("CANARY_AUTO_PAUSE", "DeploymentTask", taskId, Map.of("errorRate", errorRate, "qps", qps));
        }
    }

    // ========== 辅助方法 ==========
    private void createCanaryDeployment(ServiceInstance service, String image, int replicas) {
        // fabric8 创建 Deployment
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId())); // 需实现
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(service.getWorkloadName() + "-canary")
                .withNamespace(service.getNamespace())
                .addToLabels("app", service.getName())
                .addToLabels("version", "canary")
                .endMetadata()
                .withNewSpec()
                .withReplicas(replicas)
                .withNewSelector()
                .addToMatchLabels("app", service.getName())
                .addToMatchLabels("version", "canary")
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", service.getName())
                .addToLabels("version", "canary")
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(service.getWorkloadName())
                .withImage(image) // 关键：设置镜像地址
                .withImagePullPolicy("IfNotPresent") // 优先本地
                .addNewPort()
                .withContainerPort(8080)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        client.apps().deployments().inNamespace(service.getNamespace()).create(deployment);
    }

    private void applyTrafficWeight(DeploymentTask task, int weight) {
        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
        // 获取 Ingress 名称（假设通过注解标记）
        String ingressName = service.getWorkloadName() + "-ingress";
        nginxIntegration.setCanaryWeight(service.getNamespace(), ingressName, service.getWorkloadName() + "-canary", weight);
    }

    private void removeCanaryWeight(DeploymentTask task) {
        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
        nginxIntegration.removeCanaryAnnotations(service.getNamespace(), service.getWorkloadName() + "-ingress");
    }

    private void deleteCanaryDeployment(DeploymentTask task) {
        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId()));
        client.apps().deployments().inNamespace(service.getNamespace()).withName(service.getWorkloadName() + "-canary").delete();
    }

    private void promoteStableDeployment(DeploymentTask task) {
        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId()));
        Deployment stable = client.apps().deployments().inNamespace(service.getNamespace()).withName(service.getWorkloadName()).get();
        if (stable != null) {
            stable.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(task.getTargetImage());
            client.apps().deployments().inNamespace(service.getNamespace()).createOrReplace(stable);
        }
    }

    private int getCurrentCanaryWeight(DeploymentTask task) {
        // 查询 Ingress 注解获取权重
        return 0; // 简化
    }

    private CanaryTaskCreateRequest.CanaryStrategy parseStrategy(DeploymentTask task) {
        return (CanaryTaskCreateRequest.CanaryStrategy) JSON.parseObject(task.getStrategyJson().toString(), CanaryTaskCreateRequest.CanaryStrategy.class);
    }

    public double queryErrorRateFromPrometheus(DeploymentTask task) {
        // 调用 Prometheus API 查询金丝雀 Pod 5xx 比例  todo
        return 0.0D;
    }

    public long queryQpsFromPrometheus(DeploymentTask task) {
        return 0L;
    }

    private CanaryTaskDetail toDetail(DeploymentTask task, int weight) {
        CanaryTaskDetail detail = new CanaryTaskDetail();
        detail.setId(task.getId());
        detail.setServiceInstanceId(task.getServiceInstanceId());
        detail.setStatus(task.getStatus());
        detail.setCurrentStage(task.getCurrentStage());
        detail.setTargetImage(task.getTargetImage());
        detail.setCanaryWeight(weight);
        detail.setApprovalTimeoutHours(task.getApprovalTimeoutHours());
        detail.setCreatedAt(task.getCreatedAt());
        return detail;
    }
}