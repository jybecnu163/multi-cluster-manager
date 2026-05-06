package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.RoleAssignmentRequest;
import com.cloudplatform.manager.model.entity.Role;
import com.cloudplatform.manager.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController extends BaseController{
    @Autowired private RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<List<Role>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @PutMapping("/users/{user_id}/roles")
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<Void> assignRole(@PathVariable("user_id") Long userId,
                                           @Valid @RequestBody RoleAssignmentRequest request) {
        roleService.assignRole(userId, request.getRoleId().shortValue(), request.getEnvType(), request.getDepartmentId());
        return ResponseEntity.ok().build();
    }
}
