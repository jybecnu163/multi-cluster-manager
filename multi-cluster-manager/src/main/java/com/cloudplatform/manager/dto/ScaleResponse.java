package com.cloudplatform.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScaleResponse {
    private Long taskId;
    private Boolean requiresApproval;
}
