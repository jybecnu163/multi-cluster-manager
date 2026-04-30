package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "department_settings")
@Getter
@Setter
public class DepartmentSettings {
    @Id
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "allow_ops_bypass_prod_scale", nullable = false)
    private Boolean allowOpsBypassProdScale = false;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;
}