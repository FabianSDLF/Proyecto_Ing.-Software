package com.ubb.practicas.repository;

import com.ubb.practicas.model.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {
    List<LessonPlan> findByPracticeAssignment_Student_Email(String email);
    Optional<LessonPlan> findByDocumentId(Long documentId);
}
