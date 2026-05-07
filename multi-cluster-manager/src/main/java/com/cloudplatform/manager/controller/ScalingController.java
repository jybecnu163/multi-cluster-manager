package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.ManualScaleRequest;
import com.cloudplatform.manager.dto.ScaleResponse;
import com.cloudplatform.manager.service.ManualScalingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "扩缩容")
public class ScalingController extends BaseController{

    private final ManualScalingService manualScalingService;

    @PostMapping("/{service_id}/scale")
    @PreAuthorize("hasRole('系统管理员') or hasPermission(#serviceId, 'SCALE')")
    @Operation(summary = "手动扩缩容")
    public ResponseEntity<ScaleResponse> manualScale(
            @PathVariable("service_id") Long serviceId,
            @Valid @RequestBody ManualScaleRequest request,
            @RequestAttribute("userId") Long userId) {
        ScaleResponse response = manualScalingService.scale(serviceId, request, userId);
        return ResponseEntity.accepted().body(response);
    }
}
