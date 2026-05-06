package com.cloudplatform.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cloudplatform.manager.dto.BatchTaskCreateRequest;
import com.cloudplatform.manager.dto.CanaryTaskCreateRequest;
import com.cloudplatform.manager.dto.PipelineCreateRequest;
import com.cloudplatform.manager.mapper.PipelineMapper;
import com.cloudplatform.manager.mapper.PipelineRunMapper;
import com.cloudplatform.manager.model.entity.Pipeline;
import com.cloudplatform.manager.model.entity.PipelineRun;
import com.cloudplatform.manager.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PipelineExecutionServiceImpl implements PipelineExecutionService {
    @Autowired
    private PipelineMapper pipelineRepository;
    @Autowired
    private PipelineRunMapper runRepository;
    @Autowired
    private ApprovalService approvalService;
    @Autowired
    private CanaryService canaryService;
    @Autowired
    private BatchService batchService;
    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public void executePipeline(Long pipelineRunId) {
        PipelineRun run = runRepository.selectById(pipelineRunId);
        if (run == null || !run.getStatus().equals("pending")) return;
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        runRepository.updateById(run);

        Pipeline pipeline = pipelineRepository.selectById(run.getPipelineId());
//        JSONArray steps = JSON.parseArray(pipeline.getSteps());
        String jsonStr = JSON.toJSONString(pipeline.getSteps());
        List<PipelineCreateRequest.Step> steps = JSON.parseArray(jsonStr, PipelineCreateRequest.Step.class);

        try {
            Map<String, Object> context = new java.util.HashMap<>();
            for (int i = 0; i < steps.size(); i++) {
                PipelineCreateRequest.Step step = steps.get(i);//.getJSONObject(i);
                String type = step.getType();
                if ("approval".equals(type)) {
                    // 创建审批单
                    Long approvalId = approvalService.createApproval(
                            pipelineRunId, run.getApprovedBy(), "部门主管", pipeline.getApprovalTimeoutHours());
                    run.setApprovalNeeded(true);
                    runRepository.updateById(run);
                    // 等待审批（异步，由审批回调继续执行）
                    return; // 暂停执行，等待审批回调
                } else {
                    executeStep(step, context);
                }
            }
            // 所有步骤完成
            run.setStatus("success");
            run.setFinishedAt(Instant.now());
            runRepository.updateById(run);
            auditService.log("PIPELINE_SUCCESS", "PipelineRun", pipelineRunId, null);
        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            run.setStatus("failed");
            run.setFinishedAt(Instant.now());
            runRepository.updateById(run);
            auditService.log("PIPELINE_FAILED", "PipelineRun", pipelineRunId, Map.of("error", e.getMessage()));
        }
    }

    private void executeStep(PipelineCreateRequest.Step step, Map<String, Object> context) {
        String type = step.getType();
        switch (type) {
            case "git-clone":
                // 模拟 git clone
                log.info("Cloning repo: {}", step.getRepo());
                break;
            case "build-image":
                log.info("Building image: {}", step.getImage());
                break;
            case "unit-test":
                log.info("Running unit tests");
                break;
            case "image-scan":
                log.info("Scanning image for vulnerabilities");
                break;
            case "deploy":
                String method = step.getDeploymentMethod();//.getString("deploymentMethod");
                Long serviceId = step.getTargetServiceId();//.getLong("targetServiceId");
                String image = step.getImage();//.getString("image");
                Long userId = getCurrentUserIdFromContext(); // 需要从上下文获取
                if ("canary".equals(method)) {
                    CanaryTaskCreateRequest request = new CanaryTaskCreateRequest();
                    request.setServiceInstanceId(serviceId);
                    request.setTargetImage(image);
                    // 默认策略
                    canaryService.createTask(request, userId);
                } else {
                    BatchTaskCreateRequest request = new BatchTaskCreateRequest();
                    request.setServiceInstanceId(serviceId);
                    request.setTargetImage(image);
                    // 默认批次配置需从步骤 config 读取
                    batchService.createTask(request, userId);
                }
                break;
            default:
                throw new RuntimeException("Unknown step type: " + type);
        }
    }

    private Long getCurrentUserIdFromContext() {
        // 从线程上下文获取, 简化返回系统用户
        return 1L;
    }
}