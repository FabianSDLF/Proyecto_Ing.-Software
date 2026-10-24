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
import java.util.*;

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
        if (user == null || user.getRole() == null) return false;
        String roleName = user.getRole().getName();
        return "Coordinador de practicas".equalsIgnoreCase(roleName) ||
               "Administrador".equalsIgnoreCase(roleName) ||
               "Admin".equalsIgnoreCase(roleName);
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
        plan.setVersion(1);
        plan.setParentPlan(null);
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

    @Override
    @Transactional
    public LessonPlan editarPlanificacion(
            Long id,
            String studentEmail,
            String schoolGrade,
            String mathAxis,
            String objective,
            String specificObjectives,
            String classActivities,
            MultipartFile file) {

        LessonPlan existing = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planificación no encontrada: " + id));

        boolean isAdmin = isUserAdmin(studentEmail);
        if (!isAdmin && !existing.isOwnedBy(studentEmail)) {
            throw new AccessDeniedException("No tiene permisos para editar esta planificación");
        }

        if (existing.getStatus() == PlanStatus.BORRADOR) {
            // Edición in-situ
            if (schoolGrade != null && !schoolGrade.isBlank()) {
                existing.setSchoolGrade(schoolGrade);
            }
            if (mathAxis != null && !mathAxis.isBlank()) {
                existing.setMathAxis(MathAxis.valueOf(mathAxis));
            }
            if (objective != null && !objective.isBlank()) {
                existing.setBaseLearningObjective(objective);
            }
            if (specificObjectives != null && !specificObjectives.isBlank()) {
                existing.setObjectives(specificObjectives);
            }
            if (classActivities != null && !classActivities.isBlank()) {
                existing.setContentJson(classActivities);
            }
            existing.setUpdatedAt(LocalDateTime.now());

            if (file != null && !file.isEmpty() && file.getSize() > 0) {
                if (!"planificacion_sin_archivo.txt".equals(file.getOriginalFilename())) {
                    String storedFileName = storageService.store(file);
                    PortfolioDocument doc = existing.getDocument();
                    if (doc == null) {
                        doc = new PortfolioDocument();
                        doc.setPracticeAssignment(existing.getPracticeAssignment());
                        doc.setType(DocumentType.PLANIFICACION);
                    }
                    doc.setFileName(file.getOriginalFilename());
                    doc.setStoredFileName(storedFileName);
                    doc.setContentType(file.getContentType());
                    doc.setSizeBytes(file.getSize());
                    doc.setUploadDate(LocalDateTime.now());
                    portfolioDocumentRepository.save(doc);
                    doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
                    portfolioDocumentRepository.save(doc);
                    existing.setDocument(doc);
                }
            }

            return lessonPlanRepository.save(existing);
        } else {
            // Edición sobre versión enviada/evaluada: Preservar existing intacto y generar nueva versión
            LessonPlan newPlan = new LessonPlan();
            newPlan.setPracticeAssignment(existing.getPracticeAssignment());
            newPlan.setParentPlan(existing);
            newPlan.setVersion((existing.getVersion() != null ? existing.getVersion() : 1) + 1);
            newPlan.setStatus(PlanStatus.BORRADOR);

            newPlan.setSchoolGrade((schoolGrade != null && !schoolGrade.isBlank()) ? schoolGrade : existing.getSchoolGrade());
            newPlan.setMathAxis((mathAxis != null && !mathAxis.isBlank()) ? MathAxis.valueOf(mathAxis) : existing.getMathAxis());
            newPlan.setBaseLearningObjective((objective != null && !objective.isBlank()) ? objective : existing.getBaseLearningObjective());
            newPlan.setObjectives((specificObjectives != null && !specificObjectives.isBlank()) ? specificObjectives : existing.getObjectives());
            newPlan.setContentJson((classActivities != null && !classActivities.isBlank()) ? classActivities : existing.getContentJson());
            newPlan.setCreatedAt(LocalDateTime.now());
            newPlan.setUpdatedAt(LocalDateTime.now());

            if (file != null && !file.isEmpty() && file.getSize() > 0) {
                if (!"planificacion_sin_archivo.txt".equals(file.getOriginalFilename())) {
                    String storedFileName = storageService.store(file);
                    PortfolioDocument doc = new PortfolioDocument();
                    doc.setPracticeAssignment(existing.getPracticeAssignment());
                    doc.setType(DocumentType.PLANIFICACION);
                    doc.setFileName(file.getOriginalFilename());
                    doc.setStoredFileName(storedFileName);
                    doc.setContentType(file.getContentType());
                    doc.setSizeBytes(file.getSize());
                    doc.setUploadDate(LocalDateTime.now());
                    portfolioDocumentRepository.save(doc);
                    doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
                    portfolioDocumentRepository.save(doc);
                    newPlan.setDocument(doc);
                }
            } else {
                newPlan.setDocument(null);
            }

            return lessonPlanRepository.save(newPlan);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LessonPlan obtenerPlanificacionPorId(Long id, String userEmail) {
        LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planificación no encontrada: " + id));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !plan.isAccessibleBy(userEmail)) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta planificación");
        }

        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> obtenerHistorialPlanificacion(Long id, String userEmail) {
        LessonPlan target = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planificación no encontrada: " + id));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !target.isAccessibleBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para ver esta planificación");
        }

        if (target.getPracticeAssignment() == null) {
            return List.of(target);
        }

        List<LessonPlan> allPlans = lessonPlanRepository.findByPracticeAssignmentId(target.getPracticeAssignment().getId());
        Map<Long, LessonPlan> planMap = new HashMap<>();
        for (LessonPlan p : allPlans) {
            planMap.put(p.getId(), p);
        }
        if (!planMap.containsKey(target.getId())) {
            planMap.put(target.getId(), target);
            allPlans.add(target);
        }

        // Trace root node
        LessonPlan curr = target;
        Set<Long> visited = new HashSet<>();
        visited.add(curr.getId());
        while (curr.getParentPlan() != null && curr.getParentPlan().getId() != null) {
            Long parentId = curr.getParentPlan().getId();
            if (visited.contains(parentId)) {
                break;
            }
            visited.add(parentId);
            LessonPlan parent = planMap.get(parentId);
            if (parent == null) {
                parent = lessonPlanRepository.findById(parentId).orElse(null);
            }
            if (parent != null) {
                curr = parent;
            } else {
                break;
            }
        }
        Long rootId = curr.getId();

        List<LessonPlan> lineage = new ArrayList<>();
        for (LessonPlan p : allPlans) {
            if (p.getId().equals(rootId)) {
                lineage.add(p);
                continue;
            }
            LessonPlan walker = p;
            Set<Long> traceVisited = new HashSet<>();
            traceVisited.add(walker.getId());
            boolean inLineage = false;
            while (walker.getParentPlan() != null && walker.getParentPlan().getId() != null) {
                Long pId = walker.getParentPlan().getId();
                if (pId.equals(rootId)) {
                    inLineage = true;
                    break;
                }
                if (traceVisited.contains(pId)) {
                    break;
                }
                traceVisited.add(pId);
                LessonPlan pParent = planMap.get(pId);
                if (pParent == null) {
                    pParent = lessonPlanRepository.findById(pId).orElse(null);
                }
                if (pParent != null) {
                    walker = pParent;
                } else {
                    break;
                }
            }
            if (inLineage) {
                lineage.add(p);
            }
        }

        lineage.sort(Comparator.comparingInt(p -> p.getVersion() != null ? p.getVersion() : 1));
        return lineage;
    }
    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> obtenerPlanificacionesPorAsignacion(Long assignmentId, String userEmail) {
        PracticeAssignment assignment = practiceAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NoSuchElementException("Asignación no encontrada: " + assignmentId));

        // Si en LessonPlanServiceImpl también tienes isUserAdmin(userEmail), inclúyelo:
        // boolean isAdmin = isUserAdmin(userEmail);
        if (!assignment.hasParticipant(userEmail)) { // o: if (!isAdmin && !assignment.hasParticipant(userEmail))
            throw new AccessDeniedException("No tienes permisos para ver las planificaciones de esta práctica.");
        }

        return lessonPlanRepository.findByPracticeAssignmentId(assignmentId);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public com.ubb.practicas.model.LessonPlan agregarRetroalimentacionPlan(Long id, String feedback, String userEmail) {
        com.ubb.practicas.model.LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Planificación no encontrada con ID: " + id));

        com.ubb.practicas.model.PracticeAssignment assignment = plan.getPracticeAssignment();
        boolean esAdmin = isUserAdmin(userEmail);

        if (assignment != null) {
            if (assignment.isStudent(userEmail) && !esAdmin) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Los estudiantes no pueden registrar retroalimentación formativa."
                );
            }

            boolean esDocenteAutorizado = assignment.isProfessor(userEmail)
                    || assignment.isCollaborator(userEmail)
                    || assignment.isTutor(userEmail)
                    || esAdmin;

            if (!esDocenteAutorizado) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "No tiene permisos para retroalimentar esta planificación."
                );
            }
        } else if (!esAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La planificación no tiene una asignación de práctica válida."
            );
        }

        String comentarioLimpio = (feedback != null && !feedback.trim().isEmpty()) ? feedback.trim() : null;
        plan.setTeacherFeedback(comentarioLimpio);
        plan.setFeedbackDate(comentarioLimpio != null ? java.time.LocalDateTime.now() : null);

        return lessonPlanRepository.save(plan);
    }
}
