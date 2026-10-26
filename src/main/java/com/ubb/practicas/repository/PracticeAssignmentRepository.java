package com.ubb.practicas.repository;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeAssignmentRepository extends JpaRepository<PracticeAssignment, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    List<PracticeAssignment> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    Optional<PracticeAssignment> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    List<PracticeAssignment> findBySubjectProfessorId(Long professorId);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    List<PracticeAssignment> findByCollaboratorId(Long collaboratorId);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    List<PracticeAssignment> findByTutorId(Long tutorId);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    List<PracticeAssignment> findByStudentEmail(String email);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "subject.professor.role", "center", "collaborator", "collaborator.role", "tutor", "tutor.role", "period"})
    List<PracticeAssignment> findByStudentEmailAndStatus(String email, AssignmentStatus status);

    boolean existsBySubjectId(Long subjectId);

    boolean existsByCenterId(Long centerId);

    boolean existsByPeriodId(Long periodId);
}
