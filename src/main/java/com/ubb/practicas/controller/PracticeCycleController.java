package com.ubb.practicas.controller;

import com.ubb.practicas.dto.PracticeCycleDTO;
import com.ubb.practicas.dto.PracticeCycleUpdateRequest;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.service.PracticeCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HU5: Consulta y edición de los tres ciclos de práctica (Inicial, Intermedio, Avanzado).
 * Los ciclos son fijos: no se crean ni se eliminan desde la API.
 */
@RestController
@RequestMapping("/api/ciclos")
@RequiredArgsConstructor
public class PracticeCycleController {

    private final PracticeCycleService practiceCycleService;

    @GetMapping
    public ResponseEntity<List<PracticeCycleDTO>> listarCiclos() {
        return ResponseEntity.ok(practiceCycleService.listarCiclos());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<PracticeCycleDTO> obtenerCiclo(@PathVariable String codigo) {
        return ResponseEntity.ok(practiceCycleService.obtenerCiclo(PracticeCycle.fromString(codigo)));
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<PracticeCycleDTO> actualizarCiclo(@PathVariable String codigo,
                                                            @Valid @RequestBody PracticeCycleUpdateRequest request) {
        return ResponseEntity.ok(practiceCycleService.actualizarCiclo(PracticeCycle.fromString(codigo), request));
    }
}
