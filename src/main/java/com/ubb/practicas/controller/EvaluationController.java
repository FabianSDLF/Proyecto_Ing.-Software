package com.ubb.practicas.controller;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/evaluaciones")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public ResponseEntity<Evaluation> crearEvaluacion(@RequestBody EvaluationCreateDTO dto, Principal principal) {
        return ResponseEntity.ok(evaluationService.crearEvaluacion(dto, principal.getName()));
    }

    @GetMapping("/asignacion/{id}")
    public ResponseEntity<List<Evaluation>> listarPorAsignacion(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.listarEvaluacionesPorAsignacion(id));
    }
}
