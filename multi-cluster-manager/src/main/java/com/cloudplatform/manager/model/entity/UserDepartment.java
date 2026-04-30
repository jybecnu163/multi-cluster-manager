package com.cloudplatform.manager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_departments")
@Getter
@Setter
public class UserDepartment {
    @EmbeddedId
    private UserDepartmentId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("departmentId")
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Embeddable
    @Getter
    @Setter
    public static class UserDepartmentId implements java.io.Serializable {
        @Column(name = "user_id")
        private UUID userId;
        @Column(name = "department_id")
        private UUID departmentId;
    }
}