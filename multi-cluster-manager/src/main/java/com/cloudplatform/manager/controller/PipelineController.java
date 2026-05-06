package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.PipelineCreateRequest;
import com.cloudplatform.manager.dto.PipelineRunResponse;
import com.cloudplatform.manager.model.entity.Pipeline;
import com.cloudplatform.manager.security.CurrentUserDetails;
import com.cloudplatform.manager.service.PipelineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController extends BaseController{
    @Autowired private PipelineService pipelineService;

    @GetMapping
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<List<Pipeline>> listPipelines() {
        return ResponseEntity.ok(pipelineService.listPipelines());
    }

    @PostMapping
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Long> createPipeline(@Valid @RequestBody PipelineCreateRequest request) {
        Long userId = getCurrentUserId();
        Long id = pipelineService.createPipeline(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PostMapping("/{pipeline_id}/trigger")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Long> triggerPipeline(@PathVariable("pipeline_id") Long pipelineId) {
        Long userId = getCurrentUserId();
        Long runId = pipelineService.triggerPipeline(pipelineId, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(runId);
    }

    @GetMapping("/runs/{run_id}")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员') or hasRole('审计员')")
    public ResponseEntity<PipelineRunResponse> getRun(@PathVariable("run_id") Long runId) {
        return ResponseEntity.ok(pipelineService.getRun(runId));
    }

    private Long getCurrentUserId() {
        CurrentUserDetails user = (CurrentUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}