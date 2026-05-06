package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.PipelineCreateRequest;
import com.cloudplatform.manager.dto.PipelineRunResponse;
import com.cloudplatform.manager.model.entity.Pipeline;

import java.util.List;

public interface PipelineService {
    Long createPipeline(PipelineCreateRequest request, Long userId);
    List<Pipeline> listPipelines();
    Long triggerPipeline(Long pipelineId, Long userId);
    PipelineRunResponse getRun(Long runId);
}