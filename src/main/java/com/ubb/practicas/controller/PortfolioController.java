package com.ubb.practicas.controller;

import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.repository.LessonPlanRepository;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/portafolio")
public class PortfolioController {

    @Autowired
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private LessonPlanRepository lessonPlanRepository;

    @GetMapping("/evidencias")
    public ResponseEntity<?> obtenerMisEvidencias(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        // Filtramos para devolver solo EVIDENCIA_APRENDIZAJE
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

            PortfolioDocument doc = new PortfolioDocument();
            doc.setPracticeAssignment(assignments.get(0));
            doc.setType(com.ubb.practicas.model.enums.DocumentType.EVIDENCIA_APRENDIZAJE);
            doc.setFileName(file.getOriginalFilename());
            doc.setFileUrl("/uploads/" + file.getOriginalFilename());
            doc.setSizeBytes(file.getSize());
            doc.setUploadDate(LocalDateTime.now());

            portfolioDocumentRepository.save(doc);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al subir archivo: " + e.getMessage());
        }
    }

    @PostMapping("/planificaciones/upload")
    public ResponseEntity<?> crearPlanificacion(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective) {
        
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            List<PracticeAssignment> assignments = practiceAssignmentRepository.findByStudentEmail(principal.getName());
            if (assignments.isEmpty()) return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");

            // 1. Crear el documento
            PortfolioDocument doc = new PortfolioDocument();
            doc.setPracticeAssignment(assignments.get(0));
            doc.setType(com.ubb.practicas.model.enums.DocumentType.PLANIFICACION);
            doc.setFileName(file.getOriginalFilename());
            doc.setFileUrl("/uploads/" + file.getOriginalFilename());
            doc.setSizeBytes(file.getSize());
            doc.setUploadDate(LocalDateTime.now());

            // 2. Crear la planificación
            LessonPlan plan = new LessonPlan();
            plan.setPracticeAssignment(assignments.get(0));
            plan.setDocument(doc);
            plan.setStatus(com.ubb.practicas.model.enums.PlanStatus.BORRADOR);
            plan.setSchoolGrade(schoolGrade);
            plan.setMathAxis(com.ubb.practicas.model.enums.MathAxis.valueOf(mathAxis));
            plan.setBaseLearningObjective(objective);
            plan.setCreatedAt(LocalDateTime.now());
            plan.setUpdatedAt(LocalDateTime.now());

            lessonPlanRepository.save(plan);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear planificación: " + e.getMessage());
        }
    }
}
