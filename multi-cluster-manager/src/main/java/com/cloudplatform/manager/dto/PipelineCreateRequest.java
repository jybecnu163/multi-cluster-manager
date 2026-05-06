package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PipelineCreateRequest {
    @NotBlank
    private String name;
    @NotNull
    private List<Step> steps;
    private String triggerType;   // api, webhook, manual
    private String webhookSecret;
    private Integer approvalTimeoutHours = 24;

    @Data
    public static class Step {
        private String type;       // git-clone, build-image, unit-test, image-scan, deploy, approval
        private String name;
        private String image;
        private String script;
        private String repo;
        private String branch;
        // todo why id is string
        private Long targetServiceId;
        private String deploymentMethod; // canary 或 batch
        private Object config;
    }
}