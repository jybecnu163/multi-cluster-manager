package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualScaleRequest {
    @Min(0)
    private Integer targetReplicas;

    @NotBlank
    private String reason;

    private Boolean ignoreApproval = false;
}
