package com.cloudplatform.manager.service;

import com.cloudplatform.manager.integration.CanaryNginxIntegration;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.service.impl.CanaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanaryServiceTest {
    @Mock
    private DeploymentTaskMapper taskRepository;
    @Mock
    private ApprovalService approvalService;
    @Mock
    private CanaryNginxIntegration nginxIntegration;
    @InjectMocks
    private CanaryServiceImpl canaryService;

    private DeploymentTask task;

    @BeforeEach
    void setUp() {
        task = new DeploymentTask();
        task.setId(1L);
        task.setStatus("internal_test");
        task.setStrategyJson("{\"autoApproveTraffic\":false}");
    }

    @Test
    void requestTrafficCreatesApproval() {
        when(taskRepository.selectById(1L)).thenReturn(task);
        doNothing().when(approvalService).createApproval(anyLong(), anyLong(), anyString(), anyInt());
        canaryService.promoteStage(1L, "request_traffic", 100L);
        verify(approvalService, times(1)).createApproval(eq(1L), eq(100L), eq("部门主管"), anyInt());
        assertEquals("waiting_approval", task.getStatus());
    }

    @Test
    void autoPauseWhenErrorRateHigh() {
        task.setStatus("traffic_5");
        when(taskRepository.selectById(1L)).thenReturn(task);
        // 模拟查询错误率和QPS（通过反射或 spy）
        // 代码中实际会调用queryErrorRateFromPrometheus，我们需要 mock 该方法
        // 这里简化，直接验证调用后状态变为 paused
        // 由于方法内部调用私有方法，可通过 spy + when 实现
        CanaryServiceImpl spy = spy(canaryService);
        doReturn(10.0).when(spy).queryErrorRateFromPrometheus(any());
        doReturn(100L).when(spy).queryQpsFromPrometheus(any());
        spy.autoPauseIfErrorRateHigh(1L);
        assertEquals("paused", task.getStatus());
    }

    @Test
    void rollbackCleansUp() {
        when(taskRepository.selectById(1L)).thenReturn(task);
        canaryService.rollback(1L, 1L);
        assertEquals("rolled_back", task.getStatus());
        verify(nginxIntegration, times(1)).removeCanaryAnnotations(any(), any());
    }
}