package com.cloudplatform.manager.service.impl;

import com.cloudplatform.manager.dto.ManualScaleRequest;
import com.cloudplatform.manager.dto.ScaleResponse;
import com.cloudplatform.manager.integration.K8sClientManager;
import com.cloudplatform.manager.mapper.AutoScalingPolicyMapper;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;
import com.cloudplatform.manager.model.entity.AutoScalingPolicy;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.service.ApprovalService;
import com.cloudplatform.manager.service.ManualScalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualScalingServiceImpl implements ManualScalingService {

    private final ServiceInstanceMapper serviceInstanceMapper;
    private final K8sClientManager k8sClientManager;
    private final ApprovalService approvalService;
    private final DeploymentTaskMapper deploymentTaskMapper;
    private final AutoScalingPolicyMapper autoScalingPolicyMapper;

    @Override
    @Transactional
    public ScaleResponse scale(Long serviceId, ManualScaleRequest request, Long userId) {
        ServiceInstance inst = serviceInstanceMapper.selectById(serviceId);
        if (inst == null) {
            throw new RuntimeException("Service instance not found");
        }

        // 检查是否需要审批
        boolean needApproval = false;
        if ("prod".equals(inst.getEnvType())) {
            // 生产环境：如果用户是运维工程师且部门开启了免批且请求中 ignore_approval=true，则可直接执行
            boolean opsBypass = false;
            // 通过部门设置查询（需注入 DepartmentSettingsMapper，简化逻辑）
            // 假设已有权限组件判断：用户角色为运维 && 部门设置允许
            if (request.getIgnoreApproval() && isOpsBypassEnabled(inst.getDepartmentId(), userId)) {
                opsBypass = true;
            }
            if (!opsBypass) {
                needApproval = true;
                // 创建审批单
                Long approvalId = approvalService.createApproval(serviceId, getDepartmentDirector(inst.getDepartmentId()), 24);
                // 创建任务记录
                DeploymentTask task = new DeploymentTask();
                task.setServiceInstanceId(serviceId);
                task.setTaskType("scale");
                task.setStatus("waiting_approval");
                task.setTargetImage(null);
                task.setStrategyJson(null);
                task.setCreatedBy(userId);
                task.setCreatedAt(Instant.from(LocalDateTime.now()));
                deploymentTaskMapper.insert(task);
                return new ScaleResponse(task.getId(), true);
            }
        }

        // 无需审批，直接执行扩缩容
        executeScale(inst, request.getTargetReplicas(), userId);
        return new ScaleResponse(null, false);
    }

    private void executeScale(ServiceInstance inst, int targetReplicas, Long userId) {
        k8sClientManager.scaleWorkload(inst.getClusterId(), inst.getNamespace(),
                inst.getWorkloadName(), inst.getWorkloadType(), targetReplicas);
        // 更新本地副本数记录
        inst.setReplicas(targetReplicas);
        serviceInstanceMapper.updateById(inst);
        log.info("Manual scaling performed on service {} to {} replicas by user {}", inst.getId(), targetReplicas, userId);
        // 产生审计日志（异步）
    }

    private boolean isOpsBypassEnabled(Long departmentId, Long userId) {
        // 实际需查询 department_settings 和用户角色
        return false; // 简化实现
    }

    private Long getDepartmentDirector(Long departmentId) {
        // 查询部门主管ID
        return 1L;
    }
}
