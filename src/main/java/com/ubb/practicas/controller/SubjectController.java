package com.ubb.practicas.controller;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/asignaturas")
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/reset-ids")
    public ResponseEntity<?> resetAutoIncrement() {
        entityManager.createNativeQuery("ALTER TABLE asignaturas AUTO_INCREMENT = 1").executeUpdate();
        return ResponseEntity.ok("Contador reseteado");
    }

    @Autowired
    private com.ubb.practicas.repository.UserRepository userRepository;

    @GetMapping
    public List<Subject> listarAsignaturas() {
        return subjectRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crearAsignatura(@RequestBody Subject subject) {
        // Enforce that creation always generates a new entity and never overwrites an existing one
        subject.setId(null);

        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre de la asignatura es obligatorio");
        }
        if (subject.getName().trim().length() > 255) {
            return ResponseEntity.badRequest().body("El nombre de la asignatura no puede superar los 255 caracteres");
        }
        if (subject.getCycle() == null) {
            return ResponseEntity.badRequest().body("El ciclo de la asignatura es obligatorio");
        }
        subject.setName(subject.getName().trim());

        if (subject.getCode() != null) {
            String trimmedCode = subject.getCode().trim();
            if (trimmedCode.length() > 50) {
                return ResponseEntity.badRequest().body("El código de la asignatura no puede superar los 50 caracteres");
            }
            subject.setCode(trimmedCode.isEmpty() ? null : trimmedCode);
        }

        if (subject.getProfessor() != null) {
            if (subject.getProfessor().getId() == null) {
                subject.setProfessor(null);
            } else if (!userRepository.existsById(subject.getProfessor().getId())) {
                return ResponseEntity.badRequest().body("El profesor especificado no existe");
            }
        }

        try {
            return ResponseEntity.ok(subjectRepository.save(subject));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Datos de asignatura inválidos o referencia no encontrada");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAsignatura(@PathVariable Long id) {
        if (!subjectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (practiceAssignmentRepository != null && practiceAssignmentRepository.existsBySubjectId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica.");
        }
        try {
            subjectRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar la asignatura porque tiene dependencias en el sistema.");
        }
    }
}

