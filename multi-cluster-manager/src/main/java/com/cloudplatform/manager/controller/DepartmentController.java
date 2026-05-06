package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.DepartmentRequest;
import com.cloudplatform.manager.model.entity.Department;
import com.cloudplatform.manager.model.entity.DepartmentSettings;
import com.cloudplatform.manager.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController extends BaseController{
    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员') or hasRole('审计员')")
    public ResponseEntity<List<Department>> listDepartments(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(departmentService.listDepartments(companyId));
    }

    @PostMapping
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<Department> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        Department dept = departmentService.createDepartment(request.getCompanyId(), request.getName(), request.getDirectorUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dept);
    }

    @GetMapping("/{department_id}/settings")
    @PreAuthorize("hasRole('系统管理员') or hasRole('部门主管')")
    public ResponseEntity<DepartmentSettings> getSettings(@PathVariable("department_id") Long departmentId) {
        return ResponseEntity.ok(departmentService.getSettings(departmentId));
    }

    @PatchMapping("/{department_id}/settings")
    @PreAuthorize("hasRole('系统管理员') or hasRole('部门主管')")
    public ResponseEntity<DepartmentSettings> updateSettings(@PathVariable("department_id") Long departmentId,
                                                             @RequestBody DepartmentSettings settings) {
        departmentService.updateSettings(departmentId, settings.getAllowOpsBypassProdScale());
        return ResponseEntity.ok(settings);
    }
}