package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("deployment_tasks")
public class DeploymentTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long serviceInstanceId;
    private String taskType;
    private String status;
    private Integer currentStage;
    private String strategyJson;
    private String targetImage;
    private Long createdBy;
    private Long approvedBy;
    private Instant approvalTime;
    private Integer approvalTimeoutHours;
    private Instant createdAt;
    private Instant completedAt;
}