package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.CanaryTaskCreateRequest;
import com.cloudplatform.manager.dto.CanaryTaskDetail;
import org.springframework.transaction.annotation.Transactional;

public interface CanaryService {
    @Transactional
    CanaryTaskDetail createTask(CanaryTaskCreateRequest request, Long userId);

    CanaryTaskDetail getTaskDetail(Long taskId);

    @Transactional
    void promoteStage(Long taskId, String stage, Long userId);

    @Transactional
    void rollback(Long taskId, Long userId);

    @Transactional
    void resume(Long taskId);

    void autoPauseIfErrorRateHigh(Long taskId);
}