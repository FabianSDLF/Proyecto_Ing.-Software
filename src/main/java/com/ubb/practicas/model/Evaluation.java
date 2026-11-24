package com.ubb.practicas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ubb.practicas.dto.EvaluationCriteriaDTO;
import com.ubb.practicas.model.enums.EvaluationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "Evaluaciones",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_evaluaciones_assignment_type", columnNames = {"practice_assignment_id", "type"})
        }
)
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

    @Column(name = "rubric_version", length = 50)
    private String rubricVersion;

    @Column(name = "revision")
    private Long revision = 1L;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "max_points")
    private Integer maxPoints;

    private Float score;

    @Column(length = 1000)
    private String qualitativeComments;

    private Boolean isFinalized = false;
    private String digitalSignature;

    private LocalDateTime evaluationDate;
    private LocalDateTime createdAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "finalized_by_id")
    private Long finalizedById;

    @Column(name = "finalized_by_email")
    private String finalizedByEmail;

    @Column(name = "finalized_by_name")
    private String finalizedByName;

    @Column(name = "finalized_by_role", length = 100)
    private String finalizedByRole;

    @JsonIgnore
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    @ToString.Exclude
    private String snapshotJson;

    @Column(name = "snapshot_hash", length = 64)
    private String snapshotHash;

    // Transient contract fields populated when returning DTO representations
    @JsonProperty("practiceAssignmentId")
    public Long resolvePracticeAssignmentId() {
        return practiceAssignment != null ? practiceAssignment.getId() : null;
    }

    @Transient
    @ToString.Exclude
    private String instrumentName;

    @Transient
    @ToString.Exclude
    private String area;

    @Transient
    @ToString.Exclude
    private Integer visit;

    @Transient
    @ToString.Exclude
    private Boolean historicalManual;

    @Transient
    @ToString.Exclude
    private Integer observedIndicators;

    @Transient
    @ToString.Exclude
    private Integer totalIndicators;

    @Transient
    @ToString.Exclude
    private List<EvaluationCriteriaDTO> criteria;

    public Evaluation(
            Long id,
            PracticeAssignment practiceAssignment,
            User evaluator,
            EvaluationType type,
            Float score,
            String qualitativeComments,
            Boolean isFinalized,
            String digitalSignature,
            LocalDateTime evaluationDate,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.practiceAssignment = practiceAssignment;
        this.evaluator = evaluator;
        this.type = type;
        this.score = score;
        this.qualitativeComments = qualitativeComments;
        this.isFinalized = isFinalized;
        this.digitalSignature = digitalSignature;
        this.evaluationDate = evaluationDate;
        this.createdAt = createdAt;
        this.revision = 1L;
    }
}
