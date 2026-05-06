package com.cloudplatform.manager.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class ClusterResponse {
    private Long id;
    private String name;
    private String envType;
    private String apiEndpoint;
    private String status;
    private Instant lastHeartbeat;
    private Instant createdAt;
}