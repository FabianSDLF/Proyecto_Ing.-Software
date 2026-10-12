package com.ubb.practicas.controller;

import com.ubb.practicas.dto.StudentPairAssignmentSummaryDTO;
import com.ubb.practicas.dto.StudentPairCreateRequest;
import com.ubb.practicas.dto.StudentPairDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.service.StudentPairService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/duplas")
@RequiredArgsConstructor
public class StudentPairController {

    private final StudentPairService studentPairService;

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador', 'ADMINISTRADOR', 'Coordinador de practicas', 'COORDINADOR_DE_PRACTICAS')")
    public ResponseEntity<StudentPairDTO> crearDupla(@Valid @RequestBody StudentPairCreateRequest request) {
        StudentPairDTO created = studentPairService.crearDupla(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'ADMINISTRADOR', 'Coordinador de practicas', 'COORDINADOR_DE_PRACTICAS')")
    public ResponseEntity<List<StudentPairDTO>> listarDuplas(
            @RequestParam(value = "periodId", required = false) Long periodId) {
        List<StudentPairDTO> duplas = studentPairService.listarDuplasPorPeriodo(periodId);
        return ResponseEntity.ok(duplas);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('Administrador', 'ADMINISTRADOR', 'Coordinador de practicas', 'COORDINADOR_DE_PRACTICAS')")
    public ResponseEntity<List<StudentPairAssignmentSummaryDTO>> listarDisponibles(
            @RequestParam(value = "periodId", required = false) Long periodId) {
        List<StudentPairAssignmentSummaryDTO> disponibles = studentPairService.listarAsignacionesDisponibles(periodId);
        return ResponseEntity.ok(disponibles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'ADMINISTRADOR', 'Coordinador de practicas', 'COORDINADOR_DE_PRACTICAS')")
    public ResponseEntity<StudentPairDTO> obtenerDuplaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(studentPairService.obtenerDuplaPorId(id));
    }

    /**
     * Defensive rejection: Duplas are definitive and immutable. Deletion is explicitly forbidden.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'ADMINISTRADOR', 'Coordinador de practicas', 'COORDINADOR_DE_PRACTICAS')")
    public ResponseEntity<Void> eliminarDupla(@PathVariable Long id) {
        throw new BadRequestException("Las duplas de estudiantes son definitivas e inmutables durante el semestre.");
    }
}
