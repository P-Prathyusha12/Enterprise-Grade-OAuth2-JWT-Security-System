package com.example.security.controller;

import com.example.security.model.AuditLog;
import com.example.security.repository.AuditLogRepository;
import com.example.security.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(UserRepository userRepository,
                           AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Operation(summary = "Get system dashboard stats")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> data = new HashMap<>();
        data.put("loggedInAs", userDetails.getUsername());
        data.put("totalUsers", userRepository.count());
        data.put("authorities", userDetails.getAuthorities());
        data.put("recentLogs", auditLogRepository.findAll().stream().limit(10).toList());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Get all audit logs")
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @Operation(summary = "Get audit logs for a specific user")
    @GetMapping("/audit-logs/{username}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditLogRepository.findByUsernameOrderByTimestampDesc(username));
    }
}
