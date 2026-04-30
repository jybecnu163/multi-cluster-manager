package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listDepartments(@RequestParam(required = false) UUID companyId) {
        return notImplemented();
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Object request) {
        return notImplemented();
    }

    @GetMapping("/{department_id}/settings")
    public ResponseEntity<?> getDepartmentSettings(@PathVariable UUID departmentId) {
        return notImplemented();
    }

    @PatchMapping("/{department_id}/settings")
    public ResponseEntity<?> updateDepartmentSettings(@PathVariable UUID departmentId,
                                                      @RequestBody Object settings) {
        return notImplemented();
    }
}