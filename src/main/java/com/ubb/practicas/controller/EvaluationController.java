package com.ubb.practicas.controller;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationFinalizeDTO;
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

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Evaluation> finalizarEvaluacion(
            @PathVariable Long id,
            @RequestBody EvaluationFinalizeDTO dto,
            Principal principal
    ) {
        return ResponseEntity.ok(evaluationService.finalizarEvaluacion(id, dto, principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evaluation> obtenerPorId(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(evaluationService.obtenerEvaluacionPorId(id, principal.getName()));
    }

    @GetMapping("/asignacion/{id}")
    public ResponseEntity<List<Evaluation>> listarPorAsignacion(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(evaluationService.listarEvaluacionesPorAsignacion(id, principal.getName()));
    }
}
