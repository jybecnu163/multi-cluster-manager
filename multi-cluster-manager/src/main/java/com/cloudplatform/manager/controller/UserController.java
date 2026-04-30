package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listUsers(@RequestParam(required = false) UUID departmentId) {
        return notImplemented();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Object request) {
        return notImplemented();
    }

    @PutMapping("/{user_id}/roles")
    public ResponseEntity<?> assignRole(@PathVariable UUID userId, @RequestBody Object roleAssignment) {
        return notImplemented();
    }
}