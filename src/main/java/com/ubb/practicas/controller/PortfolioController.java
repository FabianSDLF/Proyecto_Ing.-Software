package com.ubb.practicas.controller;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.exception.UnauthorizedException;
import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.service.LessonPlanService;
import com.ubb.practicas.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portafolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final LessonPlanService lessonPlanService;

    @GetMapping("/evidencias")
    public ResponseEntity<List<PortfolioDocument>> obtenerMisEvidencias(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");
        List<PortfolioDocument> documentos = portfolioService.obtenerMisEvidencias(principal.getName());
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/planificaciones")
    public ResponseEntity<List<LessonPlan>> obtenerMisPlanificaciones(Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");
        List<LessonPlan> planificaciones = lessonPlanService.obtenerMisPlanificaciones(principal.getName());
        return ResponseEntity.ok(planificaciones);
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        DownloadedDocument downloaded = portfolioService.descargarDocumento(id, principal.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloaded.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloaded.fileName() + "\"")
                .body(downloaded.resource());
    }

    @PostMapping("/evidencias/upload")
    public ResponseEntity<PortfolioDocument> subirEvidencia(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        PortfolioDocument doc = portfolioService.subirEvidencia(principal.getName(), assignmentId, file);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/planificaciones/upload")
    public ResponseEntity<LessonPlan> crearPlanificacion(
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "classActivities", required = false) String classActivities) {

        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

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
    }

    @DeleteMapping("/planificaciones/{id}")
    public ResponseEntity<Map<String, String>> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        lessonPlanService.eliminarPlanificacion(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Planificación eliminada exitosamente"));
    }

    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<Map<String, String>> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        portfolioService.eliminarDocumento(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Documento eliminado exitosamente"));
    }
}
