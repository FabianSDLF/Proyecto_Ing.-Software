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

    private Boolean isFinalized = false;
    private String digitalSignature;

    private LocalDateTime evaluationDate;
    private LocalDateTime createdAt;

    @Transient
    @ToString.Exclude
    private java.util.List<EvaluationCriteria> criteria;

    public Evaluation(
            Long identificador,
            PracticeAssignment asignacionPractica,
            User evaluador,
            EvaluationType tipoEvaluacion,
            Float calificacion,
            String comentariosCualitativos,
            Boolean estaFinalizada,
            String firmaDigital,
            LocalDateTime fechaEvaluacion,
            LocalDateTime fechaCreacion) {
        this.id = identificador;
        this.practiceAssignment = asignacionPractica;
        this.evaluator = evaluador;
        this.type = tipoEvaluacion;
        this.score = calificacion;
        this.qualitativeComments = comentariosCualitativos;
        this.isFinalized = estaFinalizada;
        this.digitalSignature = firmaDigital;
        this.evaluationDate = fechaEvaluacion;
        this.createdAt = fechaCreacion;
    }
}
