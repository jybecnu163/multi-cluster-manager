package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/canary")
public class CanaryController extends BaseController {

    @PostMapping("/tasks")
    public ResponseEntity<?> createCanaryTask(@RequestBody Object request) {
        return notImplemented();
    }

    @GetMapping("/tasks/{task_id}")
    public ResponseEntity<?> getCanaryTask(@PathVariable UUID taskId) {
        return notImplemented();
    }

    @GetMapping("/tasks/{task_id}/internal-endpoint")
    public ResponseEntity<?> getInternalEndpoint(@PathVariable UUID taskId) {
        return notImplemented();
    }

    @PostMapping("/tasks/{task_id}/stage/{stage}")
    public ResponseEntity<?> promoteStage(@PathVariable UUID taskId,
                                          @PathVariable String stage) {
        return notImplemented();
    }

    @PostMapping("/tasks/{task_id}/rollback")
    public ResponseEntity<?> rollbackCanary(@PathVariable UUID taskId) {
        return notImplemented();
    }

    @PostMapping("/tasks/{task_id}/resume")
    public ResponseEntity<?> resumeCanary(@PathVariable UUID taskId) {
        return notImplemented();
    }
}