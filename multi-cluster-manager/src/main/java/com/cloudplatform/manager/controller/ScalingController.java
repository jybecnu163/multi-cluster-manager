package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
public class ScalingController extends BaseController {

    @PostMapping("/{service_id}/scale")
    public ResponseEntity<?> manualScale(@PathVariable UUID serviceId,
                                         @RequestBody Object request) {
        return notImplemented();
    }
}