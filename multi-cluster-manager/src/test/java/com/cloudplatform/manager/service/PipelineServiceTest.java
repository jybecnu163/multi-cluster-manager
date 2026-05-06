package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.PipelineCreateRequest;
import com.cloudplatform.manager.mapper.PipelineMapper;
import com.cloudplatform.manager.mapper.PipelineRunMapper;
import com.cloudplatform.manager.model.entity.Pipeline;
import com.cloudplatform.manager.service.impl.PipelineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {
    @Mock
    private PipelineMapper pipelineRepository;
    @Mock
    private PipelineRunMapper runRepository;
    @Mock
    private PipelineExecutionService executionService;
    @InjectMocks
    private PipelineServiceImpl pipelineService;

    private PipelineCreateRequest request;

    @BeforeEach
    void setUp() {
        request = new PipelineCreateRequest();
        request.setName("test-pipeline");
        request.setTriggerType("api");
        request.setApprovalTimeoutHours(24);
        PipelineCreateRequest.Step step = new PipelineCreateRequest.Step();
        step.setType("deploy");
        step.setDeploymentMethod("canary");
        step.setTargetServiceId(1L);
        step.setImage("myapp:v1");
        request.setSteps(List.of(step));
    }

    @Test
    void createPipeline() {
        when(pipelineRepository.insert(any(Pipeline.class))).thenReturn(1);
        Long id = pipelineService.createPipeline(request, 1L);
        assertNotNull(id);
        verify(pipelineRepository, times(1)).insert(any());
    }

    @Test
    void triggerPipeline() {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(1L);
        when(pipelineRepository.selectById(1L)).thenReturn(pipeline);
        when(runRepository.insert(any())).thenReturn(1);
        Long runId = pipelineService.triggerPipeline(1L, 1L);
        assertNotNull(runId);
        verify(executionService, times(1)).executePipeline(anyLong());
    }
}