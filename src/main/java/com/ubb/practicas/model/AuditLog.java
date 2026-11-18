package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_auditoria")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(updatable = false) private String userEmail;
    @Column(updatable = false) private String action;
    @Column(updatable = false, length = 1000) private String resource;
    @Column(updatable = false) private Integer status;
    @Column(updatable = false) private String ipAddress;
    @Column(updatable = false) private LocalDateTime createdAt;

    public AuditLog(String userEmail, String action, String resource, int status, String ipAddress) {
        this.userEmail = userEmail; this.action = action; this.resource = resource;
        this.status = status; this.ipAddress = ipAddress; this.createdAt = LocalDateTime.now();
    }
}
