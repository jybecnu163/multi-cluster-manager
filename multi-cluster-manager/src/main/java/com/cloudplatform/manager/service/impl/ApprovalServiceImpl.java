package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.ApprovalMapper;
import com.cloudplatform.manager.mapper.DeploymentTaskMapper;
import com.cloudplatform.manager.mapper.UserRoleMapper;
import com.cloudplatform.manager.model.entity.Approval;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import com.cloudplatform.manager.model.entity.UserRole;
import com.cloudplatform.manager.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalMapper approvalMapper;
    private final DeploymentTaskMapper deploymentTaskMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public Long createApproval(Long taskId, Long requesterId, String approverRole, int timeoutHours) {
        return 0L;
    }

    @Override
    public void approve(Long approvalId, Long approverId, String comment) {

    }

    @Override
    public void reject(Long approvalId, Long approverId, String comment) {

    }

    @Override
    @Transactional
    public Long createApproval(Long taskId, Long approverId, int timeoutHours) {
        // 检查是否已有待审批单
//        Approval existing = approvalMapper.findPendingByTaskId(taskId);
        Approval existing = approvalMapper.selectList(
                        new LambdaQueryWrapper<Approval>()
                                .eq(Approval::getTaskId, taskId)
                                .eq(Approval::getAction, "pending"))
                .getFirst();
        if (existing != null) {
            throw new RuntimeException("Approval already exists for task: " + taskId);
        }

        Approval approval = new Approval();
        approval.setTaskId(taskId);
        approval.setApproverId(approverId);

        approval.setAction("pending");
        approval.setComment(null);
        approval.setCreatedAt(LocalDateTime.now());
        approval.setExpiresAt(LocalDateTime.now().plusHours(timeoutHours));

        approvalMapper.insert(approval);
        log.info("Created approval {} for task {} with approver {}", approval.getId(), taskId, approverId);
        return approval.getId();
    }

    @Override
    public List<Approval> getPendingApprovals(Long userId) {
        // 查出所有 role_id=2 (部门主管) 且 env_type 为 all 或对应部门 
        // 这里简化：只返回审批人是当前用户的待审批单
//        return approvalMapper.findPendingByApproverId(userId);

        return approvalMapper.selectList(
                new LambdaQueryWrapper<Approval>()
                        .eq(Approval::getApproverId, userId)
                        .eq(Approval::getStatus, "pending")
                        .orderByDesc(Approval::getCreatedAt));
    }

    @Override
    public List<Approval> getApprovalHistory(Long userId) {
//        return approvalMapper.findHistoryByApproverId(userId);

        return approvalMapper.selectList(
                new LambdaQueryWrapper<Approval>()
                        .eq(Approval::getApproverId, userId)
                        .orderByDesc(Approval::getCreatedAt));
    }

    @Override
    @Transactional
    public void handleApproval(Long approvalId, Long userId, String action, String comment) {
        Approval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new RuntimeException("Approval not found");
        }
        if (!"pending".equals(approval.getAction())) {
            throw new RuntimeException("Approval already processed");
        }
        // 校验审批人是否有权限
        if (!Objects.equals(approval.getApproverId(), userId)) {
            // 进一步检查是否为系统管理员（role_id=1）
            List<UserRole> userRoles = userRoleMapper.findByUserIdAndEnv(userId, "all");
            boolean isAdmin = userRoles.stream().anyMatch(ur -> ur.getRoleId() == 1);
            if (!isAdmin) {
                throw new RuntimeException("Permission denied: only assigned approver or admin can handle");
            }
        }

        // 更新审批单
        approval.setAction(action.equals("approve") ? "approved" : "rejected");

        approval.setComment(comment);
        approvalMapper.updateById(approval);

        // 更新关联任务表的审批信息（如果需要）
        DeploymentTask task = deploymentTaskMapper.selectById(approval.getTaskId());
        if (task != null) {
            task.setApprovedBy(userId);
            task.setApprovalTime(Instant.from(LocalDateTime.now()));
            // 如果审批通过且任务处于等待审批状态，可以推进任务状态
            if ("approve".equals(action) && "waiting_approval".equals(task.getStatus())) {
                task.setStatus("approved");
                // 具体推进逻辑由各业务模块回调实现，这里只更新状态
            }
            deploymentTaskMapper.updateById(task);
        }

        log.info("Approval {} {} by user {}", approvalId, action, userId);
    }
}
