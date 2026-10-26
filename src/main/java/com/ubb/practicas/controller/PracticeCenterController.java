package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.service.PracticeCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/centros")
@RequiredArgsConstructor
public class PracticeCenterController {

    private final PracticeCenterService practiceCenterService;

    @GetMapping
    public ResponseEntity<List<PracticeCenter>> listarCentros() {
        return ResponseEntity.ok(practiceCenterService.listarCentros());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<PracticeCenter> crearCentro(@RequestBody PracticeCenter center) {
        return ResponseEntity.ok(practiceCenterService.crearCentro(center));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<PracticeCenter> actualizarCentro(@PathVariable Long id, @RequestBody PracticeCenter center) {
        return ResponseEntity.ok(practiceCenterService.actualizarCentro(id, center));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<Map<String, String>> eliminarCentro(@PathVariable Long id) {
        practiceCenterService.eliminarCentro(id);
        return ResponseEntity.ok(Map.of("message", "Centro de práctica eliminado exitosamente"));
    }
}
