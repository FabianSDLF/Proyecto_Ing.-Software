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
    private final com.ubb.practicas.repository.PortfolioDocumentRepository portfolioDocumentRepository;
    private final com.ubb.practicas.repository.PracticeAssignmentRepository practiceAssignmentRepository;

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
    @GetMapping(value = "/download-zip", produces = "application/zip")
    public void downloadZip(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"informes_finales.zip\"");

        java.util.List<PortfolioDocument> documentos = portfolioDocumentRepository.findAll().stream()
                .filter(doc -> doc.getType().name().equals("INFORME_FINAL"))
                .collect(java.util.stream.Collectors.toList());

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
            for (PortfolioDocument doc : documentos) {
                java.io.File file = new java.io.File(doc.getFileUrl());
                if (file.exists() && !file.isDirectory()) {
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry("Informes/" + file.getName());
                    zos.putNextEntry(zipEntry);
                    java.nio.file.Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                } else {
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry("Informes/FALTA_" + doc.getFileName() + ".txt");
                    zos.putNextEntry(zipEntry);
                    zos.write(("El archivo fisico no se encontro en el servidor.").getBytes());
                    zos.closeEntry();
                }
            }
        }
    }

    @GetMapping(value = "/export-csv", produces = "text/csv; charset=utf-8")
    public void exportToCSV(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte_institucional.csv\"");

        java.util.List<com.ubb.practicas.model.PracticeAssignment> asignaciones = practiceAssignmentRepository.findAll();
        java.io.PrintWriter writer = response.getWriter();

        writer.println("Estudiante;Asignatura;Periodo;Centro de Practica;Profesor Colaborador;Tutor;Estado");

        for (com.ubb.practicas.model.PracticeAssignment asig : asignaciones) {
            String estudiante = (asig.getStudent() != null) ? asig.getStudent().getFullName() : "N/A";
            String asignatura = (asig.getSubject() != null) ? asig.getSubject().getName() : "N/A";
            String periodo = (asig.getPeriod() != null) ? asig.getPeriod().getName() : "N/A";
            String centro = (asig.getCenter() != null) ? asig.getCenter().getName() : "N/A";
            String colaborador = (asig.getCollaborator() != null) ? asig.getCollaborator().getFullName() : "N/A";
            String tutor = (asig.getTutor() != null) ? asig.getTutor().getFullName() : "N/A";
            String estado = (asig.getStatus() != null) ? asig.getStatus().name() : "N/A";

            writer.println(estudiante + ";" + asignatura + ";" + periodo + ";" + centro + ";" + colaborador + ";" + tutor + ";" + estado);
        }
    }
}
