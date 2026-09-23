package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.TeachingRole;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profesor")
public class ProfessorController {

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{professorId}/estudiantes")
    public ResponseEntity<?> obtenerEstudiantesPorProfesor(@PathVariable Long professorId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User targetProfessor = userRepository.findById(professorId).orElse(null);
        if (targetProfessor == null || targetProfessor.getRole() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        User caller = userRepository.findByEmail(principal.getName());
        if (caller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = caller.getRole() != null && "Administrador".equalsIgnoreCase(caller.getRole().getName());
        boolean isSelf = caller.getId() != null && caller.getId().equals(professorId);

        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado: no tiene permisos para consultar los estudiantes de este docente");
        }

        Optional<TeachingRole> teachingRole = TeachingRole.fromRoleName(targetProfessor.getRole().getName());
        if (teachingRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El usuario no tiene un rol docente válido");
        }

        List<PracticeAssignment> assignments = teachingRole.get().findAssignments(practiceAssignmentRepository, professorId);
        return ResponseEntity.ok(assignments);
    }
}
