package com.ubb.practicas.controller;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asignaturas")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<Subject>> listarAsignaturas() {
        return ResponseEntity.ok(subjectService.listarAsignaturas());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador')")
    public ResponseEntity<Subject> crearAsignatura(@RequestBody Subject subject) {
        Subject created = subjectService.crearAsignatura(subject);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador')")
    public ResponseEntity<Map<String, String>> eliminarAsignatura(@PathVariable Long id) {
        subjectService.eliminarAsignatura(id);
        return ResponseEntity.ok(Map.of("message", "Asignatura eliminada exitosamente"));
    }
}
