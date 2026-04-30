package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_runs")
@Getter
@Setter
public class PipelineRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pipeline_id")
    private UUID pipelineId;

    @Column(length = 20)
    private String status;

    @Column(name = "input_params", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String inputParams;

    @Column(name = "approval_needed")
    private Boolean approvalNeeded;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}