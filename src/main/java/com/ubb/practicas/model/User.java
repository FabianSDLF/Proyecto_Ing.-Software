package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "usuarios")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    @Column(unique = true)
    private String email;
    private String passwordHash;
    private Boolean active;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
