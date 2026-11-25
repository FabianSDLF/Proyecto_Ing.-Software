package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.ChatbotLog;
import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.Notification;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.NotificationType;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.ChatbotLogRepository;
import com.ubb.practicas.repository.ChatbotMessageRepository;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.NotificationRepository;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final NotificationRepository notificationRepository;
    private final ChatbotLogRepository chatbotLogRepository;
    private final ChatbotMessageRepository chatbotMessageRepository;

    private void deletePhysicalFileAfterCommit(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storageService.deleteFile(storedFileName);
                }
            });
        } else {
            storageService.deleteFile(storedFileName);
        }
    }

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
                .max(Comparator.comparingLong(a -> a.getId() != null ? a.getId() : 0L))
                .orElse(activeAssignments.getFirst());
    }

    private boolean isUserAdmin(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || user.getRole() == null) return false;
        String roleName = user.getRole().getName();
        return "Coordinador de practicas".equalsIgnoreCase(roleName) ||
               "Administrador".equalsIgnoreCase(roleName) ||
               "Admin".equalsIgnoreCase(roleName);
    }

    private List<LessonPlan> filterLatestVersions(List<LessonPlan> plans) {
        if (plans == null || plans.size() <= 1) {
            return plans;
        }
        Set<Long> parentIds = new HashSet<>();
        for (LessonPlan p : plans) {
            if (p != null && p.getParentPlan() != null && p.getParentPlan().getId() != null) {
                parentIds.add(p.getParentPlan().getId());
            }
        }
        if (parentIds.isEmpty()) {
            return plans;
        }
        List<LessonPlan> latest = new ArrayList<>();
        for (LessonPlan p : plans) {
            if (p != null && (p.getId() == null || !parentIds.contains(p.getId()))) {
                latest.add(p);
            }
        }
        return latest;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> obtenerMisPlanificaciones(String studentEmail) {
        return filterLatestVersions(lessonPlanRepository.findByPracticeAssignment_Student_Email(studentEmail));
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

        // Re-vincular versiones hijas si existieran para evitar violar fk_plan_parent
        List<LessonPlan> children = lessonPlanRepository.findByParentPlanId(plan.getId());
        if (children != null && !children.isEmpty()) {
            for (LessonPlan child : children) {
                child.setParentPlan(plan.getParentPlan());
            }
            lessonPlanRepository.saveAll(children);
        }

        // Eliminar registros de revisión IA y mensajes asociados antes de eliminar la planificación
        if (chatbotLogRepository != null) {
            List<ChatbotLog> logs = chatbotLogRepository.findByLessonPlanId(plan.getId());
            if (logs != null && !logs.isEmpty()) {
                List<Long> logIds = logs.stream()
                        .map(ChatbotLog::getId)
                        .filter(Objects::nonNull)
                        .toList();
                if (!logIds.isEmpty() && chatbotMessageRepository != null) {
                    chatbotMessageRepository.deleteByChatbotLogIdIn(logIds);
                }
                chatbotLogRepository.deleteAll(logs);
            }
        }

        PortfolioDocument doc = plan.getDocument();
        String storedFileName = doc != null ? doc.getStoredFileName() : null;
        if (doc != null) {
            plan.setDocument(null);
        }

        lessonPlanRepository.delete(plan);
        lessonPlanRepository.flush();

        if (doc != null) {
            portfolioDocumentRepository.delete(doc);
            portfolioDocumentRepository.flush();
            if (storedFileName != null && !storedFileName.isBlank()
                    && portfolioDocumentRepository.countByStoredFileName(storedFileName) == 0) {
                deletePhysicalFileAfterCommit(storedFileName);
            }
        }
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

        boolean isPureDraftEdit = existing.getStatus() == PlanStatus.BORRADOR
                && (existing.getTeacherFeedback() == null || existing.getTeacherFeedback().isBlank())
                && (classActivities == null || classActivities.isBlank());

        if (isPureDraftEdit) {
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
            // Edición sobre versión enviada/evaluada o desde modal "Guardar Nueva Versión":
            // Preservar existing intacto y generar nueva versión
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
            } else if (existing.getDocument() != null) {
                PortfolioDocument prevDoc = existing.getDocument();
                PortfolioDocument clonedDoc = new PortfolioDocument();
                clonedDoc.setPracticeAssignment(existing.getPracticeAssignment());
                clonedDoc.setType(DocumentType.PLANIFICACION);
                clonedDoc.setFileName(prevDoc.getFileName());
                clonedDoc.setStoredFileName(prevDoc.getStoredFileName());
                clonedDoc.setContentType(prevDoc.getContentType());
                clonedDoc.setSizeBytes(prevDoc.getSizeBytes());
                clonedDoc.setUploadDate(prevDoc.getUploadDate() != null ? prevDoc.getUploadDate() : LocalDateTime.now());
                portfolioDocumentRepository.save(clonedDoc);
                clonedDoc.setFileUrl("/api/portafolio/documentos/" + clonedDoc.getId() + "/descargar");
                portfolioDocumentRepository.save(clonedDoc);
                newPlan.setDocument(clonedDoc);
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

        if (!isUserAdmin(userEmail) && !assignment.hasParticipant(userEmail)) {
            throw new AccessDeniedException("No tienes permisos para ver las planificaciones de esta práctica.");
        }

        return filterLatestVersions(lessonPlanRepository.findByPracticeAssignmentId(assignmentId));
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
        if (comentarioLimpio == null) throw new IllegalArgumentException("El comentario no puede estar vacío.");
        plan.setTeacherFeedback(com.ubb.practicas.util.FeedbackThreadHelper.appendComment(plan.getTeacherFeedback(), plan.getFeedbackDate(), comentarioLimpio, userRepository.findByEmail(userEmail), userEmail));
        plan.setFeedbackDate(java.time.LocalDateTime.now());
        // Un comentario no equivale a enviar ni aprobar una planificación.

        return lessonPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public LessonPlan cambiarEstadoPlanificacion(Long id, String statusStr, String feedback, String userEmail) {
        LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planificación no encontrada con ID: " + id));

        PracticeAssignment assignment = plan.getPracticeAssignment();
        boolean esAdmin = isUserAdmin(userEmail);

        if (assignment != null) {
            if (assignment.isStudent(userEmail) && !esAdmin) {
                throw new AccessDeniedException("Los estudiantes no pueden aprobar o rechazar planificaciones.");
            }

            boolean esDocenteAutorizado = assignment.isProfessor(userEmail)
                    || assignment.isCollaborator(userEmail)
                    || assignment.isTutor(userEmail)
                    || esAdmin;

            if (!esDocenteAutorizado) {
                throw new AccessDeniedException("No tiene permisos para evaluar el estado de esta planificación.");
            }
        } else if (!esAdmin) {
            throw new AccessDeniedException("La planificación no tiene una asignación de práctica válida.");
        }

        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo estado de la planificación es obligatorio.");
        }

        String normalized = statusStr.trim().toUpperCase();
        PlanStatus nuevoEstado;
        if ("APROBADO".equals(normalized) || "APROBADA".equals(normalized)) {
            nuevoEstado = PlanStatus.APROBADO;
        } else if ("REQUIERE_CAMBIOS".equals(normalized) || "RECHAZADO".equals(normalized) || "RECHAZADA".equals(normalized)) {
            nuevoEstado = PlanStatus.REQUIERE_CAMBIOS;
        } else if ("EN_REVISION_DOCENTE".equals(normalized)) {
            nuevoEstado = PlanStatus.EN_REVISION_DOCENTE;
        } else {
            try {
                nuevoEstado = PlanStatus.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de planificación no válido: " + statusStr);
            }
        }

        String comentarioLimpio = (feedback != null && !feedback.trim().isEmpty()) ? feedback.trim() : null;
        if (nuevoEstado == PlanStatus.REQUIERE_CAMBIOS && comentarioLimpio == null) {
            throw new IllegalArgumentException("Debe ingresar una observación indicando los cambios solicitados al estudiante.");
        }

        if (comentarioLimpio != null) {
            String prefijo = (nuevoEstado == PlanStatus.APROBADO)
                    ? "[✅ PLANIFICACIÓN APROBADA - VISTO BUENO]: "
                    : (nuevoEstado == PlanStatus.REQUIERE_CAMBIOS)
                    ? "[⚠️ SOLICITUD DE CAMBIOS]: "
                    : "";
            plan.setTeacherFeedback(
                    com.ubb.practicas.util.FeedbackThreadHelper.appendComment(
                            plan.getTeacherFeedback(),
                            plan.getFeedbackDate(),
                            prefijo + comentarioLimpio,
                            userRepository.findByEmail(userEmail),
                            userEmail
                    )
            );
            plan.setFeedbackDate(LocalDateTime.now());
        } else if (nuevoEstado == PlanStatus.APROBADO) {
            plan.setTeacherFeedback(
                    com.ubb.practicas.util.FeedbackThreadHelper.appendComment(
                            plan.getTeacherFeedback(),
                            plan.getFeedbackDate(),
                            "[✅ PLANIFICACIÓN APROBADA - VISTO BUENO]: Planificación aprobada y autorizada para dictar la clase.",
                            userRepository.findByEmail(userEmail),
                            userEmail
                    )
            );
            plan.setFeedbackDate(LocalDateTime.now());
        }

        boolean recienAprobada = nuevoEstado == PlanStatus.APROBADO && plan.getStatus() != PlanStatus.APROBADO;
        plan.setStatus(nuevoEstado);
        plan.setUpdatedAt(LocalDateTime.now());

        LessonPlan guardada = lessonPlanRepository.save(plan);
        if (recienAprobada) notificarPlanificacionAprobada(guardada);
        return guardada;
    }

    @Override
    @Transactional
    public LessonPlan aprobarPlanificacion(Long planificacionId, String correoUsuario) {
        return cambiarEstadoPlanificacion(planificacionId, "APROBADO", null, correoUsuario);
    }

    private void notificarPlanificacionAprobada(LessonPlan planificacionGuardada) {
        PracticeAssignment asignacion = planificacionGuardada.getPracticeAssignment();
        if (asignacion != null && asignacion.getStudent() != null) {
            String cursoTexto = planificacionGuardada.getSchoolGrade() != null ? planificacionGuardada.getSchoolGrade() : "";
            String ejeTexto = planificacionGuardada.getMathAxis() != null ? planificacionGuardada.getMathAxis().name() : "";

            if (notificationRepository != null) {
                Notification alertaEstudiante = new Notification();
                alertaEstudiante.setTargetUser(asignacion.getStudent());
                alertaEstudiante.setType(NotificationType.PLANIFICACION_APROBADA);
                alertaEstudiante.setMessage("Tu planificación de " + cursoTexto + " (" + ejeTexto + ") fue aprobada. Ya puedes aplicarla en el colegio.");
                alertaEstudiante.setRead(false);
                alertaEstudiante.setCreatedAt(LocalDateTime.now());
                alertaEstudiante.setReferenceId(planificacionGuardada.getId());
                alertaEstudiante.setReferenceType("PLANIFICACION");
                notificationRepository.save(alertaEstudiante);
            }

            if (emailService != null) {
                emailService.enviarNotificacionPlanificacionAprobada(
                        asignacion.getStudent().getEmail(),
                        cursoTexto,
                        ejeTexto
                );
            }
        }

    }
}
