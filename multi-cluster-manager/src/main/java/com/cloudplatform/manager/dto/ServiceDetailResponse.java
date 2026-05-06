package com.cloudplatform.manager.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ServiceDetailResponse {
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
    private String startupCommand;
    private Map<String, String> envVariables;
    private String cpuRequest;
    private String memoryRequest;
    private List<PodInfo> pods;

    @Data
    public static class PodInfo {
        private String name;
        private String status;
        private Integer restartCount;
        private String ip;
    }
}
