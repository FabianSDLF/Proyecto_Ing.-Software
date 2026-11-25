package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.service.AssignmentScope;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AssignmentQueryController {
    private final AssignmentScope scope;

    @GetMapping("/visibles")
    public List<PracticeAssignment> visible(Principal principal,
            @RequestParam(required = false) Long periodId,
            @RequestParam(defaultValue = "false") boolean history) {
        return scope.visible(principal.getName(), periodId, history);
    }

    @GetMapping("/{id}/ficha")
    public PracticeAssignment ficha(@PathVariable Long id, Principal principal) {
        return scope.require(id, principal.getName());
    }
}
