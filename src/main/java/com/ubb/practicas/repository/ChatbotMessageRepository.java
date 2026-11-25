package com.ubb.practicas.repository;

import com.ubb.practicas.model.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    void deleteByChatbotLogIdIn(Collection<Long> chatbotLogIds);
}
