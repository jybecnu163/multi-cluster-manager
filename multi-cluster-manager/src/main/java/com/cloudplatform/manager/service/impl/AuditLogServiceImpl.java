package com.cloudplatform.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.AuditLogMapper;
import com.cloudplatform.manager.model.entity.AuditLog;
import com.cloudplatform.manager.service.AuditLogService;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${audit.rabbitmq.exchange:audit.exchange}")
    private String exchange;
    @Value("${audit.rabbitmq.routing-key:audit.log}")
    private String routingKey;

    @Override
    public void log(Long userId, String operation, String targetType, Long targetId,
                    String requestIp, String userAgent, Object details) {
        // 异步发送到 RabbitMQ，不阻塞主流程
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setRequestIp(parseIp(requestIp));
        log.setUserAgent(userAgent);
        log.setDetails(JSON.toJSONString(details));
        log.setCreatedAt(LocalDateTime.now());
        rabbitTemplate.convertAndSend(exchange, routingKey, log);
    }



    @Override
    public Page<AuditLog> queryLogs(LocalDateTime startTime, LocalDateTime endTime,
                                    String operation, Pageable pageable) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(AuditLog::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(AuditLog::getCreatedAt, endTime);
        if (operation != null && !operation.isEmpty()) wrapper.eq(AuditLog::getOperation, operation);
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        Page<AuditLog> mpPage = new Page<>(pageable.getPageNumber(), pageable.getPageSize());
        return auditLogMapper.selectPage(mpPage, wrapper);
    }

    @Override
    public byte[] exportCsv(LocalDateTime startTime, LocalDateTime endTime, String operation) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(AuditLog::getCreatedAt, startTime);
        if (endTime != null) wrapper.le(AuditLog::getCreatedAt, endTime);
        if (operation != null && !operation.isEmpty()) wrapper.eq(AuditLog::getOperation, operation);
        wrapper.orderByAsc(AuditLog::getCreatedAt);
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"ID", "时间", "操作用户ID", "操作类型", "目标类型", "目标ID", "请求IP", "UserAgent", "详情", "前哈希"});
            for (AuditLog log : logs) {
                writer.writeNext(new String[]{
                        String.valueOf(log.getId()),
                        log.getCreatedAt().toString(),
                        String.valueOf(log.getUserId()),
                        log.getOperation(),
                        log.getTargetType(),
                        String.valueOf(log.getTargetId()),
                        log.getRequestIp() != null ? log.getRequestIp().toString() : "",
                        log.getUserAgent() != null ? log.getUserAgent() : "",
                        log.getDetails() != null ? log.getDetails().toString() : "",
                        log.getPrevHash() != null ? log.getPrevHash() : ""
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
        return baos.toByteArray();
    }

    @Override
    public boolean verifyChain() {
        List<AuditLog> allLogs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>().orderByAsc(AuditLog::getId));
        for (int i = 0; i < allLogs.size(); i++) {
            AuditLog current = allLogs.get(i);
            if (i == 0) {
                if (current.getPrevHash() != null && !current.getPrevHash().isEmpty()) {
                    return false; // 第一条记录的 prev_hash 必须为空或 null
                }
                continue;
            }
            AuditLog prev = allLogs.get(i - 1);
            String computedPrevHash = computeHash(prev);
            if (!computedPrevHash.equals(current.getPrevHash())) {
                log.error("Chain verification failed at index {}: expected {} but got {}", i, computedPrevHash, current.getPrevHash());
                return false;
            }
        }
        return true;
    }

    private String computeHash(AuditLog log) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String content = log.getId() + "|" +
                    log.getUserId() + "|" +
                    log.getOperation() + "|" +
                    log.getTargetType() + "|" +
                    log.getTargetId() + "|" +
                    log.getCreatedAt();
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private InetAddress parseIp(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            return null;
        }
    }
}
