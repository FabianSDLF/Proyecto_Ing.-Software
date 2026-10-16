package com.ubb.practicas.repository;

import com.ubb.practicas.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role"})
    User findByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findOptionalByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByRoleName(String roleName);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"role"})
    List<User> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findById(@NonNull Long id);
}
