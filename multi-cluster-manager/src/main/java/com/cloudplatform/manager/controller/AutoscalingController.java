package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.model.entity.AutoScalingPolicy;
import com.cloudplatform.manager.service.AutoScalingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/autoscaling/policies")
@RequiredArgsConstructor
@Tag(name = "动态扩缩")
public class AutoscalingController extends BaseController{

    private final AutoScalingService autoScalingService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'AUTOSCALING_VIEW')")
    @Operation(summary = "获取动态扩缩策略列表")
    public ResponseEntity<List<AutoScalingPolicy>> listPolicies() {
        return ResponseEntity.ok(autoScalingService.listPolicies());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'AUTOSCALING_MANAGE')")
    @Operation(summary = "创建或更新动态扩缩策略")
    public ResponseEntity<AutoScalingPolicy> createOrUpdatePolicy(@Valid @RequestBody AutoScalingPolicy policy) {
        AutoScalingPolicy saved = autoScalingService.savePolicy(policy);
        return ResponseEntity.ok(saved);
    }
}
