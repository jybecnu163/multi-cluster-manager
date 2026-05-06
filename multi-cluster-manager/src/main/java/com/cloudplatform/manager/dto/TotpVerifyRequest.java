package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TotpVerifyRequest {
    @NotNull
    private Integer code;
}