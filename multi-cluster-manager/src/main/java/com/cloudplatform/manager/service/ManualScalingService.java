package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.ManualScaleRequest;
import com.cloudplatform.manager.dto.ScaleResponse;

public interface ManualScalingService {
    ScaleResponse scale(Long serviceId, ManualScaleRequest request, Long userId);
}
