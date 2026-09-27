package com.ubb.practicas.model;

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

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_plan_id")
    @ToString.Exclude
    private LessonPlan parentPlan;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isOwnedBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.isStudent(email);
    }

    public boolean hasDocument(Long documentId) {
        return documentId != null && this.document != null && documentId.equals(this.document.getId());
    }
}
