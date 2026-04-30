package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/autoscaling/policies")
public class AutoscalingController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listPolicies() {
        return notImplemented();
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdatePolicy(@RequestBody Object policy) {
        return notImplemented();
    }
}