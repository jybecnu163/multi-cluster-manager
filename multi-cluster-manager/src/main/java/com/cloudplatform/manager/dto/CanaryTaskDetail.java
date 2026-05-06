package com.cloudplatform.manager.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class CanaryTaskDetail {
    private Long id;
    private Long serviceInstanceId;
    private String status;
    private Integer currentStage;
    private String targetImage;
    private Integer canaryWeight;
    private Long approvalId;
    private Integer approvalTimeoutHours;
    private Instant createdAt;
}