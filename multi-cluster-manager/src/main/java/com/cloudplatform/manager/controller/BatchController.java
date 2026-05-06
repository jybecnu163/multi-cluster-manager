package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.BatchTaskCreateRequest;
import com.cloudplatform.manager.dto.BatchTaskDetail;
import com.cloudplatform.manager.security.CurrentUserDetails;
import com.cloudplatform.manager.service.BatchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/batch")
public class BatchController extends BaseController{
    @Autowired private BatchService batchService;

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<BatchTaskDetail> createTask(@Valid @RequestBody BatchTaskCreateRequest request) {
        Long userId = getCurrentUserId();
        BatchTaskDetail detail = batchService.createTask(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @GetMapping("/tasks/{task_id}")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员') or hasRole('审计员')")
    public ResponseEntity<BatchTaskDetail> getTask(@PathVariable("task_id") Long taskId) {
        return ResponseEntity.ok(batchService.getTaskDetail(taskId));
    }

    @PostMapping("/tasks/{task_id}/next")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Void> nextBatch(@PathVariable("task_id") Long taskId) {
        Long userId = getCurrentUserId();
        batchService.nextBatch(taskId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{task_id}/rollback")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员') or hasRole('部门主管')")
    public ResponseEntity<Void> rollback(@PathVariable("task_id") Long taskId) {
        Long userId = getCurrentUserId();
        batchService.rollback(taskId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{task_id}/resume")
    @PreAuthorize("hasRole('运维工程师') or hasRole('系统管理员')")
    public ResponseEntity<Void> resume(@PathVariable("task_id") Long taskId) {
        Long userId = getCurrentUserId();
        batchService.resume(taskId, userId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        CurrentUserDetails user = (CurrentUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}