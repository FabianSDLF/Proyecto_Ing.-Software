package com.ubb.practicas.controller;

import com.ubb.practicas.exception.UnauthorizedException;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/profesor")
@RequiredArgsConstructor
public class ProfessorController {

    private final PracticeAssignmentService assignmentService;

    @GetMapping("/{professorId}/estudiantes")
    public ResponseEntity<List<PracticeAssignment>> obtenerEstudiantesPorProfesor(@PathVariable Long professorId, Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }

        List<PracticeAssignment> assignments = assignmentService.obtenerEstudiantesPorProfesor(professorId, principal.getName());
        return ResponseEntity.ok(assignments);
    }
}
