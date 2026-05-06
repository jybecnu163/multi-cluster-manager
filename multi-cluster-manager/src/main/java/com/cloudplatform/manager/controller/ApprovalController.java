package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.model.entity.Approval;
import com.cloudplatform.manager.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "审批")
public class ApprovalController extends BaseController{

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('部门主管') or hasRole('系统管理员')")
    @Operation(summary = "待审批任务列表")
    public ResponseEntity<List<Approval>> getPendingApprovals(@RequestAttribute("userId") Long userId) {
        List<Approval> pending = approvalService.getPendingApprovals(userId);
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('部门主管') or hasRole('系统管理员') or hasRole('审计员')")
    @Operation(summary = "已审批历史记录")
    public ResponseEntity<List<Approval>> getApprovalHistory(@RequestAttribute("userId") Long userId,
                                                              @RequestParam(defaultValue = "1") int page) {
        // page 参数忽略，简化返回全部历史
        List<Approval> history = approvalService.getApprovalHistory(userId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{approval_id}")
    @PreAuthorize("hasRole('部门主管') or hasRole('系统管理员')")
    @Operation(summary = "审批操作")
    public ResponseEntity<Map<String, String>> handleApproval(
            @PathVariable("approval_id") Long approvalId,
            @RequestAttribute("userId") Long userId,
            @RequestBody ApprovalRequest request) {
        approvalService.handleApproval(approvalId, userId, request.getAction(), request.getComment());
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Approval processed successfully");
        return ResponseEntity.ok(resp);
    }

    // 内部请求类
    static class ApprovalRequest {
        private String action;
        private String comment;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
