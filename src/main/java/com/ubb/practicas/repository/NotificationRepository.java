package com.ubb.practicas.repository;

import com.ubb.practicas.model.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = {"targetUser", "targetUser.role"})
    List<Notification> findByTargetUser_EmailOrderByCreatedAtDesc(String correoUsuario);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"targetUser", "targetUser.role"})
    Optional<Notification> findById(@NonNull Long id);
}
