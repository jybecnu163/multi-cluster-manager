package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auto_scaling_policies")
@Getter
@Setter
public class AutoScalingPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_instance_id", nullable = false)
    private UUID serviceInstanceId;

    private Boolean enabled = true;

    @Column(name = "metric_type", nullable = false, length = 20)
    private String metricType;

    @Column(name = "target_threshold", nullable = false)
    private Integer targetThreshold;

    @Column(name = "metric_query", columnDefinition = "TEXT")
    private String metricQuery;

    @Column(name = "fallback_source", length = 20)
    private String fallbackSource = "prometheus";

    @Column(name = "min_replicas", nullable = false)
    private Integer minReplicas;

    @Column(name = "max_replicas", nullable = false)
    private Integer maxReplicas;

    @Column(name = "cooldown_seconds")
    private Integer cooldownSeconds = 300;

    @Column(name = "scale_down_delay_minutes")
    private Integer scaleDownDelayMinutes = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}