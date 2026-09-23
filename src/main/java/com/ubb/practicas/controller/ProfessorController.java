package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/profesor")
@RequiredArgsConstructor
public class ProfessorController {

    private final PracticeAssignmentService assignmentService;

    @GetMapping("/{professorId}/estudiantes")
    public ResponseEntity<?> obtenerEstudiantesPorProfesor(@PathVariable Long professorId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<PracticeAssignment> assignments = assignmentService.obtenerEstudiantesPorProfesor(professorId, principal.getName());
            return ResponseEntity.ok(assignments);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
