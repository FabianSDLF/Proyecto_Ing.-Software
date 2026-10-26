package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceConflictException;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.EmailService;
import com.ubb.practicas.service.PortfolioService;
import com.ubb.practicas.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private static final long MAX_FINAL_DOCUMENT_SIZE = 20L * 1024L * 1024L; // 20 MB (20,971,520 bytes)
    private static final List<String> ALLOWED_FINAL_EXTENSIONS = List.of(".pdf", ".doc", ".docx");
    private static final List<DocumentType> FINAL_DOCUMENT_TYPES = List.of(
            DocumentType.INFORME_FINAL,
            DocumentType.PROYECTO_INTERVENCION
    );

    private final PortfolioDocumentRepository portfolioDocumentRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final LessonPlanRepository lessonPlanRepository;
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
        if (email == null) {
            return false;
        }
        User user = userRepository.findByEmail(email);
        return isUserAdmin(user);
    }

    private boolean isUserAdmin(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }
        String roleName = user.getRole().getName();
        return "coordinador de practicas".equalsIgnoreCase(roleName) || "administrador".equalsIgnoreCase(roleName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDocument> obtenerMisEvidencias(String studentEmail) {
        return portfolioDocumentRepository.findByPracticeAssignment_Student_EmailAndType(
                studentEmail, DocumentType.EVIDENCIA_APRENDIZAJE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedDocument descargarDocumento(Long id, String userEmail) {
        PortfolioDocument doc = portfolioDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !doc.isAccessibleBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para acceder a este documento");
        }

        Resource resource = storageService.loadAsResource(doc.getStoredFileName());
        String contentType = doc.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new DownloadedDocument(resource, doc.getFileName(), contentType);
    }

    @Override
    @Transactional
    public PortfolioDocument subirEvidencia(String studentEmail, Long assignmentId, MultipartFile file) {
        PracticeAssignment assignment = resolveAssignment(studentEmail, assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("No tienes una asignación de práctica activa.");
        }

        String storedFileName = storageService.store(file);

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
        emailService.enviarComprobante(studentEmail, file.getOriginalFilename());
        return portfolioDocumentRepository.save(doc);
    }

    @Override
    @Transactional
    public void eliminarDocumento(Long id, String userEmail) {
        PortfolioDocument doc = portfolioDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !doc.isOwnedBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para eliminar este documento");
        }

        storageService.deleteFile(doc.getStoredFileName());

        if (doc.getType() == DocumentType.PLANIFICACION) {
            lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete);
        }

        portfolioDocumentRepository.delete(doc);
    }

    // ==========================================
    // HU19: Métodos de Documentos de Término
    // ==========================================

    private void validarDocumentoFinal(MultipartFile file, DocumentType type) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BadRequestException("El archivo no puede ser nulo o vacío.");
        }
        if (file.getSize() > MAX_FINAL_DOCUMENT_SIZE) {
            throw new BadRequestException("El archivo excede el tamaño máximo permitido de 20 MB.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("El nombre del archivo no es válido.");
        }
        String cleanFilename = StringUtils.cleanPath(originalFilename).trim();
        int lastDot = cleanFilename.lastIndexOf('.');
        if (lastDot < 0) {
            throw new BadRequestException("El archivo debe poseer una extensión válida (.pdf, .doc, .docx).");
        }
        String extension = cleanFilename.substring(lastDot).toLowerCase();
        if (!ALLOWED_FINAL_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Formato no permitido. Solo se admiten archivos PDF (.pdf) y documentos Word (.doc, .docx).");
        }
        if (type == null || !FINAL_DOCUMENT_TYPES.contains(type)) {
            throw new BadRequestException("Tipo de documento inválido para documento de término. Debe ser INFORME_FINAL o PROYECTO_INTERVENCION.");
        }
    }

    @Override
    @Transactional
    public PortfolioDocument subirDocumentoFinal(MultipartFile file, DocumentType type, Boolean replaceConfirmed, User student) {
        if (student == null) {
            throw new IllegalArgumentException("El usuario estudiante no puede ser nulo.");
        }
        return procesarSubidaDocumentoFinal(student.getEmail(), null, file, type, replaceConfirmed);
    }

    @Override
    @Transactional
    public PortfolioDocument subirDocumentoFinal(String studentEmail, Long assignmentId, MultipartFile file, DocumentType type, Boolean replaceConfirmed) {
        return procesarSubidaDocumentoFinal(studentEmail, assignmentId, file, type, replaceConfirmed);
    }

    private PortfolioDocument procesarSubidaDocumentoFinal(String email, Long assignmentId, MultipartFile file, DocumentType type, Boolean replaceConfirmed) {
        validarDocumentoFinal(file, type);

        PracticeAssignment assignment = resolveAssignment(email, assignmentId);
        if (assignment == null) {
            throw new BadRequestException("No se encontró una asignación de práctica activa para el estudiante.");
        }

        List<PortfolioDocument> existingFinalDocs = portfolioDocumentRepository.findByPracticeAssignment_IdAndTypeIn(
                assignment.getId(), FINAL_DOCUMENT_TYPES
        );

        if (!existingFinalDocs.isEmpty()) {
            if (!Boolean.TRUE.equals(replaceConfirmed)) {
                throw new ResourceConflictException("Ya existe un documento final registrado para esta asignación de práctica. Confirme el reemplazo para actualizarlo.");
            }

            // Caso reemplazo confirmado: actualizar registro existente y reemplazar archivo físico
            PortfolioDocument docToUpdate = existingFinalDocs.get(0);
            String oldStoredFileName = docToUpdate.getStoredFileName();

            // Guardar nuevo archivo PRIMERO para garantizar integridad en caso de fallos
            String storedFileName = storageService.store(file);

            docToUpdate.setType(type);
            docToUpdate.setFileName(file.getOriginalFilename());
            docToUpdate.setStoredFileName(storedFileName);
            docToUpdate.setContentType(file.getContentType());
            docToUpdate.setSizeBytes(file.getSize());
            docToUpdate.setUploadDate(LocalDateTime.now());
            docToUpdate.setFileUrl("/api/portafolio/documentos/" + docToUpdate.getId() + "/descargar");

            PortfolioDocument savedDoc = portfolioDocumentRepository.save(docToUpdate);

            // Una vez guardado exitosamente el nuevo archivo y la entidad, eliminar el archivo previo
            if (oldStoredFileName != null && !oldStoredFileName.equals(storedFileName)) {
                try {
                    storageService.deleteFile(oldStoredFileName);
                } catch (Exception ignored) {
                }
            }

            // Si existiesen duplicados previos, removerlos para mantener unicidad estricta
            for (int i = 1; i < existingFinalDocs.size(); i++) {
                PortfolioDocument extra = existingFinalDocs.get(i);
                if (extra.getStoredFileName() != null) {
                    try {
                        storageService.deleteFile(extra.getStoredFileName());
                    } catch (Exception ignored) {
                    }
                }
                portfolioDocumentRepository.delete(extra);
            }
            portfolioDocumentRepository.flush();

            return savedDoc;
        }

        // Nuevo registro si no existía previo
        String storedFileName = storageService.store(file);

        PortfolioDocument doc = new PortfolioDocument();
        doc.setPracticeAssignment(assignment);
        doc.setType(type);
        doc.setFileName(file.getOriginalFilename());
        doc.setStoredFileName(storedFileName);
        doc.setContentType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setUploadDate(LocalDateTime.now());

        doc = portfolioDocumentRepository.save(doc);
        doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
        return portfolioDocumentRepository.save(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDocument> obtenerMisDocumentosFinales(User student) {
        if (student == null) {
            throw new IllegalArgumentException("El usuario estudiante no puede ser nulo.");
        }
        return obtenerMisDocumentosFinales(student.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDocument> obtenerMisDocumentosFinales(String studentEmail) {
        return portfolioDocumentRepository.findByPracticeAssignment_Student_EmailAndTypeIn(
                studentEmail, FINAL_DOCUMENT_TYPES
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDocument> obtenerDocumentosFinalesPorAsignacion(Long assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("El ID de asignación no puede ser nulo.");
        }
        return portfolioDocumentRepository.findByPracticeAssignment_IdAndTypeIn(
                assignmentId, FINAL_DOCUMENT_TYPES
        );
    }

    @Override
    @Transactional
    public void eliminarDocumentoFinal(Long id, String userEmail) {
        PortfolioDocument doc = portfolioDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !doc.isOwnedBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para eliminar este documento");
        }

        if (!FINAL_DOCUMENT_TYPES.contains(doc.getType())) {
            throw new BadRequestException("El documento no corresponde a un documento de término.");
        }

        if (doc.getStoredFileName() != null) {
            try {
                storageService.deleteFile(doc.getStoredFileName());
            } catch (Exception ignored) {
            }
        }
        portfolioDocumentRepository.delete(doc);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<PortfolioDocument> obtenerEvidenciasPorAsignacion(Long assignmentId, String userEmail) {
        com.ubb.practicas.model.PracticeAssignment assignment = practiceAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Asignación no encontrada: " + assignmentId));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !assignment.hasParticipant(userEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para ver las evidencias de esta práctica.");
        }

        return portfolioDocumentRepository.findByPracticeAssignment_IdAndTypeIn(
                assignmentId,
                java.util.List.of(com.ubb.practicas.model.enums.DocumentType.EVIDENCIA_APRENDIZAJE)
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PortfolioDocument agregarRetroalimentacion(Long id, String feedback, String userEmail) {
        PortfolioDocument doc = portfolioDocumentRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Documento no encontrado con ID: " + id));

        com.ubb.practicas.model.PracticeAssignment assignment = doc.getPracticeAssignment();
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
                        "No tiene permisos para retroalimentar este portafolio."
                );
            }
        } else if (!esAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "El documento no tiene una asignación de práctica válida."
            );
        }

        String comentarioLimpio = (feedback != null && !feedback.trim().isEmpty()) ? feedback.trim() : null;
        if (comentarioLimpio == null) throw new BadRequestException("El comentario no puede estar vacío.");
        doc.setTeacherFeedback(com.ubb.practicas.util.FeedbackThreadHelper.appendComment(doc.getTeacherFeedback(), doc.getFeedbackDate(), comentarioLimpio, userRepository.findByEmail(userEmail), userEmail));
        doc.setFeedbackDate(comentarioLimpio != null ? java.time.LocalDateTime.now() : null);

        return portfolioDocumentRepository.save(doc);
    }
}
