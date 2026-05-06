package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.BatchTaskCreateRequest;
import com.cloudplatform.manager.dto.BatchTaskDetail;
import com.cloudplatform.manager.integration.KubernetesRolloutManager;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;//DeploymentTaskRepository;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.service.impl.BatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {
    @Mock
    private DeploymentTaskMapper taskRepository;
    @Mock
    private ServiceInstanceMapper serviceInstanceRepository;
    @Mock
    private ApprovalService approvalService;
    @Mock
    private KubernetesRolloutManager rolloutManager;
    @InjectMocks
    private BatchServiceImpl batchService;

    private BatchTaskCreateRequest validRequest;
    private DeploymentTask task;

    @BeforeEach
    void setUp() {
        validRequest = new BatchTaskCreateRequest();
        validRequest.setServiceInstanceId(1L);
        validRequest.setTargetImage("myapp:v2");
        BatchTaskCreateRequest.BatchConfig config = new BatchTaskCreateRequest.BatchConfig();
        config.setBatchSizeType("count");
        config.setBatchValue(10);
        config.setIntervalSeconds(30);
        config.setRequireConfirmation(true);
        BatchTaskCreateRequest.BatchConfig.FailureCondition fc = new BatchTaskCreateRequest.BatchConfig.FailureCondition();
        fc.setMinReadyPercent(80);
        config.setFailureCondition(fc);
        validRequest.setBatchConfig(config);

        task = new DeploymentTask();
        task.setId(1L);
        task.setStatus("pending_approval");
        task.setStrategyJson("{\"batchSizeType\":\"count\",\"batchValue\":10}");
    }

    @Test
    void createTaskForProdNeedsApproval() {
        com.cloudplatform.manager.model.entity.ServiceInstance service = new com.cloudplatform.manager.model.entity.ServiceInstance();
        service.setReplicas(50);
        service.setEnvType("prod");
        when(serviceInstanceRepository.selectById(1L)).thenReturn(service);
        when(taskRepository.insert(any(DeploymentTask.class))).thenReturn(1);
        when(approvalService.createApproval(anyLong(), anyLong(), anyString(), anyInt())).thenReturn(100L);
        BatchTaskDetail detail = batchService.createTask(validRequest, 1L);
        assertNotNull(detail);
        verify(approvalService, times(1)).createApproval(eq(1L), eq(1L), eq("部门主管"), eq(24));
    }

    @Test
    void rollbackSetsStatus() {
        when(taskRepository.selectById(1L)).thenReturn(task);
        when(serviceInstanceRepository.selectById(any())).thenReturn(new com.cloudplatform.manager.model.entity.ServiceInstance());
        batchService.rollback(1L, 1L);
        assertEquals("rolled_back", task.getStatus());
        verify(rolloutManager, times(1)).rollbackDeployment(any());
    }
}