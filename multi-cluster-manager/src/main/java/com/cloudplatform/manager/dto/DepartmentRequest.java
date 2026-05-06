package com.cloudplatform.manager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class DepartmentRequest {
    @NotNull
    private Long companyId;
    @NotBlank
    private String name;
    private Long directorUserId;

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDirectorUserId() {
        return directorUserId;
    }

    public void setDirectorUserId(Long directorUserId) {
        this.directorUserId = directorUserId;
    }
}
