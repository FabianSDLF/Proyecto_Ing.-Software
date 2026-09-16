package com.ubb.practicas.repository;

import com.ubb.practicas.model.LessonPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {
}
