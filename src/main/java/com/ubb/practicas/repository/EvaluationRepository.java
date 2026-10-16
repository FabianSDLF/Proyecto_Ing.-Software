package com.ubb.practicas.repository;

import com.ubb.practicas.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByPracticeAssignmentId(Long practiceAssignmentId);
}
