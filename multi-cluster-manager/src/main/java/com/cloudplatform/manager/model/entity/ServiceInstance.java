package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "service_instances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "cluster_id"})
})
@Getter
@Setter
public class ServiceInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "cluster_id", nullable = false)
    private UUID clusterId;

    @Column(nullable = false, length = 64)
    private String namespace;

    @Column(name = "workload_type", nullable = false, length = 20)
    private String workloadType;

    @Column(name = "workload_name", nullable = false, length = 128)
    private String workloadName;

    private Integer replicas = 1;

    @Column(name = "nacos_service_name", length = 128)
    private String nacosServiceName;
}