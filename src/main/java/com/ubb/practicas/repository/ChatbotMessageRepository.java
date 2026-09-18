package com.ubb.practicas.repository;

import com.ubb.practicas.model.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
}
