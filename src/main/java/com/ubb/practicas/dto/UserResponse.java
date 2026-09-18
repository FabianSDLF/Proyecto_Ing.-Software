package com.ubb.practicas.dto;

import com.ubb.practicas.model.Role;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private Boolean active;
    private Role role;
    private LocalDateTime createdAt;

    public UserResponse(Long id, String fullName, String email, Boolean active, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static UserResponse fromUser(com.ubb.practicas.model.User user) {
        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getActive(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
