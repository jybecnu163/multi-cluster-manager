package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.RoleAssignmentRequest;
import com.cloudplatform.manager.dto.UserRequest;
import com.cloudplatform.manager.dto.UserResponse;
import com.cloudplatform.manager.model.entity.User;
import com.cloudplatform.manager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController{
    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员') or hasRole('部门主管')")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(userService.listUsers(departmentId)
                .stream().map(u -> getUserResponse(u))
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request.getName(), request.getEmail(), request.getPassword(),
                request.getDepartmentIds(), request.getPrimaryDepartmentId());
        UserResponse userR = getUserResponse(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(userR);
    }

    private static UserResponse getUserResponse(User user) {
        UserResponse userR = new UserResponse();
        userR.setId(user.getId());
        userR.setName(user.getName());
        userR.setEmail(user.getEmail());
        userR.setCreatedAt(userR.getCreatedAt());
        return userR;
    }

    @PutMapping("/{user_id}/roles")
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @Valid @RequestBody RoleAssignmentRequest request) {
        return notImplemented();
    }
}