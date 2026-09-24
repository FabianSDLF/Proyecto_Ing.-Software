package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.AssignmentStatus;
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
@Table(name = "Asignacion_Practicas")
public class PracticeAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    @ToString.Exclude
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    @ToString.Exclude
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    @ToString.Exclude
    private PracticeCenter center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborator_id")
    @ToString.Exclude
    private User collaborator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    @ToString.Exclude
    private User tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    @ToString.Exclude
    private AcademicPeriod period;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    private LocalDateTime createdAt;

    public boolean isStudent(String email) {
        return email != null && this.student != null && email.equalsIgnoreCase(this.student.getEmail());
    }

    public boolean isProfessor(String email) {
        return email != null && this.subject != null && this.subject.getProfessor() != null 
                && email.equalsIgnoreCase(this.subject.getProfessor().getEmail());
    }

    public boolean isCollaborator(String email) {
        return email != null && this.collaborator != null && email.equalsIgnoreCase(this.collaborator.getEmail());
    }

    public boolean isTutor(String email) {
        return email != null && this.tutor != null && email.equalsIgnoreCase(this.tutor.getEmail());
    }

    public boolean hasParticipant(String email) {
        if (email == null) return false;
        return isStudent(email) || isProfessor(email) || isCollaborator(email) || isTutor(email);
    }
}
