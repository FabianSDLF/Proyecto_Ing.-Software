package com.ubb.practicas.controller;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/portafolio")
public class PortfolioController {

    @Autowired
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private LessonPlanRepository lessonPlanRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    // --- Helper Methods ---

    private PracticeAssignment resolveAssignment(String email, Long explicitAssignmentId) {
        if (explicitAssignmentId != null) {
            Optional<PracticeAssignment> opt = practiceAssignmentRepository.findById(explicitAssignmentId);
            if (opt.isPresent()) {
                PracticeAssignment a = opt.get();
                if (a.isStudent(email) && a.getStatus() == AssignmentStatus.ACTIVA) {
                    return a;
                }
            }
            return null;
        }

        List<PracticeAssignment> activeAssignments = practiceAssignmentRepository.findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA);
        if (activeAssignments.isEmpty()) {
            return null;
        }
        return activeAssignments.stream()
                .max((a1, a2) -> Long.compare(a1.getId() != null ? a1.getId() : 0L, a2.getId() != null ? a2.getId() : 0L))
                .orElse(activeAssignments.get(0));
    }

    private boolean isUserAdmin(String email) {
        User user = userRepository.findByEmail(email);
        return user != null && user.getRole() != null && "Administrador".equalsIgnoreCase(user.getRole().getName());
    }

    // --- Endpoints ---

    @GetMapping("/evidencias")
    public ResponseEntity<?> obtenerMisEvidencias(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<PortfolioDocument> documentos = portfolioDocumentRepository.findByPracticeAssignment_Student_EmailAndType(
                principal.getName(), DocumentType.EVIDENCIA_APRENDIZAJE
        );
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/planificaciones")
    public ResponseEntity<?> obtenerMisPlanificaciones(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<LessonPlan> planificaciones = lessonPlanRepository.findByPracticeAssignment_Student_Email(principal.getName());
        return ResponseEntity.ok(planificaciones);
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortfolioDocument doc = docOpt.get();
        String userEmail = principal.getName();
        boolean isAdmin = isUserAdmin(userEmail);

        if (!isAdmin && !doc.isAccessibleBy(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());
            String contentType = doc.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .body(resource);
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
            PracticeAssignment assignment = resolveAssignment(principal.getName(), assignmentId);
            if (assignment == null) {
                return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");
            }

            String storedFileName = fileStorageService.storeFile(file);

            PortfolioDocument doc = new PortfolioDocument();
            doc.setPracticeAssignment(assignment);
            doc.setType(DocumentType.EVIDENCIA_APRENDIZAJE);
            doc.setFileName(file.getOriginalFilename());
            doc.setStoredFileName(storedFileName);
            doc.setContentType(file.getContentType());
            doc.setSizeBytes(file.getSize());
            doc.setUploadDate(LocalDateTime.now());

            portfolioDocumentRepository.save(doc);

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
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "classActivities", required = false) String classActivities) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            PracticeAssignment assignment = resolveAssignment(principal.getName(), assignmentId);
            if (assignment == null) {
                return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");
            }

            LessonPlan plan = new LessonPlan();
            plan.setPracticeAssignment(assignment);
            plan.setStatus(PlanStatus.BORRADOR);
            plan.setSchoolGrade(schoolGrade);
            plan.setMathAxis(MathAxis.valueOf(mathAxis));
            plan.setBaseLearningObjective(objective);
            plan.setObjectives(specificObjectives);
            plan.setContentJson(classActivities);
            plan.setCreatedAt(LocalDateTime.now());
            plan.setUpdatedAt(LocalDateTime.now());

            if (file != null && !file.isEmpty() && file.getSize() > 0) {
                if (!"planificacion_sin_archivo.txt".equals(file.getOriginalFilename())) {
                    String storedFileName = fileStorageService.storeFile(file);
                    PortfolioDocument doc = new PortfolioDocument();
                    doc.setPracticeAssignment(assignment);
                    doc.setType(DocumentType.PLANIFICACION);
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

    @DeleteMapping("/planificaciones/{id}")
    public ResponseEntity<?> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<LessonPlan> planOpt = lessonPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LessonPlan plan = planOpt.get();
        String userEmail = principal.getName();
        boolean isAdmin = isUserAdmin(userEmail);

        if (!isAdmin && !plan.isOwnedBy(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            if (plan.getDocument() != null) {
                fileStorageService.deleteFile(plan.getDocument().getStoredFileName());
            }
            lessonPlanRepository.delete(plan);
            return ResponseEntity.ok("Planificación eliminada");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortfolioDocument doc = docOpt.get();
        String userEmail = principal.getName();
        boolean isAdmin = isUserAdmin(userEmail);

        if (!isAdmin && !doc.isOwnedBy(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            fileStorageService.deleteFile(doc.getStoredFileName());

            // Si es planificación, eliminar la entidad LessonPlan vinculada de manera segura
            if (doc.getType() == DocumentType.PLANIFICACION) {
                lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete);
            }

            portfolioDocumentRepository.delete(doc);
            return ResponseEntity.ok("Documento eliminado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar documento: " + e.getMessage());
        }
    }
}
