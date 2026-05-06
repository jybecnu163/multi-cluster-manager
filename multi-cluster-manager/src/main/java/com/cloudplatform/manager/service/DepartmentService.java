package com.cloudplatform.manager.service;

import com.cloudplatform.manager.model.entity.Department;
import com.cloudplatform.manager.model.entity.DepartmentSettings;

import java.util.List;

public interface DepartmentService {
    List<Department> listDepartments(Long companyId);

    Department createDepartment(Long companyId, String name, Long directorUserId);

    DepartmentSettings getSettings(Long departmentId);

    void updateSettings(Long departmentId, Boolean allowOpsBypassProdScale);
}
