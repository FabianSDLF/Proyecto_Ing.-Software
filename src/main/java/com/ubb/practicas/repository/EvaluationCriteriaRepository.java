package com.ubb.practicas.repository;

import com.ubb.practicas.model.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Long> {
    List<EvaluationCriteria> findByEvaluationId(Long evaluationId);
    void deleteByEvaluationId(Long evaluationId);
}
