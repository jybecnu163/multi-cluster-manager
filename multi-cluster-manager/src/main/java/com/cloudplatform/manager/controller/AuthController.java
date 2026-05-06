package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.LoginRequest;
import com.cloudplatform.manager.dto.LoginResponse;
import com.cloudplatform.manager.dto.TotpVerifyRequest;
import com.cloudplatform.manager.security.CurrentUserDetails;
import com.cloudplatform.manager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends BaseController{
    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", 8 * 3600));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setup2fa() {
        CurrentUserDetails user = (CurrentUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String provisioningUri = authService.setupTotp(user.getUserId());
        Map<String, String> resp = new HashMap<>();
        resp.put("provisioning_uri", provisioningUri);
        resp.put("qr_code_url", "https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl=" + provisioningUri);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<Void> verify2fa(@Valid @RequestBody TotpVerifyRequest request) {
        CurrentUserDetails user = (CurrentUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        boolean valid = authService.verifyTotp(user.getUserId(), request.getCode());
        if (!valid) {
            throw new RuntimeException("Invalid TOTP code");
        }
        return ResponseEntity.ok().build();
    }
}