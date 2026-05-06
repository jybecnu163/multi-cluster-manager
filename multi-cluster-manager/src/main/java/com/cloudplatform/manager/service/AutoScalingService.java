package com.cloudplatform.manager.service;

import com.cloudplatform.manager.model.entity.AutoScalingPolicy;
import java.util.List;

public interface AutoScalingService {
    List<AutoScalingPolicy> listPolicies();
    AutoScalingPolicy savePolicy(AutoScalingPolicy policy);
    void evaluateAndScale();  // 由调度器调用
}
