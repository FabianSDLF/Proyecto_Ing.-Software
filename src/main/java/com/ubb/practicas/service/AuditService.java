package com.ubb.practicas.service;

import com.ubb.practicas.model.AuditLog;
import com.ubb.practicas.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository logs;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String email, String action, String resource, int status, String ip) {
        logs.save(new AuditLog(email, action, resource.substring(0, Math.min(1000, resource.length())), status, ip));
    }
}
