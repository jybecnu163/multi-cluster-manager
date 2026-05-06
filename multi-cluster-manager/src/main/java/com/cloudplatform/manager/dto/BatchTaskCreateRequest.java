package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchTaskCreateRequest {
    @NotNull
    private Long serviceInstanceId;
    @NotNull
    private String targetImage;
    @NotNull
    private BatchConfig batchConfig;

    @Data
    public static class BatchConfig {
        @NotNull
        private String batchSizeType;   // "count" 或 "percentage"
        @NotNull
        private Integer batchValue;       // 固定数量或百分比 (1-100)
        @NotNull
        private Integer intervalSeconds;
        @NotNull
        private Boolean requireConfirmation;
        private Boolean statefulsetPartitionMode = true;
        private FailureCondition failureCondition;

        @Data
        public static class FailureCondition {
            private Integer minReadyPercent = 80;
            private Double maxErrorLogSpike = 2.0;
            private Integer minAbsoluteErrorIncrement = 10;
        }
    }
}