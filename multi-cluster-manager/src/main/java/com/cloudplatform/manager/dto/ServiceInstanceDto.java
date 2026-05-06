package com.cloudplatform.manager.dto;

import lombok.Data;

@Data
public class ServiceInstanceDto {
    private Long id;
    private String name;
    private Long departmentId;
    private Long clusterId;
    private String namespace;
    private String workloadType;
    private String workloadName;
    private Integer replicas;
    private String nacosServiceName;
    private String nacosHealthStatus;
    // 实时状态，从K8s获取
    private String currentStatus;
    private Integer readyReplicas;
    private Long uptimeSeconds;
}
