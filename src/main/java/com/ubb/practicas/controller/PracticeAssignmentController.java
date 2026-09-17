package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/asignaciones")
public class PracticeAssignmentController {

    @Autowired
    private PracticeAssignmentRepository assignmentRepository;

    @GetMapping
    public List<PracticeAssignment> listarAsignaciones() {
        return assignmentRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crearAsignacion(@RequestBody PracticeAssignment assignment) {
        if (assignment.getStudent() == null || assignment.getStudent().getId() == null) {
            return ResponseEntity.badRequest().body("El estudiante es obligatorio");
        }
        if (assignment.getSubject() == null || assignment.getSubject().getId() == null) {
            return ResponseEntity.badRequest().body("La asignatura es obligatoria");
        }
        
        assignment.setCreatedAt(LocalDateTime.now());
        if (assignment.getStatus() == null) {
            assignment.setStatus(com.ubb.practicas.model.enums.AssignmentStatus.ACTIVA);
        }

        return ResponseEntity.ok(assignmentRepository.save(assignment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAsignacion(@PathVariable Long id) {
        if (!assignmentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        assignmentRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
