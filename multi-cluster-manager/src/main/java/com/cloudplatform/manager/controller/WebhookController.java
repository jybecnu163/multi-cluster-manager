package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.mapper.PipelineMapper;
import com.cloudplatform.manager.model.entity.Pipeline;
import com.cloudplatform.manager.service.PipelineService;
import com.cloudplatform.manager.webhook.WebhookSignatureValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController extends BaseController {
    @Autowired
    private PipelineMapper pipelineRepository;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private WebhookSignatureValidator signatureValidator;

    @PostMapping("/{pipelineId}")
    public ResponseEntity<Void> handleWebhook(@PathVariable Long pipelineId,
                                              @RequestHeader("X-Signature") String signature,
                                              @RequestBody String body) {
        Pipeline pipeline = pipelineRepository.selectById(pipelineId);
        if (pipeline == null || !"webhook".equals(pipeline.getTriggerType())) {
            return ResponseEntity.notFound().build();
        }
        if (signature == null || !signatureValidator.isValid(body, signature, pipeline.getWebhookSecret())) {
            return ResponseEntity.status(401).build();
        }
        // 触发流水线，用户 ID 使用系统用户
        pipelineService.triggerPipeline(pipelineId, 0L); // 0 表示系统触发
        return ResponseEntity.accepted().build();
    }
}