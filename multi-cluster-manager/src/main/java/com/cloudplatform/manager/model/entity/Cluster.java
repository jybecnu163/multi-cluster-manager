package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;

@Data
@TableName("clusters")
public class Cluster {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String envType;
    private String apiEndpoint;
    private String kubeconfigEncrypted;
    private String caCertEncrypted;
    private String tokenEncrypted;
    private String status;
    private Instant lastHeartbeat;
    @TableField("created_at")
    private Instant createdAt;
}