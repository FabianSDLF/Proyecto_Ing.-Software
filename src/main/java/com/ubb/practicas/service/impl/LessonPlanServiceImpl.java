package com.ubb.practicas.service.impl;

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
import com.ubb.practicas.service.EmailService;
import com.ubb.practicas.service.LessonPlanService;
import com.ubb.practicas.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonPlanServiceImpl implements LessonPlanService {

    private final LessonPlanRepository lessonPlanRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final PortfolioDocumentRepository portfolioDocumentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final EmailService emailService;

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
        return user != null && user.getRole() != null && "Coordinador de practicas".equalsIgnoreCase(user.getRole().getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> obtenerMisPlanificaciones(String studentEmail) {
        return lessonPlanRepository.findByPracticeAssignment_Student_Email(studentEmail);
    }

    @Override
    @Transactional
    public LessonPlan crearPlanificacion(
            String studentEmail,
            Long assignmentId,
            String schoolGrade,
            String mathAxis,
            String objective,
            String specificObjectives,
            String classActivities,
            MultipartFile file) {

        PracticeAssignment assignment = resolveAssignment(studentEmail, assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("No tienes una asignación de práctica activa.");
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
                String storedFileName = storageService.store(file);
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
        String nombreEntrega = (file != null && !file.isEmpty() && !"planificacion_sin_archivo.txt".equals(file.getOriginalFilename()))
                ? file.getOriginalFilename()
                : "Formulario de Planificación Web";
        emailService.enviarComprobante(studentEmail, nombreEntrega);
        return lessonPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public void eliminarPlanificacion(Long id, String userEmail) {
        LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planificación no encontrada"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !plan.isOwnedBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para eliminar esta planificación");
        }

        if (plan.getDocument() != null) {
            storageService.deleteFile(plan.getDocument().getStoredFileName());
        }
        lessonPlanRepository.delete(plan);
    }
}
