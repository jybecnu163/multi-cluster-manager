package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController extends BaseController {

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingApprovals() {
        return notImplemented();
    }

    @GetMapping("/history")
    public ResponseEntity<?> getApprovalHistory(@RequestParam(defaultValue = "1") int page) {
        return notImplemented();
    }

    @PostMapping("/{approval_id}")
    public ResponseEntity<?> handleApproval(@PathVariable UUID approvalId,
                                            @RequestBody Object decision) {
        return notImplemented();
    }
}