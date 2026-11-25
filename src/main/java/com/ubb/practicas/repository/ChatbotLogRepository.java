package com.ubb.practicas.repository;

import com.ubb.practicas.model.ChatbotLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotLogRepository extends JpaRepository<ChatbotLog, Long> {
    java.util.Optional<ChatbotLog> findFirstByLessonPlanIdOrderByReviewDateDesc(Long lessonPlanId);
    java.util.List<ChatbotLog> findByLessonPlanId(Long lessonPlanId);
}
