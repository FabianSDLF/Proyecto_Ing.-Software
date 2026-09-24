package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class PracticeAssignmentController {

    private final PracticeAssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<List<PracticeAssignment>> listarAsignaciones() {
        return ResponseEntity.ok(assignmentService.listarAsignaciones());
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
    public ResponseEntity<PracticeAssignment> crearAsignacion(@RequestBody PracticeAssignment assignment) {
        PracticeAssignment created = assignmentService.crearAsignacion(assignment);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarAsignacion(@PathVariable Long id) {
        assignmentService.eliminarAsignacion(id);
        return ResponseEntity.ok(Map.of("message", "Asignación eliminada exitosamente"));
    }
}
