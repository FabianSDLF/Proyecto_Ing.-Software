package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.MessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Mensajes_Chatbot")
public class ChatbotMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbotLog_id")
    @ToString.Exclude
    private ChatbotLog chatbotLog;

    @Enumerated(EnumType.STRING)
    private MessageRole senderRole;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer orderNum;
    private LocalDateTime timestamp;
}
