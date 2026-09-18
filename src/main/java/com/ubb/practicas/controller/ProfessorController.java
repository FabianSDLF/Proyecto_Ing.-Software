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
        if (user == null || user.getRole() == null || !"Profesor".equals(user.getRole().getName())) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("El usuario no existe o no tiene el rol de Profesor");
        }
        return org.springframework.http.ResponseEntity.ok(practiceAssignmentRepository.findBySubjectProfessorId(professorId));
    }
}
