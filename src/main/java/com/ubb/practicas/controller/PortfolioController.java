package com.ubb.practicas.controller;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.service.LessonPlanService;
import com.ubb.practicas.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/portafolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final LessonPlanService lessonPlanService;

    @GetMapping("/evidencias")
    public ResponseEntity<?> obtenerMisEvidencias(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<PortfolioDocument> documentos = portfolioService.obtenerMisEvidencias(principal.getName());
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/planificaciones")
    public ResponseEntity<?> obtenerMisPlanificaciones(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<LessonPlan> planificaciones = lessonPlanService.obtenerMisPlanificaciones(principal.getName());
        return ResponseEntity.ok(planificaciones);
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            DownloadedDocument downloaded = portfolioService.descargarDocumento(id, principal.getName());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(downloaded.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloaded.fileName() + "\"")
                    .body(downloaded.resource());
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/evidencias/upload")
    public ResponseEntity<?> subirEvidencia(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            PortfolioDocument doc = portfolioService.subirEvidencia(principal.getName(), assignmentId, file);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al subir archivo: " + e.getMessage());
        }
    }

    @PostMapping("/planificaciones/upload")
    public ResponseEntity<?> crearPlanificacion(
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "classActivities", required = false) String classActivities) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            LessonPlan plan = lessonPlanService.crearPlanificacion(
                    principal.getName(),
                    assignmentId,
                    schoolGrade,
                    mathAxis,
                    objective,
                    specificObjectives,
                    classActivities,
                    file
            );
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/planificaciones/{id}")
    public ResponseEntity<?> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            lessonPlanService.eliminarPlanificacion(id, principal.getName());
            return ResponseEntity.ok("Planificación eliminada");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            portfolioService.eliminarDocumento(id, principal.getName());
            return ResponseEntity.ok("Documento eliminado");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar documento: " + e.getMessage());
        }
    }
}
