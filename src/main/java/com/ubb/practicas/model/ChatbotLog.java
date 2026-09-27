package com.ubb.practicas.model;

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
@Table(name = "Registros_Chatbot")
public class ChatbotLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lessonPlan_id")
    @ToString.Exclude
    private LessonPlan lessonPlan;

    @Column(length = 1000)
    private String aiFeedback;

    private Boolean aiApproved;
    private Integer iterationNumber;
    private LocalDateTime reviewDate;
}
