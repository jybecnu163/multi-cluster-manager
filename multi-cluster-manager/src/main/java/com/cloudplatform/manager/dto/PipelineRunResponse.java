package com.cloudplatform.manager.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class PipelineRunResponse {
    private Long id;
    private Long pipelineId;
    private String status;
    private Instant startedAt;
    private Instant finishedAt;
}