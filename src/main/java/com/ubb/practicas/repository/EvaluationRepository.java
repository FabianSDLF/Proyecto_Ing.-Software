package com.ubb.practicas.repository;

import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.model.enums.EvaluationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    @EntityGraph(attributePaths = {
            "practiceAssignment",
            "practiceAssignment.student",
            "practiceAssignment.student.role",
            "practiceAssignment.subject",
            "practiceAssignment.subject.professor",
            "practiceAssignment.center",
            "practiceAssignment.collaborator",
            "practiceAssignment.tutor",
            "practiceAssignment.period",
            "evaluator",
            "evaluator.role"
    })
    List<Evaluation> findByPracticeAssignmentId(Long practiceAssignmentId);

    @EntityGraph(attributePaths = {
            "practiceAssignment",
            "practiceAssignment.student",
            "practiceAssignment.student.role",
            "practiceAssignment.subject",
            "practiceAssignment.subject.professor",
            "practiceAssignment.center",
            "practiceAssignment.collaborator",
            "practiceAssignment.tutor",
            "practiceAssignment.period",
            "evaluator",
            "evaluator.role"
    })
    Optional<Evaluation> findByPracticeAssignmentIdAndType(Long practiceAssignmentId, EvaluationType type);
}
