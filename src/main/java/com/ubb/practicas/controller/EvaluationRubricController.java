package com.ubb.practicas.controller;

import com.ubb.practicas.service.AssignmentScope;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.io.IOException;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EvaluationRubricController {
    private final AssignmentScope scope;

    @GetMapping(value = "/api/evaluaciones/pautas/colaborador", produces = "application/json")
    public ResponseEntity<String> rubrics(Principal principal) throws IOException {
        var user = scope.user(principal.getName());
        if (!AssignmentScope.isAdmin(user) && !Set.of("PROFESOR_COLABORADOR", "PROFESOR_DE_ASIGNATURA").contains(AssignmentScope.role(user)))
            throw new AccessDeniedException("No tiene acceso a las pautas del colaborador");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(
                new ClassPathResource("rubrics/collaborator.json").getContentAsString(StandardCharsets.UTF_8));
    }
}
