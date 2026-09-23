package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class PracticeAssignmentController {

    private final PracticeAssignmentService assignmentService;

    @GetMapping
    public List<PracticeAssignment> listarAsignaciones() {
        return assignmentService.listarAsignaciones();
    }

    @GetMapping("/colaborador/{id}")
    public ResponseEntity<List<PracticeAssignment>> listarPorColaborador(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.listarPorColaborador(id));
    }

    @GetMapping("/tutor/{id}")
    public ResponseEntity<List<PracticeAssignment>> listarPorTutor(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.listarPorTutor(id));
    }

    @PostMapping
    public ResponseEntity<?> crearAsignacion(@RequestBody PracticeAssignment assignment) {
        try {
            PracticeAssignment created = assignmentService.crearAsignacion(assignment);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAsignacion(@PathVariable Long id) {
        try {
            assignmentService.eliminarAsignacion(id);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
