package com.cloudplatform.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudplatform.manager.model.entity.AuditLog;
import com.cloudplatform.manager.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
@Tag(name = "审计日志")
public class AuditController extends BaseController{

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员') or hasRole('审计员')")
    @Operation(summary = "查询审计日志")
    public ResponseEntity<Page<AuditLog>> queryLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operation,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> result = auditLogService.queryLogs(startTime, endTime, operation, PageRequest.of(page-1, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('系统管理员') or hasRole('审计员')")
    @Operation(summary = "导出审计日志CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String operation) {
        byte[] csv = auditLogService.exportCsv(startTime, endTime, operation);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(csv);
    }

    @GetMapping("/verify")
    @PreAuthorize("hasRole('系统管理员')")
    @Operation(summary = "验证哈希链")
    public ResponseEntity<Boolean> verifyChain() {
        boolean valid = auditLogService.verifyChain();
        return ResponseEntity.ok(valid);
    }
}
