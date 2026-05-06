package com.cloudplatform.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.AuditLogMapper;
import com.cloudplatform.manager.model.entity.AuditLog;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AuditLogMapper auditLogMapper;

    public byte[] exportServiceReport(Long serviceId, String timeRange, String startDate, String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        List<AuditLog> logs = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getTargetId, serviceId)
                        .eq(AuditLog::getTargetType, "service")
                        .between(AuditLog::getCreatedAt, start.atStartOfDay(), end.atStartOfDay())
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"Time", "Operation", "User", "Details"});
            for (AuditLog log : logs) {
                writer.writeNext(new String[]{
                        log.getCreatedAt().toString(),
                        log.getOperation(),
                        String.valueOf(log.getUserId()),
                        log.getDetails() != null ? log.getDetails().toString() : ""
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
        return baos.toByteArray();
    }
}
