package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batch")
public class BatchController extends BaseController {

    @PostMapping("/tasks")
    public ResponseEntity<?> createBatchTask(@RequestBody Object request) {
        return notImplemented();
    }

    @GetMapping("/tasks/{task_id}")
    public ResponseEntity<?> getBatchTask(@PathVariable UUID taskId) {
        return notImplemented();
    }

    @PostMapping("/tasks/{task_id}/next")
    public ResponseEntity<?> nextBatch(@PathVariable UUID taskId) {
        return notImplemented();
    }

    @PostMapping("/tasks/{task_id}/rollback")
    public ResponseEntity<?> rollbackBatch(@PathVariable UUID taskId) {
        return notImplemented();
    }

    @PostMapping("/tasks/{task_id}/resume")
    public ResponseEntity<?> resumeBatch(@PathVariable UUID taskId) {
        return notImplemented();
    }
}