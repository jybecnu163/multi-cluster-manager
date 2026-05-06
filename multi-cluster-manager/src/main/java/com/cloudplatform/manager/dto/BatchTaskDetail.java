package com.cloudplatform.manager.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class BatchTaskDetail {
    private Long id;
    private String status;
    private Integer totalBatches;
    private Integer currentBatch;
    private List<BatchStatus> batchStatuses;
    private Instant createdAt;
}