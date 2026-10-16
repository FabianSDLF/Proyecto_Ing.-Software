package com.ubb.practicas.service;

import com.ubb.practicas.dto.StudentPairAssignmentSummaryDTO;
import com.ubb.practicas.dto.StudentPairCreateRequest;
import com.ubb.practicas.dto.StudentPairDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.AcademicPeriodRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.StudentPairRepository;
import com.ubb.practicas.service.impl.StudentPairServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: StudentPairService")
public class StudentPairServiceTest {

    @Mock
    private StudentPairRepository studentPairRepository;

    @Mock
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @InjectMocks
    private StudentPairServiceImpl studentPairService;

    private AcademicPeriod period;
    private User student1;
    private User student2;
    private Subject subjectIntermedio;
    private Subject subjectInicial;
    private PracticeCenter center;
    private PracticeAssignment asig1;
    private PracticeAssignment asig2;

    @BeforeEach
    void setUp() {
        period = new AcademicPeriod();
        period.setId(1L);
        period.setName("2026-1");

        student1 = new User();
        student1.setId(10L);
        student1.setFullName("Estudiante Uno");
        student1.setEmail("estudiante1@alumnos.ubb.cl");

        student2 = new User();
        student2.setId(20L);
        student2.setFullName("Estudiante Dos");
        student2.setEmail("estudiante2@alumnos.ubb.cl");

        subjectIntermedio = new Subject();
        subjectIntermedio.setId(100L);
        subjectIntermedio.setName("Práctica Intermedia I");
        subjectIntermedio.setCycle(PracticeCycle.INTERMEDIO);

        subjectInicial = new Subject();
        subjectInicial.setId(101L);
        subjectInicial.setName("Práctica Inicial");
        subjectInicial.setCycle(PracticeCycle.INICIAL);

        center = new PracticeCenter();
        center.setId(200L);
        center.setName("Colegio Bicentenario");

        asig1 = new PracticeAssignment();
        asig1.setId(1L);
        asig1.setStudent(student1);
        asig1.setSubject(subjectIntermedio);
        asig1.setPeriod(period);
        asig1.setCenter(center);
        asig1.setStatus(AssignmentStatus.ACTIVA);

        asig2 = new PracticeAssignment();
        asig2.setId(2L);
        asig2.setStudent(student2);
        asig2.setSubject(subjectIntermedio);
        asig2.setPeriod(period);
        asig2.setCenter(center);
        asig2.setStatus(AssignmentStatus.ACTIVA);
    }

    @Test
    @DisplayName("Crear dupla válida en Ciclo Intermedio retorna DTO y persiste")
    void testCrearDuplaValida_CicloIntermedio_RetornaDTO() {
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 2L, 1L);

        when(practiceAssignmentRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(practiceAssignmentRepository.findById(2L)).thenReturn(Optional.of(asig2));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(studentPairRepository.existsByAssignmentIdAndPeriodId(1L, 1L)).thenReturn(false);
        when(studentPairRepository.existsByStudentIdAndPeriodId(10L, 1L)).thenReturn(false);
        when(studentPairRepository.existsByAssignmentIdAndPeriodId(2L, 1L)).thenReturn(false);
        when(studentPairRepository.existsByStudentIdAndPeriodId(20L, 1L)).thenReturn(false);

        StudentPair savedPair = new StudentPair();
        savedPair.setId(50L);
        savedPair.setAssignment1(asig1);
        savedPair.setAssignment2(asig2);
        savedPair.setPeriod(period);
        savedPair.setCreatedAt(LocalDateTime.now());
        when(studentPairRepository.save(any(StudentPair.class))).thenReturn(savedPair);

        StudentPairDTO result = studentPairService.crearDupla(req);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals(1L, result.getAssignment1Id());
        assertEquals(2L, result.getAssignment2Id());
        assertEquals(1L, result.getPeriodId());
        assertEquals("Estudiante Uno", result.getStudent1Name());
        assertEquals("Estudiante Dos", result.getStudent2Name());
        verify(studentPairRepository, times(1)).save(any(StudentPair.class));
    }

