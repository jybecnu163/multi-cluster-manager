package com.cloudplatform.manager.service;

import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface AuditService {

    @Async
    void log(String operation, String targetType, Long targetId, Map<String, Object> details);
}