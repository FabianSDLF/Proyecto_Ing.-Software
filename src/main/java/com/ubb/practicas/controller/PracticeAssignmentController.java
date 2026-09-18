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

    @Autowired
    private com.ubb.practicas.repository.UserRepository userRepository;
    
    @Autowired
    private com.ubb.practicas.repository.SubjectRepository subjectRepository;
    
    @Autowired
    private com.ubb.practicas.repository.PracticeCenterRepository centerRepository;

    @PostMapping
    public ResponseEntity<?> crearAsignacion(@RequestBody PracticeAssignment assignment) {
        if (assignment.getStudent() == null || assignment.getStudent().getId() == null) {
            return ResponseEntity.badRequest().body("El estudiante es obligatorio");
        }
        if (assignment.getSubject() == null || assignment.getSubject().getId() == null) {
            return ResponseEntity.badRequest().body("La asignatura es obligatoria");
        }
        
        if (!userRepository.existsById(assignment.getStudent().getId())) {
            return ResponseEntity.badRequest().body("El estudiante no existe en la base de datos");
        }
        if (!subjectRepository.existsById(assignment.getSubject().getId())) {
            return ResponseEntity.badRequest().body("La asignatura no existe en la base de datos");
        }
        if (assignment.getCenter() != null && assignment.getCenter().getId() != null && !centerRepository.existsById(assignment.getCenter().getId())) {
            return ResponseEntity.badRequest().body("El centro de práctica no existe");
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