    @Test
    @DisplayName("Auto-emparejamiento con el mismo assignmentId lanza BadRequestException")
    void testCrearDupla_AutoEmparejamientoAsignacion_LanzaBadRequestException() {
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 1L, 1L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(req));
        assertTrue(ex.getMessage().contains("consigo misma"));
        verify(studentPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mismo estudiante con distintas asignaciones lanza BadRequestException")
    void testCrearDupla_MismoEstudianteDistintasAsignaciones_LanzaBadRequestException() {
        asig2.setStudent(student1); // Mismo estudiante en ambas asignaciones
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 2L, 1L);

        when(practiceAssignmentRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(practiceAssignmentRepository.findById(2L)).thenReturn(Optional.of(asig2));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(req));
        assertTrue(ex.getMessage().contains("mismo estudiante"));
        verify(studentPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asignación con ciclo no intermedio (INICIAL) lanza BadRequestException")
    void testCrearDupla_CicloNoIntermedio_LanzaBadRequestException() {
        asig1.setSubject(subjectInicial);
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 2L, 1L);

        when(practiceAssignmentRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(practiceAssignmentRepository.findById(2L)).thenReturn(Optional.of(asig2));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(req));
        assertTrue(ex.getMessage().contains("Intermedio"));
        verify(studentPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asignación no activa lanza BadRequestException")
    void testCrearDupla_AsignacionInactiva_LanzaBadRequestException() {
        asig2.setStatus(AssignmentStatus.FINALIZADA);
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 2L, 1L);

        when(practiceAssignmentRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(practiceAssignmentRepository.findById(2L)).thenReturn(Optional.of(asig2));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(req));
        assertTrue(ex.getMessage().contains("activa"));
        verify(studentPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("Asignación en período diferente al especificado lanza BadRequestException")
    void testCrearDupla_PeriodoDiferente_LanzaBadRequestException() {
        AcademicPeriod otherPeriod = new AcademicPeriod();
        otherPeriod.setId(99L);
        asig2.setPeriod(otherPeriod);
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 2L, 1L);

        when(practiceAssignmentRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(practiceAssignmentRepository.findById(2L)).thenReturn(Optional.of(asig2));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(req));
        assertTrue(ex.getMessage().contains("período"));
        verify(studentPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("Estudiante ya emparejado en el período lanza BadRequestException")
    void testCrearDupla_EstudianteYaEmparejado_LanzaBadRequestException() {
        StudentPairCreateRequest req = new StudentPairCreateRequest(1L, 2L, 1L);

        when(practiceAssignmentRepository.findById(1L)).thenReturn(Optional.of(asig1));
        when(practiceAssignmentRepository.findById(2L)).thenReturn(Optional.of(asig2));
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(studentPairRepository.existsByAssignmentIdAndPeriodId(1L, 1L)).thenReturn(false);
        when(studentPairRepository.existsByStudentIdAndPeriodId(10L, 1L)).thenReturn(true); // Ya tiene dupla

        BadRequestException ex = assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(req));
        assertTrue(ex.getMessage().contains("ya tiene una dupla asignada"));
        verify(studentPairRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDs negativos o nulos lanzan BadRequestException")
    void testCrearDupla_IDsInvalidos_LanzaBadRequestException() {
        StudentPairCreateRequest reqNeg = new StudentPairCreateRequest(-1L, 2L, 1L);
        assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(reqNeg));

        StudentPairCreateRequest reqNull = new StudentPairCreateRequest(null, 2L, 1L);
        assertThrows(BadRequestException.class, () -> studentPairService.crearDupla(reqNull));
    }

    @Test
    @DisplayName("Asignación inexistente lanza ResourceNotFoundException")
    void testCrearDupla_AsignacionInexistente_LanzaResourceNotFoundException() {
        StudentPairCreateRequest req = new StudentPairCreateRequest(999L, 2L, 1L);
        when(practiceAssignmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentPairService.crearDupla(req));
    }

    @Test
    @DisplayName("Listar duplas por período retorna lista mapeada")
    void testListarDuplasPorPeriodo_RetornaLista() {
        StudentPair pair = new StudentPair();
        pair.setId(10L);
        pair.setAssignment1(asig1);
        pair.setAssignment2(asig2);
        pair.setPeriod(period);

        when(studentPairRepository.findByPeriodId(1L)).thenReturn(List.of(pair));

        List<StudentPairDTO> list = studentPairService.listarDuplasPorPeriodo(1L);
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getId());
    }

    @Test
    @DisplayName("Listar disponibles retorna asignaciones no emparejadas de ciclo intermedio")
    void testListarAsignacionesDisponibles_RetornaLista() {
        when(academicPeriodRepository.existsById(1L)).thenReturn(true);
        when(studentPairRepository.findAvailableUnpairedIntermediateAssignments(1L)).thenReturn(List.of(asig1, asig2));

        List<StudentPairAssignmentSummaryDTO> list = studentPairService.listarAsignacionesDisponibles(1L);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getId());
        assertEquals("INTERMEDIO", list.get(0).getCycle());
    }

    @Test
    @DisplayName("Obtener dupla por ID existente retorna DTO")
    void testObtenerDuplaPorId_Existe_RetornaDTO() {
        StudentPair pair = new StudentPair();
        pair.setId(10L);
        pair.setAssignment1(asig1);
        pair.setAssignment2(asig2);
        pair.setPeriod(period);

        when(studentPairRepository.findById(10L)).thenReturn(Optional.of(pair));

        StudentPairDTO dto = studentPairService.obtenerDuplaPorId(10L);
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
    }

    @Test
    @DisplayName("Obtener dupla por ID inexistente lanza ResourceNotFoundException")
    void testObtenerDuplaPorId_Inexistente_LanzaResourceNotFoundException() {
        when(studentPairRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentPairService.obtenerDuplaPorId(999L));
    }
}
