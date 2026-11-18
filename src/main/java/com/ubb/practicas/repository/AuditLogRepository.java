package com.ubb.practicas.repository;
import com.ubb.practicas.model.AuditLog;
import org.springframework.data.jpa.repository.*;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {}
