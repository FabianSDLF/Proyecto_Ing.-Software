package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.MessageRole;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Mensajes_Chatbot")
public class ChatbotMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbotLog_id")
    private ChatbotLog chatbotLog;
    @Enumerated(EnumType.STRING)
    private MessageRole senderRole;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Integer orderNum;
    private LocalDateTime timestamp;
}
