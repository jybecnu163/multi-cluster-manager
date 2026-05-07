package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.MetricTimeSeries;
import com.cloudplatform.manager.dto.ServiceDetailResponse;
import com.cloudplatform.manager.dto.ServiceListResponse;
import com.cloudplatform.manager.service.ReportService;
import com.cloudplatform.manager.service.ServiceInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "服务管理")
public class ServiceController extends BaseController {

    private final ServiceInstanceService serviceInstanceService;
    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员') or hasPermission(#departmentId, 'SERVICE_VIEW')")
    @Operation(summary = "服务列表")
    public ResponseEntity<ServiceListResponse> listServices(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String envType,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        ServiceListResponse response = serviceInstanceService.listServices(departmentId, envType, name, page, pageSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{service_id}")
    @PreAuthorize("hasRole('系统管理员') or hasPermission(#serviceId, 'SERVICE_VIEW')")
    @Operation(summary = "服务详情")
    public ResponseEntity<ServiceDetailResponse> getService(@PathVariable("service_id") Long serviceId) {
        ServiceDetailResponse detail = serviceInstanceService.getServiceDetail(serviceId);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/{service_id}/metrics")
    @PreAuthorize("hasRole('系统管理员') or hasPermission(#serviceId, 'SERVICE_VIEW')")
    @Operation(summary = "获取资源使用趋势")
    public ResponseEntity<MetricTimeSeries> getMetrics(
            @PathVariable("service_id") Long serviceId,
            @RequestParam String metric,
            @RequestParam(defaultValue = "1h") String range) {
        MetricTimeSeries data = serviceInstanceService.getMetrics(serviceId, metric, range);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{service_id}/reports/export")
    @PreAuthorize("hasRole('系统管理员') or hasPermission(#serviceId, 'HISTORY_VIEW')")
    @Operation(summary = "导出历史报表CSV")
    public void exportReport(
            @PathVariable("service_id") Long serviceId,
            @RequestParam String timeRange,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        byte[] csvData = reportService.exportServiceReport(serviceId, timeRange, startDate, endDate);
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=service_report.csv");
        response.getOutputStream().write(csvData);
        response.flushBuffer();
    }
}
