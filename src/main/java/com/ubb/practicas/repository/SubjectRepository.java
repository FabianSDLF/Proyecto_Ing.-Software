package com.ubb.practicas.repository;

import com.ubb.practicas.model.Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"professor", "professor.role"})
    List<Subject> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"professor", "professor.role"})
    Optional<Subject> findById(@NonNull Long id);
}
