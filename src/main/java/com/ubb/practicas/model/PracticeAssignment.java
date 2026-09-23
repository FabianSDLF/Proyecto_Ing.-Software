package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Asignacion_Practicas")
public class PracticeAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;
    @ManyToOne
    @JoinColumn(name = "center_id")
    private PracticeCenter center;
    @ManyToOne
    @JoinColumn(name = "collaborator_id")
    private User collaborator;
    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private User tutor;
    @ManyToOne
    @JoinColumn(name = "period_id")
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
