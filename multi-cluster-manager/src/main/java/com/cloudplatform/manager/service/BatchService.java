package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.BatchTaskCreateRequest;
import com.cloudplatform.manager.dto.BatchTaskDetail;

public interface BatchService {
    BatchTaskDetail createTask(BatchTaskCreateRequest request, Long userId);
    BatchTaskDetail getTaskDetail(Long taskId);
    void nextBatch(Long taskId, Long userId);
    void rollback(Long taskId, Long userId);
    void resume(Long taskId, Long userId);
    void autoRollbackIfNeeded(Long taskId);
}