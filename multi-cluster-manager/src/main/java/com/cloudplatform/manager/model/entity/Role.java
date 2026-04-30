package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {
    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 32)
    private String name;

    @Column(name = "env_permission_mask")
    private Integer envPermissionMask;
}