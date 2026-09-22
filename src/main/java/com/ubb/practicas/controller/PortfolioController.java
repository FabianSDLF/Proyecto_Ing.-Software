package com.ubb.practicas.controller;

import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.repository.LessonPlanRepository;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/portafolio")
public class PortfolioController {

    @DeleteMapping("/planificaciones/{id}")
    public ResponseEntity<?> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        Optional<LessonPlan> planOpt = lessonPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LessonPlan plan = planOpt.get();
        if (!plan.getPracticeAssignment().getStudent().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).build();
        }
        
        try {
            if (plan.getDocument() != null) {
                fileStorageService.deleteFile(plan.getDocument().getStoredFileName());
                // The cascade or manual delete of document will happen
            }
            lessonPlanRepository.delete(plan);
            return ResponseEntity.ok("Planificación eliminada");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar planificación: " + e.getMessage());
        }
    }

    @Autowired
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private LessonPlanRepository lessonPlanRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/evidencias")
    public ResponseEntity<?> obtenerMisEvidencias(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        List<PortfolioDocument> documentos = portfolioDocumentRepository.findByPracticeAssignment_Student_Email(principal.getName())
            .stream().filter(d -> d.getType() == com.ubb.practicas.model.enums.DocumentType.EVIDENCIA_APRENDIZAJE).toList();
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/planificaciones")
    public ResponseEntity<?> obtenerMisPlanificaciones(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        List<LessonPlan> planificaciones = lessonPlanRepository.findByPracticeAssignment_Student_Email(principal.getName());
        return ResponseEntity.ok(planificaciones);
    }

    @PostMapping("/evidencias/upload")
    public ResponseEntity<?> subirEvidencia(Principal principal, @RequestParam("file") MultipartFile file) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            List<PracticeAssignment> assignments = practiceAssignmentRepository.findByStudentEmail(principal.getName());
            if (assignments.isEmpty()) return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");

            String storedFileName = fileStorageService.storeFile(file);

            PortfolioDocument doc = new PortfolioDocument();
            doc.setPracticeAssignment(assignments.get(0));
            doc.setType(com.ubb.practicas.model.enums.DocumentType.EVIDENCIA_APRENDIZAJE);
            doc.setFileName(file.getOriginalFilename());
            doc.setStoredFileName(storedFileName);
            doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar"); // ID is null initially, will update after save
            doc.setContentType(file.getContentType());
            doc.setSizeBytes(file.getSize());
            doc.setUploadDate(LocalDateTime.now());

            portfolioDocumentRepository.save(doc);
            
            // Update URL with actual ID
            doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
            portfolioDocumentRepository.save(doc);

            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al subir archivo: " + e.getMessage());
        }
    }

    @PostMapping("/planificaciones/upload")
    public ResponseEntity<?> crearPlanificacion(
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "classActivities", required = false) String classActivities) {
        
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            List<PracticeAssignment> assignments = practiceAssignmentRepository.findByStudentEmail(principal.getName());
            if (assignments.isEmpty()) return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");

            LessonPlan plan = new LessonPlan();
            plan.setPracticeAssignment(assignments.get(0));
            plan.setStatus(com.ubb.practicas.model.enums.PlanStatus.BORRADOR);
            plan.setSchoolGrade(schoolGrade);
            plan.setMathAxis(com.ubb.practicas.model.enums.MathAxis.valueOf(mathAxis));
            plan.setBaseLearningObjective(objective);
            plan.setObjectives(specificObjectives);
            plan.setContentJson(classActivities);
            plan.setCreatedAt(LocalDateTime.now());
            plan.setUpdatedAt(LocalDateTime.now());

            if (file != null && !file.isEmpty() && file.getSize() > 0) {
                if (!"planificacion_sin_archivo.txt".equals(file.getOriginalFilename())) {
                    String storedFileName = fileStorageService.storeFile(file);
                    PortfolioDocument doc = new PortfolioDocument();
                    doc.setPracticeAssignment(assignments.get(0));
                    doc.setType(com.ubb.practicas.model.enums.DocumentType.PLANIFICACION);
                    doc.setFileName(file.getOriginalFilename());
                    doc.setStoredFileName(storedFileName);
                    doc.setContentType(file.getContentType());
                    doc.setSizeBytes(file.getSize());
                    doc.setUploadDate(LocalDateTime.now());

                    portfolioDocumentRepository.save(doc);
                    doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
                    portfolioDocumentRepository.save(doc);
                    
                    plan.setDocument(doc);
                }
            }

            lessonPlanRepository.save(plan);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear planificación: " + e.getMessage());
        }
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortfolioDocument doc = docOpt.get();
        // Nota de seguridad: aquí se debería validar que el principal tenga permiso para ver este documento
        
        Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());
        
        String contentType = doc.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }
    
    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortfolioDocument doc = docOpt.get();
        
        try {
            fileStorageService.deleteFile(doc.getStoredFileName());
            
            // Si es planificación, eliminarla
            if (doc.getType() == com.ubb.practicas.model.enums.DocumentType.PLANIFICACION) {
                // Find and delete the related LessonPlan first due to foreign key
                List<LessonPlan> plans = lessonPlanRepository.findByPracticeAssignment_Student_Email(principal.getName());
                for (LessonPlan plan : plans) {
                    if (plan.getDocument().getId().equals(doc.getId())) {
                        lessonPlanRepository.delete(plan);
                        break;
                    }
                }
            }
            
            portfolioDocumentRepository.delete(doc);
            return ResponseEntity.ok("Documento eliminado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar documento: " + e.getMessage());
        }
    }
}
