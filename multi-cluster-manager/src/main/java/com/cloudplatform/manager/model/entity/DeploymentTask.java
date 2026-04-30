package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployment_tasks")
@Getter
@Setter
public class DeploymentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_instance_id", nullable = false)
    private UUID serviceInstanceId;

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "current_stage")
    private Integer currentStage;

    @Column(name = "strategy_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String strategyJson;

    @Column(name = "target_image")
    private String targetImage;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approval_time")
    private Instant approvalTime;

    @Column(name = "approval_timeout_hours")
    private Integer approvalTimeoutHours = 24;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}