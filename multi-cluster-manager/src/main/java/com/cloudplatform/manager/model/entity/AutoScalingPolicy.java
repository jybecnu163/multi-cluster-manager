package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("auto_scaling_policies")
public class AutoScalingPolicy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long serviceInstanceId;
    private Boolean enabled = true;
    private String metricType;   // cpu, memory, qps, custom
    private Integer targetThreshold;
    private String metricQuery;   // 自定义 PromQL
    private String fallbackSource; // prometheus 或 metrics_server
    private Integer minReplicas;
    private Integer maxReplicas;
    private Integer cooldownSeconds = 300;
    private Integer scaleDownDelayMinutes = 10;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
