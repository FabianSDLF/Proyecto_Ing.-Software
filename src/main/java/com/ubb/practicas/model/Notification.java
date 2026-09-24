package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Notificaciones")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "targetUser_id")
    @ToString.Exclude
    private User targetUser;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    @Column(name = "is_read")
    private Boolean read;

    private LocalDateTime createdAt;
    private Long referenceId;
    private String referenceType;
}
