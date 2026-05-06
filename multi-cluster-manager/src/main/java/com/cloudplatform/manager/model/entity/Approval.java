package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("approvals")
public class Approval {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long approverId;
    /**
     * 审批单状态: pending, approved, rejected, expired
     */
    private String action;          // approve, reject, expired
    private String comment;
    private LocalDateTime createdAt;


    private String status;

    /**
     * 超时时间（绝对时间），用于定时任务判断
     */
    private LocalDateTime expiresAt;
}
