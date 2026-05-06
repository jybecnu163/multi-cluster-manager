package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("pipelines")
public class Pipeline {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String steps; // JSON 存储步骤数组
    private String triggerType; // api, webhook, manual
    private Integer approvalTimeoutHours;
    private Long createdBy;
    private Instant createdAt;
    private String webhookSecret;  // 用于签名验证
}