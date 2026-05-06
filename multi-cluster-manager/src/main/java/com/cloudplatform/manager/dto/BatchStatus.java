package com.cloudplatform.manager.dto;

import lombok.Data;

@Data
public class BatchStatus {
    private Integer batchNumber;
    private String status; // pending, in_progress, success, failed
    private Integer replicasUpdated;
    private Long startTime;
    private Long endTime;
}