package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listServices(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String envType,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return notImplemented();
    }

    @GetMapping("/{service_id}")
    public ResponseEntity<?> getService(@PathVariable UUID serviceId) {
        return notImplemented();
    }

    @GetMapping("/{service_id}/metrics")
    public ResponseEntity<?> getMetrics(@PathVariable UUID serviceId,
                                        @RequestParam String metric,
                                        @RequestParam(defaultValue = "1h") String range) {
        return notImplemented();
    }

    // WebSocket endpoint is handled separately; HTTP placeholder for OpenAPI only.
    @GetMapping("/{service_id}/logs")
    public ResponseEntity<?> logsPlaceholder() {
        return notImplemented();
    }

    @GetMapping("/{service_id}/reports/export")
    public ResponseEntity<?> exportReport(@PathVariable UUID serviceId,
                                          @RequestParam String timeRange,
                                          @RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate) {
        return notImplemented();
    }
}