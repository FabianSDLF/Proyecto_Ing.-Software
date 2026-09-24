package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.EvaluationType;
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
@Table(name = "Evaluaciones")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_assignment_id")
    @ToString.Exclude
    private PracticeAssignment practiceAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    @ToString.Exclude
    private User evaluator;

    @Enumerated(EnumType.STRING)
    private EvaluationType type;

    private Float score;

    @Column(length = 1000)
    private String qualitativeComments;

    private LocalDateTime evaluationDate;
    private LocalDateTime createdAt;
}
