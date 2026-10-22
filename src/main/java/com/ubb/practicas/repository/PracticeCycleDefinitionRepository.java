package com.ubb.practicas.repository;

import com.ubb.practicas.model.PracticeCycleDefinition;
import com.ubb.practicas.model.enums.PracticeCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeCycleDefinitionRepository extends JpaRepository<PracticeCycleDefinition, Long> {

    Optional<PracticeCycleDefinition> findByCode(PracticeCycle code);

    boolean existsByCode(PracticeCycle code);

    List<PracticeCycleDefinition> findAllByOrderByDisplayOrderAsc();
}
