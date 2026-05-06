package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClusterRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String envType;   // dev, test, prod
    @NotBlank
    private String apiEndpoint;
    @NotBlank
    private String kubeconfig;   // base64 编码的 kubeconfig
    private String caCert;       // 可选
    private String token;        // 可选
}