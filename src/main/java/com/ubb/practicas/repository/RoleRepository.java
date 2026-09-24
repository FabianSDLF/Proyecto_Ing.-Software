package com.ubb.practicas.repository;

import com.ubb.practicas.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String roleName);

    Optional<Role> findOptionalByName(String roleName);
}
