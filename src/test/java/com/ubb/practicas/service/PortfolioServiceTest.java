package com.ubb.practicas.service;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.exception.StorageException;
import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.impl.PortfolioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTest {

    @Mock
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Mock
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Mock
    private LessonPlanRepository lessonPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private User student;
    private User teacher;
    private User admin;
    private PracticeAssignment assignment;
    private PortfolioDocument doc;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setEmail("student@ubb.cl");
        student.setRole(new Role(2L, "Estudiante"));

        teacher = new User();
        teacher.setId(2L);
        teacher.setEmail("tutor@ubb.cl");
        teacher.setRole(new Role(3L, "Tutor"));

        admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@ubb.cl");
        admin.setRole(new Role(1L, "Administrador"));

        assignment = new PracticeAssignment();
        assignment.setId(10L);
        assignment.setStudent(student);
        assignment.setTutor(teacher);
        assignment.setStatus(AssignmentStatus.ACTIVA);

        doc = new PortfolioDocument();
        doc.setId(100L);
        doc.setPracticeAssignment(assignment);
        doc.setStoredFileName("stored-uuid-file.pdf");
        doc.setFileName("evidencia.pdf");
        doc.setContentType("application/pdf");
        doc.setType(DocumentType.EVIDENCIA_APRENDIZAJE);
    }

    @Test
    void subirEvidencia_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "evidencia.pdf", "application/pdf", "data".getBytes());

        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of(assignment));
        when(storageService.store(file)).thenReturn("uuid-evidencia.pdf");
        when(portfolioDocumentRepository.save(any(PortfolioDocument.class))).thenAnswer(inv -> {
            PortfolioDocument d = inv.getArgument(0);
            d.setId(100L);
            return d;
        });

        PortfolioDocument result = portfolioService.subirEvidencia("student@ubb.cl", null, file);

        assertNotNull(result);
        assertEquals(DocumentType.EVIDENCIA_APRENDIZAJE, result.getType());
        assertEquals("/api/portafolio/documentos/100/descargar", result.getFileUrl());
        verify(storageService).store(file);
    }

    @Test
    void subirEvidencia_NoActiveAssignment_ThrowsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                portfolioService.subirEvidencia("student@ubb.cl", null, file));

        verify(storageService, never()).store(any());
        verify(portfolioDocumentRepository, never()).save(any());
    }

    @Test
    void subirEvidencia_StorageException_Propagated() {
        MockMultipartFile file = new MockMultipartFile("file", "evil.exe", "application/octet-stream", "data".getBytes());

        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of(assignment));
        when(storageService.store(file)).thenThrow(new StorageException("Tipo de archivo no permitido"));

        assertThrows(StorageException.class, () ->
                portfolioService.subirEvidencia("student@ubb.cl", null, file));

        verify(portfolioDocumentRepository, never()).save(any());
    }

    @Test
    void descargarDocumento_Success_OwnerStudent() {
        Resource mockResource = new ByteArrayResource("content".getBytes());

        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("student@ubb.cl")).thenReturn(student);
        when(storageService.loadAsResource("stored-uuid-file.pdf")).thenReturn(mockResource);

        DownloadedDocument downloaded = portfolioService.descargarDocumento(100L, "student@ubb.cl");

        assertNotNull(downloaded);
        assertEquals("evidencia.pdf", downloaded.fileName());
        assertEquals("application/pdf", downloaded.contentType());
        assertEquals(mockResource, downloaded.resource());
    }

    @Test
    void descargarDocumento_Success_SupervisingTeacher() {
        Resource mockResource = new ByteArrayResource("content".getBytes());

        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("tutor@ubb.cl")).thenReturn(teacher);
        when(storageService.loadAsResource("stored-uuid-file.pdf")).thenReturn(mockResource);

        DownloadedDocument downloaded = portfolioService.descargarDocumento(100L, "tutor@ubb.cl");

        assertNotNull(downloaded);
        assertEquals(mockResource, downloaded.resource());
    }

    @Test
    void descargarDocumento_Success_AdminBypass() {
        Resource mockResource = new ByteArrayResource("content".getBytes());

        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("admin@ubb.cl")).thenReturn(admin);
        when(storageService.loadAsResource("stored-uuid-file.pdf")).thenReturn(mockResource);

        DownloadedDocument downloaded = portfolioService.descargarDocumento(100L, "admin@ubb.cl");

        assertNotNull(downloaded);
        assertEquals(mockResource, downloaded.resource());
    }

    @Test
    void descargarDocumento_IdorProtection_ThrowsAccessDeniedException() {
        User outsider = new User();
        outsider.setEmail("outsider@ubb.cl");
        outsider.setRole(new Role(2L, "Estudiante"));

        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("outsider@ubb.cl")).thenReturn(outsider);

        assertThrows(AccessDeniedException.class, () ->
                portfolioService.descargarDocumento(100L, "outsider@ubb.cl"));

        verify(storageService, never()).loadAsResource(any());
    }

    @Test
    void descargarDocumento_NotFound_ThrowsNoSuchElementException() {
        when(portfolioDocumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                portfolioService.descargarDocumento(999L, "student@ubb.cl"));
    }

    @Test
    void eliminarDocumento_Success_OwnerStudent() {
        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("student@ubb.cl")).thenReturn(student);

        portfolioService.eliminarDocumento(100L, "student@ubb.cl");

        verify(storageService).deleteFile("stored-uuid-file.pdf");
        verify(portfolioDocumentRepository).delete(doc);
    }

    @Test
    void eliminarDocumento_Success_PlanificacionCascadesLessonPlan() {
        doc.setType(DocumentType.PLANIFICACION);
        LessonPlan plan = new LessonPlan();
        plan.setId(50L);

        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("student@ubb.cl")).thenReturn(student);
        when(lessonPlanRepository.findByDocumentId(100L)).thenReturn(Optional.of(plan));

        portfolioService.eliminarDocumento(100L, "student@ubb.cl");

        verify(lessonPlanRepository).delete(plan);
        verify(storageService).deleteFile("stored-uuid-file.pdf");
        verify(portfolioDocumentRepository).delete(doc);
    }

    @Test
    void eliminarDocumento_IdorProtection_ThrowsAccessDeniedException() {
        User anotherStudent = new User();
        anotherStudent.setEmail("other@ubb.cl");
        anotherStudent.setRole(new Role(2L, "Estudiante"));

        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("other@ubb.cl")).thenReturn(anotherStudent);

        assertThrows(AccessDeniedException.class, () ->
                portfolioService.eliminarDocumento(100L, "other@ubb.cl"));

        verify(storageService, never()).deleteFile(any());
        verify(portfolioDocumentRepository, never()).delete(any());
    }

    @Test
    void eliminarDocumento_Success_AdminBypass() {
        when(portfolioDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("admin@ubb.cl")).thenReturn(admin);

        portfolioService.eliminarDocumento(100L, "admin@ubb.cl");

        verify(storageService).deleteFile("stored-uuid-file.pdf");
        verify(portfolioDocumentRepository).delete(doc);
    }

    @Test
    void eliminarDocumento_NotFound_ThrowsNoSuchElementException() {
        when(portfolioDocumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                portfolioService.eliminarDocumento(999L, "student@ubb.cl"));
    }

    @Test
    void obtenerMisEvidencias_ReturnsStudentDocuments() {
        when(portfolioDocumentRepository.findByPracticeAssignment_Student_EmailAndType(
                "student@ubb.cl", DocumentType.EVIDENCIA_APRENDIZAJE))
                .thenReturn(List.of(doc));

        List<PortfolioDocument> results = portfolioService.obtenerMisEvidencias("student@ubb.cl");

        assertEquals(1, results.size());
        assertEquals("evidencia.pdf", results.get(0).getFileName());
    }
}
