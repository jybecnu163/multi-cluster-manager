package com.cloudplatform.manager.scheduler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cloudplatform.manager.mapper.ApprovalMapper;
import com.cloudplatform.manager.model.entity.Approval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalTimeoutScheduler {

    private final ApprovalMapper approvalMapper;

    /**
     * 每 10 分钟执行一次，将过期的待审批单标记为 expired
     */
    @Scheduled(fixedDelay = 600000)
    public void expirePendingApprovals() {
//        int updated = approvalMapper.batchExpirePending();
        int updated = approvalMapper.update(null,
                new LambdaUpdateWrapper<Approval>()
                        .set(Approval::getAction, "expired")
                        .eq(Approval::getAction, "pending")
                        .lt(Approval::getExpiresAt, LocalDateTime.now())
        );
        if (updated > 0) {
            log.info("Expired {} pending approvals", updated);
        }
    }
}
