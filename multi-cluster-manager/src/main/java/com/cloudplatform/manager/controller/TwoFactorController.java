package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/2fa")
public class TwoFactorController extends BaseController {

    @PostMapping("/setup")
    public ResponseEntity<?> setupTwoFactor() {
        return notImplemented();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody Object code) {
        return notImplemented();
    }
}