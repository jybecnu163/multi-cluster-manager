package com.cloudplatform.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cloudplatform.manager.dto.PipelineCreateRequest;
import com.cloudplatform.manager.dto.PipelineRunResponse;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.mapper.PipelineMapper;
import com.cloudplatform.manager.mapper.PipelineRunMapper;
import com.cloudplatform.manager.model.entity.Pipeline;
import com.cloudplatform.manager.model.entity.PipelineRun;
import com.cloudplatform.manager.service.AuditService;
import com.cloudplatform.manager.service.PipelineExecutionService;
import com.cloudplatform.manager.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PipelineServiceImpl implements PipelineService {
    @Autowired
    private PipelineMapper pipelineRepository;
    @Autowired
    private PipelineRunMapper runRepository;
    @Autowired
    private PipelineExecutionService executionService;
    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public Long createPipeline(PipelineCreateRequest request, Long userId) {
        Pipeline pipeline = new Pipeline();
        pipeline.setName(request.getName());
        pipeline.setSteps(JSON.toJSONString(request.getSteps()));
        pipeline.setTriggerType(request.getTriggerType());
        if ("webhook".equals(request.getTriggerType()) && request.getWebhookSecret() != null) {
            pipeline.setWebhookSecret(request.getWebhookSecret());
        }
        pipeline.setApprovalTimeoutHours(request.getApprovalTimeoutHours());
        pipeline.setCreatedBy(userId);
        pipeline.setCreatedAt(Instant.now());
        pipelineRepository.insert(pipeline);
        auditService.log("CREATE_PIPELINE", "Pipeline", pipeline.getId(), null);
        return pipeline.getId();
    }

    @Override
    public List<Pipeline> listPipelines() {
        return pipelineRepository.selectList(null);
    }

    @Override
    @Transactional
    public Long triggerPipeline(Long pipelineId, Long userId) {
        Pipeline pipeline = pipelineRepository.selectById(pipelineId);
        if (pipeline == null) throw new BusinessException("Pipeline not found", 404);
        PipelineRun run = new PipelineRun();
        run.setPipelineId(pipelineId);
        run.setStatus("pending");
        run.setStartedAt(Instant.now());
        run.setApprovalNeeded(false);
        runRepository.insert(run);
        // 异步执行
        executeAsync(run.getId());
        auditService.log("TRIGGER_PIPELINE", "PipelineRun", run.getId(), Map.of("pipelineId", pipelineId));
        return run.getId();
    }

    @Async
    public void executeAsync(Long runId) {
        executionService.executePipeline(runId);
    }

    @Override
    public PipelineRunResponse getRun(Long runId) {
        PipelineRun run = runRepository.selectById(runId);
        if (run == null) throw new BusinessException("Run not found", 404);
        PipelineRunResponse resp = new PipelineRunResponse();
        resp.setId(run.getId());
        resp.setPipelineId(run.getPipelineId());
        resp.setStatus(run.getStatus());
        resp.setStartedAt(run.getStartedAt());
        resp.setFinishedAt(run.getFinishedAt());
        return resp;
    }
}