package com.ubb.practicas.dto;

import com.ubb.practicas.model.User;

public record NotificationRecipientDTO(
        Long id,
        String fullName,
        String email,
        RoleSummary role
) {
    public record RoleSummary(String name) {
    }

    public static NotificationRecipientDTO from(User user) {
        String roleName = (user.getRole() != null) ? user.getRole().getName() : null;
        return new NotificationRecipientDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roleName != null ? new RoleSummary(roleName) : null
        );
    }
}
