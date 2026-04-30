package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends BaseController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        return notImplemented();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return notImplemented();
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2fa() {
        return notImplemented();
    }
}