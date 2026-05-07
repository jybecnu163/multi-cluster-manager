package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.integration.K8sClientManager;
import com.cloudplatform.manager.integration.MetricsCollector;
import com.cloudplatform.manager.mapper.AutoScalingPolicyMapper;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.model.entity.AutoScalingPolicy;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import com.cloudplatform.manager.service.AutoScalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScalingServiceImpl implements AutoScalingService {

    private final AutoScalingPolicyMapper policyMapper;
    private final ServiceInstanceMapper serviceInstanceMapper;
    private final MetricsCollector metricsCollector;
    private final K8sClientManager k8sClientManager;
    private final RedissonClient redissonClient;

    // 记录上次扩缩时间（冷却控制）
    private final ConcurrentHashMap<Long, AtomicLong> lastScaleTime = new ConcurrentHashMap<>();
    // 记录连续超阈值次数
    private final ConcurrentHashMap<Long, Integer> consecutiveExceed = new ConcurrentHashMap<>();

    @Override
    public List<AutoScalingPolicy> listPolicies() {
        return policyMapper.selectList(null);
    }

    @Override
    @Transactional
    public AutoScalingPolicy savePolicy(AutoScalingPolicy policy) {
        if (policy.getId() == null) {
            policy.setCreatedAt(LocalDateTime.from(Instant.now()));
            policyMapper.insert(policy);
        } else {
            policy.setUpdatedAt(LocalDateTime.from(Instant.now()));
            policyMapper.updateById(policy);
        }
        return policy;
    }

    @Scheduled(fixedDelay = 30000) // 每30秒执行一次
    public void evaluateAndScale() {
        RLock lock = redissonClient.getLock("autoscaling_lock");
        try {
            if (lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                try {
//                    List<AutoScalingPolicy> policies = policyMapper.findEnabledPolicies();
                    List<AutoScalingPolicy> policies = policyMapper
                            .selectList(new LambdaQueryWrapper<AutoScalingPolicy>()
                                    .eq(AutoScalingPolicy::getEnabled, true));
                    for (AutoScalingPolicy policy : policies) {
                        evaluatePolicy(policy);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AutoScaling lock interrupted");
        }
    }

    private void evaluatePolicy(AutoScalingPolicy policy) {
        ServiceInstance inst = serviceInstanceMapper.selectById(policy.getServiceInstanceId());
        if (inst == null) return;

        int currentReplicas = getCurrentReplicas(inst);
        double currentValue;
        try {
            currentValue = metricsCollector.fetchMetric(policy, inst);
        } catch (Exception e) {
            log.error("Failed to fetch metric for policy {}, fallback to last value", policy.getId(), e);
            // 降级处理：跳过本次评估，保留状态
            return;
        }

        int threshold = policy.getTargetThreshold();
        int min = policy.getMinReplicas();
        int max = policy.getMaxReplicas();
        long cooldownSec = policy.getCooldownSeconds();
        long last = lastScaleTime.computeIfAbsent(policy.getId(), id -> new AtomicLong(0)).get();
        boolean inCooldown = (System.currentTimeMillis() - last) < cooldownSec * 1000L;

        // 扩容条件：连续3次超阈值
        if (currentValue > threshold) {
            int exceedCount = consecutiveExceed.compute(policy.getId(), (id, count) -> (count == null ? 1 : count + 1));
            if (exceedCount >= 3 && !inCooldown) {
                int desired = (int) Math.ceil(currentReplicas * (currentValue / threshold));
                desired = Math.min(max, Math.max(min, desired));
                if (desired != currentReplicas) {
                    executeScale(inst, desired, "auto-scale up");
                    lastScaleTime.get(policy.getId()).set(System.currentTimeMillis());
                    consecutiveExceed.put(policy.getId(), 0); // 重置计数
                }
            }
        } else {
            consecutiveExceed.put(policy.getId(), 0); // 重置
        }

        // 缩容条件：低负载连续10分钟（由 scale_down_delay_minutes 决定）
        if (currentValue < threshold * 0.8) {
            // 实际应记录低负载开始时间，简化：用连续评估次数模拟持续低负载
            // 假设调度间隔30秒，scale_down_delay_minutes=10 => 需要20个连续低负载周期 policy.getId() + "_low"
            int lowLoadCount = consecutiveExceed.compute(policy.getId(), (id, cnt) -> (cnt == null ? 1 : cnt + 1));
            long requiredCycles = policy.getScaleDownDelayMinutes() * 60L / 30L;
            if (lowLoadCount >= requiredCycles && !inCooldown) {
                int desired = (int) Math.ceil(currentReplicas * (currentValue / threshold));
                desired = Math.min(max, Math.max(min, desired));
                if (desired < currentReplicas) {
                    executeScale(inst, desired, "auto-scale down");
                    lastScaleTime.get(policy.getId()).set(System.currentTimeMillis());
                    consecutiveExceed.put(policy.getId(), 0);
                }
            }
        } else {
            consecutiveExceed.put(policy.getId(), 0);
        }
    }

    private int getCurrentReplicas(ServiceInstance inst) {
        // 可从K8s实时获取或使用数据库记录，优先实时查询确保准确
        return k8sClientManager.getCurrentReplicas(inst.getClusterId(), inst.getNamespace(),
                inst.getWorkloadName(), inst.getWorkloadType());
    }


    private void executeScale(ServiceInstance inst, int targetReplicas, String reason) {
        k8sClientManager.scaleWorkload(inst.getClusterId(), inst.getNamespace(),
                inst.getWorkloadName(), inst.getWorkloadType(), targetReplicas, inst.getId());
        inst.setReplicas(targetReplicas);
        serviceInstanceMapper.updateById(inst);
        log.info("Auto-scaling executed for service {} to {} replicas, reason: {}", inst.getId(), targetReplicas, reason);
        // 记录审计日志
    }
}
