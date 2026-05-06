package com.cloudplatform.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cloudplatform.manager.dto.BatchTaskCreateRequest;
import com.cloudplatform.manager.dto.BatchTaskDetail;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.integration.KubernetesRolloutManager;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;//DeploymentTaskRepository;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;//.repository.ServiceInstanceRepository;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import com.cloudplatform.manager.service.ApprovalService;
import com.cloudplatform.manager.service.AuditService;
import com.cloudplatform.manager.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class BatchServiceImpl implements BatchService {
    @Autowired
    private DeploymentTaskMapper taskRepository;
    @Autowired
    private ServiceInstanceMapper serviceInstanceRepository;
    @Autowired
    private KubernetesRolloutManager rolloutManager;
    @Autowired
    private ApprovalService approvalService;
    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public BatchTaskDetail createTask(BatchTaskCreateRequest request, Long userId) {
        ServiceInstance service = serviceInstanceRepository.selectById(request.getServiceInstanceId());
        if (service == null) throw new BusinessException("Service not found", 404);

        int totalReplicas = service.getReplicas();
        BatchTaskCreateRequest.BatchConfig config = request.getBatchConfig();

        // 计算总批次数
        int batchSize = config.getBatchSizeType().equals("count") ? config.getBatchValue() :
                (int) Math.ceil(totalReplicas * config.getBatchValue() / 100.0);
        if (batchSize > 100) throw new BusinessException("Single batch cannot exceed 100 replicas", 400);
        if (totalReplicas > 500) {
            // 可选警告，不阻断
            auditService.log("BATCH_WARNING", "ServiceInstance", service.getId(), Map.of("warning", "Total replicas >500, consider blue-green"));
        }
        int totalBatches = (int) Math.ceil((double) totalReplicas / batchSize);

        // 创建任务记录
        DeploymentTask task = new DeploymentTask();
        task.setServiceInstanceId(request.getServiceInstanceId());
        task.setTaskType("batch");
        task.setStatus("pending_approval");  // 生产环境需审批
        task.setCurrentStage(0);
        task.setStrategyJson(JSON.toJSONString(config));
        task.setTargetImage(request.getTargetImage());
        task.setCreatedBy(userId);
        task.setApprovalTimeoutHours(24);
        task.setCreatedAt(Instant.now());
        taskRepository.insert(task);

        // 如果是生产环境且需审批，创建审批单
        if ("prod".equals(service.getEnvType())) {
            Long approvalId = approvalService.createApproval(task.getId(), userId, "部门主管", 24);
            task.setStatus("waiting_approval");
            taskRepository.updateById(task);
        } else {
            // 非生产环境直接开始第一批
            startNextBatch(task, 1, batchSize, totalBatches);
            task.setStatus("in_progress");
            taskRepository.updateById(task);
        }

        auditService.log("CREATE_BATCH_TASK", "DeploymentTask", task.getId(), Map.of("batches", totalBatches));
        return toDetail(task, totalBatches);
    }

//    @Override
//    public BatchTaskDetail getTaskDetail(Long taskId) {
//        DeploymentTask task = taskRepository.selectById(taskId);
//        if (task == null) throw new BusinessException("Task not found", 404);
//        BatchTaskCreateRequest.BatchConfig config
//                = JSON.parseObject(task.getStrategyJson(), BatchTaskCreateRequest.BatchConfig.class);
//
//        int totalBatches = calculateTotalBatches(task.getServiceInstanceId(), config);
//        return toDetail(task, totalBatches);
//    }

    @Override
    public BatchTaskDetail getTaskDetail(Long taskId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);

        // strategyJson 实际存储的是 LinkedHashMap，需要转为 JSON 字符串再解析
        String jsonStr = JSON.toJSONString(task.getStrategyJson());
        BatchTaskCreateRequest.BatchConfig config = JSON.parseObject(jsonStr, BatchTaskCreateRequest.BatchConfig.class);

        int totalBatches = calculateTotalBatches(task.getServiceInstanceId(), config);
        return toDetail(task, totalBatches);
    }

    @Override
    @Transactional
    public void nextBatch(Long taskId, Long userId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        if (!task.getStatus().equals("in_progress") && !task.getStatus().equals("waiting_confirmation")) {
            throw new BusinessException("Task not in progress", 400);
        }
//        BatchTaskCreateRequest.BatchConfig config = JSON.parseObject(task.getStrategyJson(), BatchTaskCreateRequest.BatchConfig.class);

        String jsonStr = JSON.toJSONString(task.getStrategyJson());
        BatchTaskCreateRequest.BatchConfig config = JSON.parseObject(jsonStr, BatchTaskCreateRequest.BatchConfig.class);

        int totalBatches = calculateTotalBatches(task.getServiceInstanceId(), config);
        int currentBatch = task.getCurrentStage() + 1;
        if (currentBatch > totalBatches) {
            // 已完成
            task.setStatus("success");
            task.setCompletedAt(Instant.now());
            taskRepository.updateById(task);
            auditService.log("BATCH_COMPLETED", "DeploymentTask", taskId, null);
            return;
        }

        startNextBatch(task, currentBatch, getBatchSize(config, task.getServiceInstanceId()), totalBatches);
        task.setCurrentStage(currentBatch);
        task.setStatus("in_progress");
        taskRepository.updateById(task);
        auditService.log("BATCH_START", "DeploymentTask", taskId, Map.of("batch", currentBatch));
    }

    @Override
    @Transactional
    public void rollback(Long taskId, Long userId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
        if (service.getWorkloadType().equals("Deployment")) {
            rolloutManager.rollbackDeployment(service);
        } else {
            rolloutManager.rollbackStatefulSet(service);
        }
        task.setStatus("rolled_back");
        task.setCompletedAt(Instant.now());
        taskRepository.updateById(task);
        auditService.log("BATCH_ROLLBACK", "DeploymentTask", taskId, null);
    }

    @Override
    @Transactional
    public void resume(Long taskId, Long userId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) throw new BusinessException("Task not found", 404);
        if (!task.getStatus().equals("paused")) throw new BusinessException("Task not paused", 400);
        task.setStatus("in_progress");
        taskRepository.updateById(task);
        // 继续当前批次（可能已部分失败，重新执行）
        // 简化：直接重新执行当前批次
        // 详细实现略
    }

    @Override
    public void autoRollbackIfNeeded(Long taskId) {
        DeploymentTask task = taskRepository.selectById(taskId);
        if (task == null) return;
        if (!task.getStatus().equals("in_progress")) return;

        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
//        BatchTaskCreateRequest.BatchConfig config = JSON.parseObject(task.getStrategyJson(), BatchTaskCreateRequest.BatchConfig.class);
        String jsonStr = JSON.toJSONString(task.getStrategyJson());
        BatchTaskCreateRequest.BatchConfig config = JSON.parseObject(jsonStr, BatchTaskCreateRequest.BatchConfig.class);

        BatchTaskCreateRequest.BatchConfig.FailureCondition fc = config.getFailureCondition();
        int readyReplicas = rolloutManager.getReadyReplicas(service);
        int expectedReplicas = Math.min(service.getReplicas(), task.getCurrentStage() * getBatchSize(config, task.getServiceInstanceId()));
        double readyPercent = (double) readyReplicas / expectedReplicas * 100;
        if (readyPercent < fc.getMinReadyPercent()) {
            // 触发回滚
            rollback(taskId, null);
            auditService.log("AUTO_ROLLBACK", "DeploymentTask", taskId, Map.of("reason", "ready_percent_low"));
        }
        // 错误日志增幅检查需要在日志系统查询，此处略
    }

    // ========== 辅助方法 ==========
    private void startNextBatch(DeploymentTask task, int batchNumber, int batchSize, int totalBatches) {
        ServiceInstance service = serviceInstanceRepository.selectById(task.getServiceInstanceId());
        if (service.getWorkloadType().equals("Deployment")) {
            rolloutManager.updateDeploymentBatch(service, task.getTargetImage(), batchNumber, totalBatches, batchSize);
        } else {
            // StatefulSet 使用 partition 参数：partition = totalReplicas - (batchNumber * batchSize)
            int totalReplicas = service.getReplicas();
            int newPartition = totalReplicas - (batchNumber * batchSize);
            if (newPartition < 0) newPartition = 0;
            rolloutManager.updateStatefulSetPartition(service, task.getTargetImage(), newPartition);
        }
    }

    private int getBatchSize(BatchTaskCreateRequest.BatchConfig config, Long serviceInstanceId) {
        ServiceInstance service = serviceInstanceRepository.selectById(serviceInstanceId);
        int totalReplicas = service.getReplicas();
        if (config.getBatchSizeType().equals("count")) {
            return config.getBatchValue();
        } else {
            return (int) Math.ceil(totalReplicas * config.getBatchValue() / 100.0);
        }
    }

    private int calculateTotalBatches(Long serviceInstanceId, BatchTaskCreateRequest.BatchConfig config) {
        ServiceInstance service = serviceInstanceRepository.selectById(serviceInstanceId);
        int totalReplicas = service.getReplicas();
        int batchSize = getBatchSize(config, serviceInstanceId);
        return (int) Math.ceil((double) totalReplicas / batchSize);
    }

    private BatchTaskDetail toDetail(DeploymentTask task, int totalBatches) {
        BatchTaskDetail detail = new BatchTaskDetail();
        detail.setId(task.getId());
        detail.setStatus(task.getStatus());
        detail.setTotalBatches(totalBatches);
        detail.setCurrentBatch(task.getCurrentStage());
        detail.setCreatedAt(task.getCreatedAt());
        return detail;
    }
}