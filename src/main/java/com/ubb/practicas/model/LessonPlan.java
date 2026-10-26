package com.ubb.practicas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.PlanStatus;
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
@SuppressWarnings("JpaDataSourceORMInspection")
@Table(name = "Planificaciones")
public class LessonPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_assignment_id")
    @ToString.Exclude
    private PracticeAssignment practiceAssignment;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @ToString.Exclude
    private PortfolioDocument document;

    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    private String schoolGrade;

    @Enumerated(EnumType.STRING)
    private MathAxis mathAxis;

    private String baseLearningObjective;

    @Column(length = 2000)
    private String objectives;

    @Column(columnDefinition = "TEXT")
    private String contentJson;

    @Column(length = 2000)
    private String teacherFeedback;
    private java.time.LocalDateTime feedbackDate;
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_plan_id")
    @ToString.Exclude
    @JsonIgnore
    private LessonPlan parentPlan;

    public LessonPlan(
            Long id,
            PracticeAssignment practiceAssignment,
            PortfolioDocument document,
            com.ubb.practicas.model.enums.PlanStatus status,
            String schoolGrade,
            com.ubb.practicas.model.enums.MathAxis mathAxis,
            String baseLearningObjective,
            String objectives,
            String contentJson,
            String teacherFeedback,
            Integer version,
            LessonPlan parentPlan,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.practiceAssignment = practiceAssignment;
        this.document = document;
        this.status = status;
        this.schoolGrade = schoolGrade;
        this.mathAxis = mathAxis;
        this.baseLearningObjective = baseLearningObjective;
        this.objectives = objectives;
        this.contentJson = contentJson;
        this.teacherFeedback = teacherFeedback;
        this.version = version;
        this.parentPlan = parentPlan;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @JsonProperty("parentPlanId")
    public Long getParentPlanId() {
        return parentPlan != null ? parentPlan.getId() : null;
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isOwnedBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.isStudent(email);
    }
    public boolean isAccessibleBy(String email) {
        return this.practiceAssignment != null && this.practiceAssignment.hasParticipant(email);
    }

    public boolean hasDocument(Long documentId) {
        return documentId != null && this.document != null && documentId.equals(this.document.getId());
    }
}
