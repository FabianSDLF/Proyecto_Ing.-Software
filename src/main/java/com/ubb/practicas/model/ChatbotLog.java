package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Registros_Chatbot")
public class ChatbotLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lessonPlan_id")
    private LessonPlan lessonPlan;
    @Column(length = 1000)
    private String aiFeedback;
    private Boolean aiApproved;
    private Integer iterationNumber;
    private LocalDateTime reviewDate;
}
