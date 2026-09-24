package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.TeachingRole;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.PracticeCenterRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PracticeAssignmentServiceImpl implements PracticeAssignmentService {

    private final PracticeAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final PracticeCenterRepository centerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> listarAsignaciones() {
        return assignmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> listarPorColaborador(Long collaboratorId) {
        return assignmentRepository.findByCollaboratorId(collaboratorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> listarPorTutor(Long tutorId) {
        return assignmentRepository.findByTutorId(tutorId);
    }

    @Override
    @Transactional
    public PracticeAssignment crearAsignacion(PracticeAssignment assignment) {
        if (assignment.getStudent() == null || assignment.getStudent().getId() == null) {
            throw new IllegalArgumentException("El estudiante es obligatorio");
        }
        if (assignment.getSubject() == null || assignment.getSubject().getId() == null) {
            throw new IllegalArgumentException("La asignatura es obligatoria");
        }

        if (!userRepository.existsById(assignment.getStudent().getId())) {
            throw new IllegalArgumentException("El estudiante no existe en la base de datos");
        }
        if (!subjectRepository.existsById(assignment.getSubject().getId())) {
            throw new IllegalArgumentException("La asignatura no existe en la base de datos");
        }
        if (assignment.getCenter() != null && assignment.getCenter().getId() != null
                && !centerRepository.existsById(assignment.getCenter().getId())) {
            throw new IllegalArgumentException("El centro de práctica no existe");
        }

        assignment.setCreatedAt(LocalDateTime.now());
        if (assignment.getStatus() == null) {
            assignment.setStatus(AssignmentStatus.ACTIVA);
        }

        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void eliminarAsignacion(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new NoSuchElementException("Asignación no encontrada");
        }
        assignmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeAssignment resolveActiveAssignment(String email, Long explicitAssignmentId) {
        if (explicitAssignmentId != null) {
            Optional<PracticeAssignment> opt = assignmentRepository.findById(explicitAssignmentId);
            if (opt.isPresent()) {
                PracticeAssignment a = opt.get();
                if (a.isStudent(email) && a.getStatus() == AssignmentStatus.ACTIVA) {
                    return a;
                }
            }
            return null;
        }

        List<PracticeAssignment> activeAssignments = assignmentRepository.findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA);
        if (activeAssignments.isEmpty()) {
            return null;
        }
        return activeAssignments.stream()
                .max((a1, a2) -> Long.compare(a1.getId() != null ? a1.getId() : 0L, a2.getId() != null ? a2.getId() : 0L))
                .orElse(activeAssignments.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> obtenerEstudiantesPorProfesor(Long professorId, String callerEmail) {
        User targetProfessor = userRepository.findById(professorId).orElse(null);
        if (targetProfessor == null || targetProfessor.getRole() == null) {
            throw new NoSuchElementException("Usuario no encontrado");
        }

        User caller = userRepository.findByEmail(callerEmail);
        if (caller == null) {
            throw new SecurityException("No autorizado");
        }

        boolean isAdmin = caller.getRole() != null && "Administrador".equalsIgnoreCase(caller.getRole().getName());
        boolean isSelf = caller.getId() != null && caller.getId().equals(professorId);

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("Acceso denegado: no tiene permisos para consultar los estudiantes de este docente");
        }

        Optional<TeachingRole> teachingRole = TeachingRole.fromRoleName(targetProfessor.getRole().getName());
        if (teachingRole.isEmpty()) {
            throw new com.ubb.practicas.exception.ForbiddenException("El usuario no tiene un rol docente válido");
        }

        return teachingRole.get().findAssignments(assignmentRepository, professorId);
    }
}
