package com.cloudplatform.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cloudplatform.manager.mapper.AuditLogMapper;
import com.cloudplatform.manager.model.entity.AuditLog;
import com.cloudplatform.manager.service.AuditService;
import com.cloudplatform.manager.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Async
    @Override
    public void log(String operation, String targetType, Long targetId, Map<String, Object> details) {
        AuditLog logEntry = new AuditLog();
        logEntry.setOperation(operation);
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setDetails(JSON.toJSONString(details));
        logEntry.setCreatedAt(LocalDateTime.from(Instant.now()));

        // 获取当前登录用户 ID（从 SecurityContext 获取）
        Long userId = null;
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.cloudplatform.manager.security.CurrentUserDetails) {
                userId = ((com.cloudplatform.manager.security.CurrentUserDetails) authentication.getPrincipal()).getUserId();
            }
        } catch (Exception e) {
            log.warn("Unable to get current user for audit log", e);
        }
        logEntry.setUserId(userId);

        // 获取请求 IP 和 User-Agent（如果存在当前请求）
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntry.setRequestIp(InetAddress.getByName(request.getRemoteAddr()));
                logEntry.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (UnknownHostException e) {
            log.warn("Unable to resolve IP address", e);
        }

        // 计算 prev_hash：查询上一条审计日志的哈希
        String lastHash = getLastAuditHash();
        String currentHash = calculateHash(logEntry, lastHash);
        logEntry.setPrevHash(lastHash);

        // 插入数据库
        auditLogMapper.insert(logEntry);

        // （可选）将当前哈希存储，以便下次使用，可以用 Redis 记录最后一个 id 的哈希，但这里不强制。
    }

    private String getLastAuditHash() {
        // 查询最新一条 audit_log 的 id 和组合字符串
        // 简单实现：从数据库取最新一条记录，计算其哈希；若无记录则返回 null
        // 实际可优化：直接存储最后一条的哈希到缓存，避免多次查询
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuditLog>();
        wrapper.orderByDesc(AuditLog::getId).last("limit 1");
        AuditLog last = auditLogMapper.selectOne(wrapper);
        if (last == null) return null;
        return calculateHash(last, last.getPrevHash());
    }

    private String calculateHash(AuditLog log, String prevHash) {
        // 组合字符串：prevHash + userId + operation + targetType + targetId + details + createdAt
        StringBuilder sb = new StringBuilder();
        if (prevHash != null) sb.append(prevHash);
        sb.append(log.getUserId() == null ? "" : log.getUserId());
        sb.append(log.getOperation());
        sb.append(log.getTargetType() == null ? "" : log.getTargetType());
        sb.append(log.getTargetId() == null ? "" : log.getTargetId());
        sb.append(log.getDetails() == null ? "" : log.getDetails().toString());
        sb.append(log.getCreatedAt().toString());
        return HashUtil.sha256(sb.toString());
    }
}