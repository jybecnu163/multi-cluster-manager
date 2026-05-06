package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CanaryTaskCreateRequest {
    @NotNull
    private Long serviceInstanceId;
    @NotNull
    private String targetImage;
    private CanaryStrategy strategy;

    @Data
    public static class CanaryStrategy {
        private Integer canaryReplicas = 1;
        private Boolean autoApproveTraffic = false;
        private List<Integer> autoPromoteSteps; // [5,25] 自动扩大，但默认需手动
        private Boolean rollbackOnError = true;
    }
}