package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.model.enums.MathAxis;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Planificaciones")
public class LessonPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "practice_assignment_id")
    private PracticeAssignment practiceAssignment;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_id")
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
    @ManyToOne
    @JoinColumn(name = "parent_plan_id")
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
