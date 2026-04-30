package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listPipelines() {
        return notImplemented();
    }

    @PostMapping
    public ResponseEntity<?> createPipeline(@RequestBody Object request) {
        return notImplemented();
    }

    @PostMapping("/{pipeline_id}/trigger")
    public ResponseEntity<?> triggerPipeline(@PathVariable UUID pipelineId) {
        return notImplemented();
    }
}