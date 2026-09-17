package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Notificaciones")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "targetUser_id")
    private User targetUser;
    private String type;
    private String message;
    @Column(name = "is_read")
    private Boolean read;
    private LocalDateTime createdAt;
    private Long referenceId;
    private String referenceType;
}
