package com.ubb.practicas.controller;

import com.ubb.practicas.model.AcademicPeriod;
import com.ubb.practicas.model.enums.PeriodStatus;
import com.ubb.practicas.repository.AcademicPeriodRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/periodos")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodRepository periodRepository;
    private final PracticeAssignmentRepository assignmentRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AcademicPeriod>> listarPeriodos() {
        List<AcademicPeriod> periodos = periodRepository.findAll();
        periodos.sort(
                Comparator.comparing((AcademicPeriod p) -> p.getYear() != null ? p.getYear() : 0).reversed()
                        .thenComparing(Comparator.comparing((AcademicPeriod p) -> p.getSemesterNumber() != null ? p.getSemesterNumber() : 0).reversed())
        );
        return ResponseEntity.ok(periodos);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<AcademicPeriod> crearPeriodo(@RequestBody AcademicPeriod period) {
        if (period.getName() == null || period.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del periodo académico es obligatorio (ej. 2026-2)");
        }
        period.setId(null);
        period.setName(period.getName().trim());
        if (period.getStatus() == null) {
            period.setStatus(PeriodStatus.ACTIVO);
        }
        if (period.getStatus() == PeriodStatus.ACTIVO) {
            desactivarOtrosPeriodosActivos(null);
        }
        return ResponseEntity.ok(periodRepository.save(period));
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<AcademicPeriod> actualizarPeriodo(@PathVariable Long id, @RequestBody AcademicPeriod datos) {
        AcademicPeriod existing = periodRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Periodo académico no encontrado"));

        if (datos.getName() != null && !datos.getName().trim().isEmpty()) {
            existing.setName(datos.getName().trim());
        }
        if (datos.getYear() != null) {
            existing.setYear(datos.getYear());
        }
        if (datos.getSemesterNumber() != null) {
            existing.setSemesterNumber(datos.getSemesterNumber());
        }
        existing.setStartDate(datos.getStartDate());
        existing.setEndDate(datos.getEndDate());
        if (datos.getStatus() != null) {
            existing.setStatus(datos.getStatus());
            if (datos.getStatus() == PeriodStatus.ACTIVO) {
                desactivarOtrosPeriodosActivos(id);
            }
        }
        return ResponseEntity.ok(periodRepository.save(existing));
    }

    @PatchMapping("/{id}/estado")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<AcademicPeriod> cambiarEstadoPeriodo(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        AcademicPeriod existing = periodRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Periodo académico no encontrado"));

        String statusStr = payload.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        PeriodStatus newStatus = PeriodStatus.valueOf(statusStr.trim().toUpperCase());
        existing.setStatus(newStatus);
        if (newStatus == PeriodStatus.ACTIVO) {
            desactivarOtrosPeriodosActivos(id);
        }
        return ResponseEntity.ok(periodRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador', 'COORDINADOR_DE_PRACTICAS', 'Coordinador de practicas')")
    public ResponseEntity<Map<String, String>> eliminarPeriodo(@PathVariable Long id) {
        if (!periodRepository.existsById(id)) {
            throw new NoSuchElementException("Periodo académico no encontrado");
        }
        if (assignmentRepository != null && assignmentRepository.existsByPeriodId(id)) {
            throw new IllegalStateException("No se puede eliminar el periodo porque tiene asignaciones de práctica asociadas.");
        }
        try {
            periodRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar el periodo porque tiene registros asociados.", e);
        }
        return ResponseEntity.ok(Map.of("message", "Periodo académico eliminado exitosamente"));
    }

    private void desactivarOtrosPeriodosActivos(Long excludeId) {
        List<AcademicPeriod> activos = periodRepository.findByStatus(PeriodStatus.ACTIVO);
        for (AcademicPeriod p : activos) {
            if (excludeId == null || !excludeId.equals(p.getId())) {
                p.setStatus(PeriodStatus.CERRADO);
                periodRepository.save(p);
            }
        }
    }
}
