package com.cloudplatform.manager.service;

import com.cloudplatform.manager.model.entity.Approval;

import java.util.List;

public interface ApprovalService {
    /**
     * 创建审批单（供其他模块调用，不暴露REST）
     *
     * @param taskId       关联的任务ID
     * @param requesterId  审批人用户ID
     * @param approverRole 审批人角色
     * @param timeoutHours 超时小时数
     * @return 审批单ID
     */
    Long createApproval(Long taskId, Long requesterId, String approverRole, int timeoutHours);

    void approve(Long approvalId, Long approverId, String comment);

    void reject(Long approvalId, Long approverId, String comment);


    /**
     * 创建审批单（供其他模块调用，不暴露REST）
     *
     * @param taskId       关联的任务ID
     * @param approverId   审批人用户ID
     * @param timeoutHours 超时小时数
     * @return 审批单ID
     */
    Long createApproval(Long taskId, Long approverId, int timeoutHours);

    /**
     * 获取当前用户待审批列表
     *
     * @param userId 当前用户ID
     * @return 待审批单列表
     */
    List<Approval> getPendingApprovals(Long userId);

    /**
     * 获取当前用户已审批历史
     *
     * @param userId 当前用户ID
     * @return 历史审批单列表
     */
    List<Approval> getApprovalHistory(Long userId);

    /**
     * 处理审批
     *
     * @param approvalId 审批单ID
     * @param userId     操作人ID
     * @param action     approve / reject
     * @param comment    意见
     * @throws RuntimeException 权限不足或重复操作
     */
    void handleApproval(Long approvalId, Long userId, String action, String comment);
}