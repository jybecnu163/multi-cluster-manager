package com.cloudplatform.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudplatform.manager.model.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface AuditLogService {
    void log(Long userId, String operation, String targetType, Long targetId,
             String requestIp, String userAgent, Object details);
    Page<AuditLog> queryLogs(LocalDateTime startTime, LocalDateTime endTime,
                             String operation, Pageable pageable);
    byte[] exportCsv(LocalDateTime startTime, LocalDateTime endTime, String operation);
    boolean verifyChain();
}
