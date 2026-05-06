package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.CanaryTaskCreateRequest;
import com.cloudplatform.manager.dto.CanaryTaskDetail;
import com.cloudplatform.manager.security.CurrentUserDetails;
import com.cloudplatform.manager.service.CanaryProxyService;
import com.cloudplatform.manager.service.CanaryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/canary")
public class CanaryController extends BaseController{
    @Autowired private CanaryService canaryService;
    @Autowired private CanaryProxyService proxyService;

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<CanaryTaskDetail> createTask(@Valid @RequestBody CanaryTaskCreateRequest request) {
        Long userId = getCurrentUserId();
        CanaryTaskDetail detail = canaryService.createTask(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @GetMapping("/tasks/{task_id}")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员') or hasRole('审计员')")
    public ResponseEntity<CanaryTaskDetail> getTask(@PathVariable("task_id") Long taskId) {
        return ResponseEntity.ok(canaryService.getTaskDetail(taskId));
    }

    @GetMapping("/tasks/{task_id}/internal-endpoint")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Map<String, Object>> getInternalEndpoint(@PathVariable("task_id") Long taskId) {
        URL url = proxyService.getInternalEndpoint(taskId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("internal_url", url.toString());
        resp.put("expires_in_seconds", 3600); // 简化
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/tasks/{task_id}/stage/{stage}")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Void> promoteStage(@PathVariable("task_id") Long taskId,
                                             @PathVariable("stage") String stage) {
        Long userId = getCurrentUserId();
        canaryService.promoteStage(taskId, stage, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{task_id}/rollback")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员') or hasRole('部门主管')")
    public ResponseEntity<Void> rollback(@PathVariable("task_id") Long taskId) {
        Long userId = getCurrentUserId();
        canaryService.rollback(taskId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{task_id}/resume")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Void> resume(@PathVariable("task_id") Long taskId) {
        canaryService.resume(taskId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        CurrentUserDetails user = (CurrentUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}