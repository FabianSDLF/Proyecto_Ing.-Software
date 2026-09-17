package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.EvaluationType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Evaluaciones")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "practice_assignment_id")
    private PracticeAssignment practiceAssignment;
    @ManyToOne
    @JoinColumn(name = "evaluator_id")
    private User evaluator;
    @Enumerated(EnumType.STRING)
    private EvaluationType type;
    private Float score;
    @Column(length = 1000)
    private String qualitativeComments;
    private LocalDateTime evaluationDate;
    private LocalDateTime createdAt;
}
