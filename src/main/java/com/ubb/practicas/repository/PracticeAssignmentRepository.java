package com.ubb.practicas.repository;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeAssignmentRepository extends JpaRepository<PracticeAssignment, Long> {
    List<PracticeAssignment> findBySubjectProfessorId(Long professorId);
    List<PracticeAssignment> findByCollaboratorId(Long collaboratorId);
    List<PracticeAssignment> findByTutorId(Long tutorId);
    List<PracticeAssignment> findByStudentEmail(String email);
    List<PracticeAssignment> findByStudentEmailAndStatus(String email, AssignmentStatus status);
    boolean existsBySubjectId(Long subjectId);
}
