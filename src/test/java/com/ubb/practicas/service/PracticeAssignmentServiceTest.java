package com.ubb.practicas.service;

import com.ubb.practicas.exception.ForbiddenException;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.PracticeCenterRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.impl.PracticeAssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PracticeAssignmentServiceTest {

    @Mock
    private PracticeAssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private PracticeCenterRepository centerRepository;

    @InjectMocks
    private PracticeAssignmentServiceImpl assignmentService;

    private User student;
    private Subject subject;
    private PracticeCenter center;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(10L);
        student.setEmail("alumno@ubb.cl");

        subject = new Subject();
        subject.setId(20L);
        subject.setName("Práctica Profesional");

        center = new PracticeCenter();
        center.setId(30L);
        center.setName("Liceo Bicentenario");
    }

    @Test
    void crearAsignacion_Success_DefaultsToActiva() {
        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setSubject(subject);
        assignment.setCenter(center);
        assignment.setStatus(null);

        when(userRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.existsById(20L)).thenReturn(true);
        when(centerRepository.existsById(30L)).thenReturn(true);
        when(assignmentRepository.save(any(PracticeAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        PracticeAssignment result = assignmentService.crearAsignacion(assignment);

        assertNotNull(result);
        assertEquals(AssignmentStatus.ACTIVA, result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void crearAsignacion_MissingStudent_ThrowsIllegalArgumentException() {
        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setStudent(null);
        assignment.setSubject(subject);

        assertThrows(IllegalArgumentException.class, () -> assignmentService.crearAsignacion(assignment));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void crearAsignacion_MissingSubject_ThrowsIllegalArgumentException() {
        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setSubject(null);

        assertThrows(IllegalArgumentException.class, () -> assignmentService.crearAsignacion(assignment));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void crearAsignacion_StudentNotInDb_ThrowsIllegalArgumentException() {
        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setSubject(subject);

        when(userRepository.existsById(10L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> assignmentService.crearAsignacion(assignment));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void crearAsignacion_SubjectNotInDb_ThrowsIllegalArgumentException() {
        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setSubject(subject);

        when(userRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.existsById(20L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> assignmentService.crearAsignacion(assignment));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void crearAsignacion_CenterNotInDb_ThrowsIllegalArgumentException() {
        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setSubject(subject);
        assignment.setCenter(center);

        when(userRepository.existsById(10L)).thenReturn(true);
        when(subjectRepository.existsById(20L)).thenReturn(true);
        when(centerRepository.existsById(30L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> assignmentService.crearAsignacion(assignment));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void obtenerEstudiantesPorProfesor_Self_ProfesorAsignatura() {
        User teacher = new User();
        teacher.setId(10L);
        teacher.setEmail("profe@ubb.cl");
        teacher.setRole(new Role(1L, "Profesor de Asignatura"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmail("profe@ubb.cl")).thenReturn(teacher);
        when(assignmentRepository.findBySubjectProfessorId(10L)).thenReturn(List.of(new PracticeAssignment()));

        List<PracticeAssignment> list = assignmentService.obtenerEstudiantesPorProfesor(10L, "profe@ubb.cl");

        assertNotNull(list);
        assertEquals(1, list.size());
        verify(assignmentRepository).findBySubjectProfessorId(10L);
    }

    @Test
    void obtenerEstudiantesPorProfesor_Self_ProfesorColaborador() {
        User colab = new User();
        colab.setId(11L);
        colab.setEmail("colab@ubb.cl");
        colab.setRole(new Role(2L, "Profesor Colaborador"));

        when(userRepository.findById(11L)).thenReturn(Optional.of(colab));
        when(userRepository.findByEmail("colab@ubb.cl")).thenReturn(colab);
        when(assignmentRepository.findByCollaboratorId(11L)).thenReturn(List.of(new PracticeAssignment()));

        List<PracticeAssignment> list = assignmentService.obtenerEstudiantesPorProfesor(11L, "colab@ubb.cl");

        assertNotNull(list);
        assertEquals(1, list.size());
        verify(assignmentRepository).findByCollaboratorId(11L);
    }

    @Test
    void obtenerEstudiantesPorProfesor_Self_Tutor() {
        User tutor = new User();
        tutor.setId(12L);
        tutor.setEmail("tutor@ubb.cl");
        tutor.setRole(new Role(3L, "Tutor"));

        when(userRepository.findById(12L)).thenReturn(Optional.of(tutor));
        when(userRepository.findByEmail("tutor@ubb.cl")).thenReturn(tutor);
        when(assignmentRepository.findByTutorId(12L)).thenReturn(List.of(new PracticeAssignment()));

        List<PracticeAssignment> list = assignmentService.obtenerEstudiantesPorProfesor(12L, "tutor@ubb.cl");

        assertNotNull(list);
        assertEquals(1, list.size());
        verify(assignmentRepository).findByTutorId(12L);
    }

    @Test
    void obtenerEstudiantesPorProfesor_Admin_CanQueryAnyTeacher() {
        User teacher = new User();
        teacher.setId(10L);
        teacher.setEmail("profe@ubb.cl");
        teacher.setRole(new Role(1L, "Profesor de Asignatura"));

        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@ubb.cl");
        admin.setRole(new Role(4L, "Coordinador de practicas"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmail("admin@ubb.cl")).thenReturn(admin);
        when(assignmentRepository.findBySubjectProfessorId(10L)).thenReturn(List.of(new PracticeAssignment()));

        List<PracticeAssignment> list = assignmentService.obtenerEstudiantesPorProfesor(10L, "admin@ubb.cl");

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    void obtenerEstudiantesPorProfesor_UnauthorizedCaller_ThrowsAccessDeniedException() {
        User teacher = new User();
        teacher.setId(10L);
        teacher.setEmail("profe@ubb.cl");
        teacher.setRole(new Role(1L, "Profesor de Asignatura"));

        User otherUser = new User();
        otherUser.setId(20L);
        otherUser.setEmail("otro@ubb.cl");
        otherUser.setRole(new Role(5L, "Estudiante"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmail("otro@ubb.cl")).thenReturn(otherUser);

        assertThrows(AccessDeniedException.class, () ->
                assignmentService.obtenerEstudiantesPorProfesor(10L, "otro@ubb.cl"));
    }

    @Test
    void obtenerEstudiantesPorProfesor_NonTeachingRole_ThrowsForbiddenException() {
        User studentUser = new User();
        studentUser.setId(30L);
        studentUser.setEmail("estudiante@ubb.cl");
        studentUser.setRole(new Role(5L, "Estudiante"));

        User admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@ubb.cl");
        admin.setRole(new Role(4L, "Administrador"));

        when(userRepository.findById(30L)).thenReturn(Optional.of(studentUser));
        when(userRepository.findByEmail("admin@ubb.cl")).thenReturn(admin);

        assertThrows(ForbiddenException.class, () ->
                assignmentService.obtenerEstudiantesPorProfesor(30L, "admin@ubb.cl"));
    }

    @Test
    void obtenerEstudiantesPorProfesor_TargetNotFound_ThrowsNoSuchElementException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                assignmentService.obtenerEstudiantesPorProfesor(999L, "admin@ubb.cl"));
    }

    @Test
    void obtenerEstudiantesPorProfesor_CallerNotFound_ThrowsSecurityException() {
        User teacher = new User();
        teacher.setId(10L);
        teacher.setRole(new Role(1L, "Profesor de Asignatura"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(userRepository.findByEmail("caller@ubb.cl")).thenReturn(null);

        assertThrows(SecurityException.class, () ->
                assignmentService.obtenerEstudiantesPorProfesor(10L, "caller@ubb.cl"));
    }

    @Test
    void eliminarAsignacion_Success() {
        when(assignmentRepository.existsById(1L)).thenReturn(true);

        assignmentService.eliminarAsignacion(1L);

        verify(assignmentRepository).deleteById(1L);
    }

    @Test
    void eliminarAsignacion_NotFound_ThrowsNoSuchElementException() {
        when(assignmentRepository.existsById(99L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> assignmentService.eliminarAsignacion(99L));
        verify(assignmentRepository, never()).deleteById(any());
    }

    @Test
    void listarAsignaciones_ReturnsList() {
        when(assignmentRepository.findAll()).thenReturn(List.of(new PracticeAssignment()));

        List<PracticeAssignment> list = assignmentService.listarAsignaciones();

        assertEquals(1, list.size());
    }
}
