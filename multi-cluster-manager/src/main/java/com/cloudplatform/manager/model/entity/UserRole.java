package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_roles")
@Getter
@Setter
public class UserRole {
    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "env_type", length = 20)
    private String envType;

    @Column(name = "department_id")
    private UUID departmentId;

    @Embeddable
    @Getter
    @Setter
    public static class UserRoleId implements java.io.Serializable {
        @Column(name = "user_id")
        private UUID userId;
        @Column(name = "role_id")
        private Short roleId;
        @Column(name = "env_type", length = 20)
        private String envType;
        @Column(name = "department_id")
        private UUID departmentId;
    }
}