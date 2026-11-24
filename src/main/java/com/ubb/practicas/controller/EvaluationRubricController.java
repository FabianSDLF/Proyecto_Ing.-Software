package com.ubb.practicas.controller;

import com.ubb.practicas.service.AssignmentScope;
import com.ubb.practicas.service.EvaluationRubricCatalog;
import com.ubb.practicas.service.EvaluationRubricCatalog.RubricDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EvaluationRubricController {

    private final AssignmentScope scope;
    private final EvaluationRubricCatalog rubricCatalog;

    @GetMapping("/api/evaluaciones/pautas")
    public ResponseEntity<List<RubricDefinition>> listarPautasAplicables(
            @RequestParam(required = false) Long assignmentId,
            Principal principal
    ) {
        return ResponseEntity.ok(rubricCatalog.getApplicableRubrics(principal.getName(), assignmentId));
    }

    @GetMapping("/api/evaluaciones/pautas/colaborador")
    public ResponseEntity<List<RubricDefinition>> rubricsColaborador(Principal principal) {
        var user = scope.user(principal.getName());
        if (!AssignmentScope.isAdmin(user) && !Set.of("PROFESOR_COLABORADOR", "PROFESOR_DE_ASIGNATURA").contains(AssignmentScope.role(user))) {
            throw new AccessDeniedException("No tiene acceso a las pautas del colaborador");
        }
        return ResponseEntity.ok(rubricCatalog.getCollaboratorRubrics());
    }

    @GetMapping("/api/evaluaciones/pautas/tutor")
    public ResponseEntity<List<RubricDefinition>> rubricsTutor(Principal principal) {
        var user = scope.user(principal.getName());
        if (!AssignmentScope.isAdmin(user) && !Set.of("TUTOR", "PROFESOR_DE_ASIGNATURA").contains(AssignmentScope.role(user))) {
            throw new AccessDeniedException("No tiene acceso a las pautas del tutor");
        }
        return ResponseEntity.ok(rubricCatalog.getTutorRubrics());
    }
}
