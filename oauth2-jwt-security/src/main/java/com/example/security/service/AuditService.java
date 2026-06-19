package com.example.security.service;

import com.example.security.model.AuditLog;
import com.example.security.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String username, String action, String ipAddress, boolean success, String message) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setIpAddress(ipAddress);
        log.setSuccess(success);
        log.setMessage(message);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }
}
