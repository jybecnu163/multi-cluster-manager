package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit/logs")
public class AuditController extends BaseController {

    @GetMapping
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String operation,
            @RequestParam(defaultValue = "1") int page) {
        return notImplemented();
    }
}