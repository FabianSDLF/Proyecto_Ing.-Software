package com.ubb.practicas.service;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.impl.LessonPlanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonPlanServiceTest {

    @Mock
    private LessonPlanRepository lessonPlanRepository;

    @Mock
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Mock
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LessonPlanServiceImpl lessonPlanService;

    private User student;
    private User admin;
    private PracticeAssignment assignment;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setEmail("student@ubb.cl");
        student.setRole(new Role(2L, "Estudiante"));

        admin = new User();
        admin.setId(2L);
        admin.setEmail("admin@ubb.cl");
        admin.setRole(new Role(1L, "Administrador"));

        assignment = new PracticeAssignment();
        assignment.setId(10L);
        assignment.setStudent(student);
        assignment.setStatus(AssignmentStatus.ACTIVA);
    }

    @Test
    void crearPlanificacion_WithFile_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "data".getBytes());

        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of(assignment));
        when(storageService.store(file)).thenReturn("uuid-plan.pdf");
        when(portfolioDocumentRepository.save(any(PortfolioDocument.class))).thenAnswer(inv -> {
            PortfolioDocument d = inv.getArgument(0);
            d.setId(55L);
            return d;
        });
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonPlan result = lessonPlanService.crearPlanificacion(
                "student@ubb.cl", null, "1 Medio", "NUMEROS",
                "Obj General", "Obj Especificos", "Actividades", file
        );

        assertNotNull(result);
        assertEquals(PlanStatus.BORRADOR, result.getStatus());
        assertEquals(MathAxis.NUMEROS, result.getMathAxis());
        assertNotNull(result.getDocument());
        assertEquals(DocumentType.PLANIFICACION, result.getDocument().getType());
        verify(storageService).store(file);
        verify(lessonPlanRepository).save(any(LessonPlan.class));
    }

    @Test
    void crearPlanificacion_WithoutFile_Success() {
        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of(assignment));
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonPlan result = lessonPlanService.crearPlanificacion(
                "student@ubb.cl", null, "2 Medio", "GEOMETRIA",
                "Obj General", "Obj Especificos", "Actividades", null
        );

        assertNotNull(result);
        assertNull(result.getDocument());
        assertEquals(MathAxis.GEOMETRIA, result.getMathAxis());
        verify(storageService, never()).store(any());
        verify(lessonPlanRepository).save(any(LessonPlan.class));
    }

    @Test
    void crearPlanificacion_PlaceholderFile_Ignored() {
        MockMultipartFile file = new MockMultipartFile("file", "planificacion_sin_archivo.txt", "text/plain", "placeholder".getBytes());

        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of(assignment));
        when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonPlan result = lessonPlanService.crearPlanificacion(
                "student@ubb.cl", null, "3 Medio", "ALGEBRA",
                "Obj General", "Obj Especificos", "Actividades", file
        );

        assertNotNull(result);
        assertNull(result.getDocument());
        verify(storageService, never()).store(any());
    }

    @Test
    void crearPlanificacion_NoActiveAssignment_ThrowsException() {
        when(practiceAssignmentRepository.findByStudentEmailAndStatus("student@ubb.cl", AssignmentStatus.ACTIVA))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
                lessonPlanService.crearPlanificacion(
                        "student@ubb.cl", null, "1 Medio", "PROBABILIDAD",
                        "Obj", "Esp", "Act", null
                ));

        verify(lessonPlanRepository, never()).save(any());
    }

    @Test
    void eliminarPlanificacion_Success_Owner() {
        LessonPlan plan = new LessonPlan();
        plan.setId(10L);
        plan.setPracticeAssignment(assignment);
        plan.setDocument(null);

        when(lessonPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(userRepository.findByEmail("student@ubb.cl")).thenReturn(student);

        lessonPlanService.eliminarPlanificacion(10L, "student@ubb.cl");

        verify(lessonPlanRepository).delete(plan);
        verify(storageService, never()).deleteFile(any());
    }

    @Test
    void eliminarPlanificacion_Success_OwnerWithAttachedDocument() {
        PortfolioDocument doc = new PortfolioDocument();
        doc.setStoredFileName("stored-plan.pdf");

        LessonPlan plan = new LessonPlan();
        plan.setId(10L);
        plan.setPracticeAssignment(assignment);
        plan.setDocument(doc);

        when(lessonPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(userRepository.findByEmail("student@ubb.cl")).thenReturn(student);

        lessonPlanService.eliminarPlanificacion(10L, "student@ubb.cl");

        verify(storageService).deleteFile("stored-plan.pdf");
        verify(lessonPlanRepository).delete(plan);
    }

    @Test
    void eliminarPlanificacion_Success_AdminBypass() {
        LessonPlan plan = new LessonPlan();
        plan.setId(10L);
        plan.setPracticeAssignment(assignment);

        when(lessonPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(userRepository.findByEmail("admin@ubb.cl")).thenReturn(admin);

        lessonPlanService.eliminarPlanificacion(10L, "admin@ubb.cl");

        verify(lessonPlanRepository).delete(plan);
    }

    @Test
    void eliminarPlanificacion_OwnershipViolation_ThrowsAccessDeniedException() {
        LessonPlan plan = new LessonPlan();
        plan.setId(10L);
        plan.setPracticeAssignment(assignment);

        User anotherStudent = new User();
        anotherStudent.setEmail("other@ubb.cl");
        anotherStudent.setRole(new Role(2L, "Estudiante"));

        when(lessonPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(userRepository.findByEmail("other@ubb.cl")).thenReturn(anotherStudent);

        assertThrows(AccessDeniedException.class, () ->
                lessonPlanService.eliminarPlanificacion(10L, "other@ubb.cl"));

        verify(lessonPlanRepository, never()).delete(any());
    }

    @Test
    void eliminarPlanificacion_NotFound_ThrowsNoSuchElementException() {
        when(lessonPlanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                lessonPlanService.eliminarPlanificacion(999L, "student@ubb.cl"));
    }

    @Test
    void obtenerMisPlanificaciones_ReturnsStudentPlans() {
        LessonPlan plan = new LessonPlan();
        plan.setId(1L);

        when(lessonPlanRepository.findByPracticeAssignment_Student_Email("student@ubb.cl"))
                .thenReturn(List.of(plan));

        List<LessonPlan> plans = lessonPlanService.obtenerMisPlanificaciones("student@ubb.cl");

        assertEquals(1, plans.size());
        assertEquals(1L, plans.get(0).getId());
    }
}
