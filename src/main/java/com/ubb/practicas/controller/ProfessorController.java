package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/profesor")
public class ProfessorController {

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private com.ubb.practicas.repository.UserRepository userRepository;

    @GetMapping("/{professorId}/estudiantes")
    public org.springframework.http.ResponseEntity<?> obtenerEstudiantesPorProfesor(@PathVariable Long professorId) {
        com.ubb.practicas.model.User user = userRepository.findById(professorId).orElse(null);
        
        if (user == null || user.getRole() == null) {
            return org.springframework.http.ResponseEntity.badRequest().body("Usuario no encontrado");
        }
        
        String roleName = user.getRole().getName();
        List<PracticeAssignment> assignments;
        
        if ("Profesor de Asignatura".equals(roleName)) {
            assignments = practiceAssignmentRepository.findBySubjectProfessorId(professorId);
        } else if ("Profesor Colaborador".equals(roleName)) {
            assignments = practiceAssignmentRepository.findByCollaboratorId(professorId);
        } else if ("Tutor".equals(roleName)) {
            assignments = practiceAssignmentRepository.findByTutorId(professorId);
        } else {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("El usuario no tiene un rol docente vǭlido");
        }
        
        return org.springframework.http.ResponseEntity.ok(assignments);
    }
}
