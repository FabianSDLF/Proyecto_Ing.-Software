package com.ubb.practicas.controller;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.exception.UnauthorizedException;
import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.enums.DocumentType;
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

    // ==========================================
    // HU30: Endpoints de Versionado e Historial
    // ==========================================

    @PostMapping(value = {"/planificaciones/{id}/editar", "/planificaciones/{id}"})
    public ResponseEntity<LessonPlan> editarPlanificacion(
            @PathVariable Long id,
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "schoolGrade", required = false) String schoolGrade,
            @RequestParam(value = "mathAxis", required = false) String mathAxis,
            @RequestParam(value = "objective", required = false) String objective,
            @RequestParam(value = "baseLearningObjective", required = false) String baseLearningObjective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "objectives", required = false) String objectives,
            @RequestParam(value = "classActivities", required = false) String classActivities,
            @RequestParam(value = "contentJson", required = false) String contentJson) {

        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        String finalObjective = objective != null ? objective : baseLearningObjective;
        String finalSpecificObjectives = specificObjectives != null ? specificObjectives : objectives;
        String finalClassActivities = classActivities != null ? classActivities : contentJson;

        LessonPlan plan = lessonPlanService.editarPlanificacion(
                id,
                principal.getName(),
                schoolGrade,
                mathAxis,
                finalObjective,
                finalSpecificObjectives,
                finalClassActivities,
                file
        );
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/planificaciones/{id}")
    public ResponseEntity<LessonPlan> obtenerPlanificacionPorId(
            @PathVariable Long id,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");
        LessonPlan plan = lessonPlanService.obtenerPlanificacionPorId(id, principal.getName());
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/planificaciones/{id}/historial")
    public ResponseEntity<List<LessonPlan>> obtenerHistorialPlanificacion(
            @PathVariable Long id,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");
        List<LessonPlan> historial = lessonPlanService.obtenerHistorialPlanificacion(id, principal.getName());
        return ResponseEntity.ok(historial);
    }

    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<Map<String, String>> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        portfolioService.eliminarDocumento(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Documento eliminado exitosamente"));
    }

    // ==========================================
    // HU19: Endpoints de Documentos Finales
    // ==========================================

    @PostMapping("/documentos-finales/upload")
    public ResponseEntity<PortfolioDocument> subirDocumentoFinal(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "replace", required = false) Boolean replace,
            @RequestParam(value = "replaceConfirmed", required = false) Boolean replaceConfirmed) {

        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        boolean confirmed = Boolean.TRUE.equals(replace) || Boolean.TRUE.equals(replaceConfirmed);
        PortfolioDocument doc = portfolioService.subirDocumentoFinal(principal.getName(), assignmentId, file, type, confirmed);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/documentos-finales")
    public ResponseEntity<List<PortfolioDocument>> obtenerMisDocumentosFinales(
            Principal principal,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        List<PortfolioDocument> documentos;
        if (assignmentId != null) {
            documentos = portfolioService.obtenerDocumentosFinalesPorAsignacion(assignmentId);
        } else {
            documentos = portfolioService.obtenerMisDocumentosFinales(principal.getName());
        }
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/documentos-finales/asignacion/{assignmentId}")
    public ResponseEntity<List<PortfolioDocument>> obtenerDocumentosFinalesPorAsignacion(
            @PathVariable Long assignmentId,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        List<PortfolioDocument> documentos = portfolioService.obtenerDocumentosFinalesPorAsignacion(assignmentId);
        return ResponseEntity.ok(documentos);
    }

    @DeleteMapping("/documentos-finales/{id}")
    public ResponseEntity<Map<String, String>> eliminarDocumentoFinal(
            @PathVariable Long id,
            Principal principal) {
        if (principal == null) throw new UnauthorizedException("Sesión no autorizada");

        portfolioService.eliminarDocumentoFinal(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Documento final eliminado exitosamente"));
    }
}
