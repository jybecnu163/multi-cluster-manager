package com.cloudplatform.manager.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.service.BatchService;
import com.cloudplatform.manager.service.CanaryService;
import com.cloudplatform.manager.service.ClusterService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledTasks {
    @Autowired
    private DeploymentTaskMapper taskRepository;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private CanaryService canaryService;

    @Autowired
    private BatchService batchService;

    @Scheduled(fixedDelay = 30000) // 每30秒检查一次
    public void checkBatchHealth() {
        List<DeploymentTask> tasks = taskRepository.selectList(
                new LambdaQueryWrapper<DeploymentTask>()
                        .eq(DeploymentTask::getTaskType, "batch")
                        .eq(DeploymentTask::getStatus, "in_progress")
        );
        for (DeploymentTask task : tasks) {
            batchService.autoRollbackIfNeeded(task.getId());
        }
    }

    @Scheduled(fixedDelay = 30000) // 每30秒检查一次
    public void checkCanaryErrorRate() {
        // 需要获取所有处于 traffic_5 和 traffic_25 状态的任务
        List<DeploymentTask> tasks = taskRepository.selectList(
                new LambdaQueryWrapper<DeploymentTask>()
                        .in(DeploymentTask::getStatus, List.of("traffic_5", "traffic_25"))
        );
        for (DeploymentTask task : tasks) {
            canaryService.autoPauseIfErrorRateHigh(task.getId());
        }
    }

    @Scheduled(fixedDelay = 30000)  // 每30秒执行一次
    public void healthCheck() {
        String lockKey = "cluster-health-check-lock";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，等待0秒，持有锁最多25秒（避免死锁）
            if (lock.tryLock(0, 25, TimeUnit.SECONDS)) {
                try {
                    clusterService.checkAndUpdateClusterHealth();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}