package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.PortfolioService;
import com.ubb.practicas.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioDocumentRepository portfolioDocumentRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final LessonPlanRepository lessonPlanRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

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
}
