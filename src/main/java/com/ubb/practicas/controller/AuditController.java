package com.ubb.practicas.controller;

import com.ubb.practicas.model.AuditLog;
import com.ubb.practicas.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/auditoria")
@RequiredArgsConstructor
public class AuditController {
    private final AuditLogRepository logs;
    @GetMapping
    @Transactional(readOnly = true)
    public Page<AuditLog> list(@RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion, @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return logs.findAll((root, query, cb) -> {
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (usuario != null) conditions.add(cb.equal(root.get("userEmail"), usuario));
            if (accion != null) conditions.add(cb.equal(root.get("action"), accion));
            if (desde != null) conditions.add(cb.greaterThanOrEqualTo(root.get("createdAt"), desde));
            if (hasta != null) conditions.add(cb.lessThanOrEqualTo(root.get("createdAt"), hasta));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), Sort.by("id").descending()));
    }
}
