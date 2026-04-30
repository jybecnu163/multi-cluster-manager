package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clusters")
@Getter
@Setter
public class Cluster {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "env_type", nullable = false, length = 20)
    private String envType;

    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;

    @Column(name = "kubeconfig_encrypted", nullable = false, columnDefinition = "TEXT")
    private String kubeconfigEncrypted;

    @Column(name = "ca_cert_encrypted", columnDefinition = "TEXT")
    private String caCertEncrypted;

    @Column(name = "token_encrypted", columnDefinition = "TEXT")
    private String tokenEncrypted;

    @Column(length = 20)
    private String status = "offline";

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}