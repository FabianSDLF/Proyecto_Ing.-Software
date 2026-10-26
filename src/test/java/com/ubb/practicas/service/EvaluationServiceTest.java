package com.ubb.practicas.service;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationCriteriaDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceConflictException;
import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AchievementLevel;
import com.ubb.practicas.model.enums.EvaluationType;
import com.ubb.practicas.repository.EvaluationCriteriaRepository;
import com.ubb.practicas.repository.EvaluationRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU46 - EvaluationService Unit Tests (Finalización, Firma Digital e Inmutabilidad)")
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Mock
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private User evaluator;
    private PracticeAssignment assignment;

    @BeforeEach
    void setUp() {
        evaluator = new User();
        evaluator.setId(5L);
        evaluator.setEmail("colaborador@ubb.cl");
        evaluator.setFullName("Prof. Colaborador UBB");

        assignment = new PracticeAssignment();
        assignment.setId(10L);
    }

    @Test
    @DisplayName("Permite enviar evaluación finalizada y firmada digitalmente quedando oficial")
    void testCrearEvaluacionFinalizadaYFirmada() {
        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L);
        dto.setType(EvaluationType.PAUTA_OBSERVACION_COLABORADOR);
        dto.setScore(6.5f);
        dto.setQualitativeComments("Excelente desempeño pedagógico en aula.");
        dto.setIsFinalized(true);
        dto.setDigitalSignature("Prof. Colaborador UBB");

        EvaluationCriteriaDTO crit = new EvaluationCriteriaDTO();
        crit.setCriteriaName("Dominio del Grupo");
        crit.setAchievementLevel(AchievementLevel.LOGRO_ALTO);
        crit.setPoints(25.0f);
        crit.setJustification("Adecuado manejo");
        dto.setCriteria(List.of(crit));

        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(evaluator);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(evaluationRepository.findByPracticeAssignmentIdAndType(10L, EvaluationType.PAUTA_OBSERVACION_COLABORADOR))
                .thenReturn(Optional.empty());
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation ev = inv.getArgument(0);
            ev.setId(100L);
            return ev;
        });

        Evaluation result = evaluationService.crearEvaluacion(dto, "colaborador@ubb.cl");

        assertNotNull(result);
        assertTrue(result.getIsFinalized());
        assertEquals("Prof. Colaborador UBB", result.getDigitalSignature());
        assertEquals(6.5f, result.getScore());
        verify(evaluationRepository, times(1)).save(any(Evaluation.class));
        verify(evaluationCriteriaRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Impide modificar una evaluación que ya fue finalizada y firmada digitalmente (inamovible)")
    void testEvaluacionFinalizadaEsInamovible() {
        Evaluation existingFinalized = new Evaluation();
        existingFinalized.setId(99L);
        existingFinalized.setPracticeAssignment(assignment);
        existingFinalized.setType(EvaluationType.PAUTA_OBSERVACION_COLABORADOR);
        existingFinalized.setScore(6.8f);
        existingFinalized.setIsFinalized(true);
        existingFinalized.setDigitalSignature("Prof. Colaborador UBB");

        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L);
        dto.setType(EvaluationType.PAUTA_OBSERVACION_COLABORADOR);
        dto.setScore(4.0f);
        dto.setQualitativeComments("Intento de cambio posterior");
        dto.setIsFinalized(true);
        dto.setDigitalSignature("Otra Firma");

        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(evaluator);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(evaluationRepository.findByPracticeAssignmentIdAndType(10L, EvaluationType.PAUTA_OBSERVACION_COLABORADOR))
                .thenReturn(Optional.of(existingFinalized));

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> evaluationService.crearEvaluacion(dto, "colaborador@ubb.cl")
        );

        assertTrue(ex.getMessage().contains("inamovible"));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exige firma digital obligatoria si se marca como finalizada")
    void testExigeFirmaDigitalAlFinalizar() {
        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L);
        dto.setType(EvaluationType.PAUTA_OBSERVACION_COLABORADOR);
        dto.setScore(6.0f);
        dto.setQualitativeComments("Comentarios");
        dto.setIsFinalized(true);
        dto.setDigitalSignature("   ");

        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(evaluator);

        assertThrows(
                BadRequestException.class,
                () -> evaluationService.crearEvaluacion(dto, "colaborador@ubb.cl")
        );
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Permite actualizar un borrador no finalizado y luego finalizarlo con firma digital")
    void testActualizarBorradorYFinalizar() {
        Evaluation existingDraft = new Evaluation();
        existingDraft.setId(50L);
        existingDraft.setPracticeAssignment(assignment);
        existingDraft.setType(EvaluationType.PAUTA_OBSERVACION_COLABORADOR);
        existingDraft.setScore(5.5f);
        existingDraft.setIsFinalized(false);

        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L);
        dto.setType(EvaluationType.PAUTA_OBSERVACION_COLABORADOR);
        dto.setScore(6.2f);
        dto.setQualitativeComments("Observación definitiva");
        dto.setIsFinalized(true);
        dto.setDigitalSignature("Prof. Colaborador UBB");

        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(evaluator);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(evaluationRepository.findByPracticeAssignmentIdAndType(10L, EvaluationType.PAUTA_OBSERVACION_COLABORADOR))
                .thenReturn(Optional.of(existingDraft));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        Evaluation updated = evaluationService.crearEvaluacion(dto, "colaborador@ubb.cl");

        assertEquals(50L, updated.getId());
        assertTrue(updated.getIsFinalized());
        assertEquals(6.2f, updated.getScore());
        assertEquals("Prof. Colaborador UBB", updated.getDigitalSignature());
        verify(evaluationCriteriaRepository, times(1)).deleteByEvaluationId(50L);
    }
}
