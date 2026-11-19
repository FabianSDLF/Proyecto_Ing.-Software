package com.ubb.practicas.service;

import tools.jackson.databind.ObjectMapper;
import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationCriteriaDTO;
import com.ubb.practicas.dto.EvaluationFinalizeDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceConflictException;
import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.model.EvaluationCriteria;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.EvaluationType;
import com.ubb.practicas.repository.EvaluationCriteriaRepository;
import com.ubb.practicas.repository.EvaluationRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.EvaluationRubricCatalog.IndicatorDefinition;
import com.ubb.practicas.service.EvaluationRubricCatalog.RubricDefinition;
import com.ubb.practicas.service.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HU43, HU46 y HU47 - Pautas del Tutor, Cierre/Firma Oficial y Consulta Estudiantil")
class EvaluationServiceTest {

    @Test
    void hu44RejectsProfessionalRubricForUnconfiguredSubject() {
        assignment.getSubject().setProfessionalRubrics(false);
        when(userRepository.findByEmail(tutor.getEmail())).thenReturn(tutor);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L); dto.setType(EvaluationType.TUTOR_ESP_1);
        assertThrows(BadRequestException.class, () -> evaluationService.crearEvaluacion(dto, tutor.getEmail()));
        verifyNoInteractions(evaluationRepository, evaluationCriteriaRepository);
    }

    @Test
    void hu44RejectsRepeatedCodeEvenWhenFirstValueIsPending() {
        when(userRepository.findByEmail(tutor.getEmail())).thenReturn(tutor);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L); dto.setType(EvaluationType.TUTOR_ESP_1);
        var pending = new EvaluationCriteriaDTO(); pending.setCriterionCode("A1");
        var observed = new EvaluationCriteriaDTO(); observed.setCriterionCode("A1"); observed.setPoints(5f);
        dto.setCriteria(List.of(pending, observed));
        assertThrows(BadRequestException.class, () -> evaluationService.crearEvaluacion(dto, tutor.getEmail()));
        verify(evaluationRepository, never()).save(any());
    }

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Mock
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private EvaluationRubricCatalog rubricCatalog;
    private EvaluationServiceImpl evaluationService;

    private User collaborator;
    private User tutor;
    private User otherTutor;
    private User professor;
    private User student;
    private User otherStudent;
    private PracticeAssignment assignment;

    @BeforeEach
    void setUp() {
        AssignmentScope scope = new AssignmentScope(practiceAssignmentRepository, userRepository);
        rubricCatalog = new EvaluationRubricCatalog(new ObjectMapper(), scope, practiceAssignmentRepository);
        rubricCatalog.init();

        evaluationService = new EvaluationServiceImpl(
                evaluationRepository,
                evaluationCriteriaRepository,
                practiceAssignmentRepository,
                userRepository,
                rubricCatalog,
                auditService,
                new ObjectMapper()
        );

        Role colabRole = new Role(1L, "Profesor Colaborador");
        collaborator = new User();
        collaborator.setId(5L);
        collaborator.setRole(colabRole);
        collaborator.setEmail("colaborador@ubb.cl");
        collaborator.setFullName("Prof. Colaborador UBB");
        collaborator.setActive(true);

        Role tutorRole = new Role(2L, "Tutor");
        tutor = new User();
        tutor.setId(6L);
        tutor.setRole(tutorRole);
        tutor.setEmail("tutor@ubb.cl");
        tutor.setFullName("Mg. Claudia Arriagada");
        tutor.setActive(true);

        otherTutor = new User();
        otherTutor.setId(7L);
        otherTutor.setRole(tutorRole);
        otherTutor.setEmail("otro.tutor@ubb.cl");
        otherTutor.setFullName("Otro Tutor");
        otherTutor.setActive(true);

        Role profRole = new Role(3L, "Profesor de Asignatura");
        professor = new User();
        professor.setId(8L);
        professor.setRole(profRole);
        professor.setEmail("profesor@ubb.cl");
        professor.setFullName("Dr. Gonzalo Henríquez");
        professor.setActive(true);

        Role studentRole = new Role(4L, "Estudiante");
        student = new User();
        student.setId(9L);
        student.setRole(studentRole);
        student.setEmail("estudiante@ubb.cl");
        student.setFullName("Valentina Castro");
        student.setActive(true);

        otherStudent = new User();
        otherStudent.setId(10L);
        otherStudent.setRole(studentRole);
        otherStudent.setEmail("otro.estudiante@ubb.cl");
        otherStudent.setFullName("Otro Estudiante");
        otherStudent.setActive(true);

        Subject subject = new Subject();
        subject.setId(20L);
        subject.setName("Práctica Profesional");
        subject.setProfessionalRubrics(true);
        subject.setProfessor(professor);

        assignment = new PracticeAssignment();
        assignment.setId(10L);
        assignment.setStudent(student);
        assignment.setCollaborator(collaborator);
        assignment.setTutor(tutor);
        assignment.setSubject(subject);
    }

    private List<EvaluationCriteriaDTO> buildCompleteCriteria(EvaluationType type, int pointsPerIndicator) {
        RubricDefinition rubric = rubricCatalog.findRubric(type).orElseThrow();
        return rubric.getIndicators().stream().map(ind -> {
            EvaluationCriteriaDTO c = new EvaluationCriteriaDTO();
            c.setCriterionCode(ind.getCriterionCode());
            c.setPoints((float) pointsPerIndicator);
            return c;
        }).toList();
    }

    private List<EvaluationCriteria> toPersistedCriteria(Evaluation evaluation, List<EvaluationCriteriaDTO> dtos, EvaluationType type) {
        RubricDefinition rubric = rubricCatalog.findRubric(type).orElseThrow();
        var map = rubric.indicatorMap();
        List<EvaluationCriteria> list = new ArrayList<>();
        long id = 1L;
        for (EvaluationCriteriaDTO dto : dtos) {
            IndicatorDefinition ind = map.get(dto.getCriterionCode());
            EvaluationCriteria ec = new EvaluationCriteria();
            ec.setId(id++);
            ec.setEvaluation(evaluation);
            ec.setCriterionCode(dto.getCriterionCode());
            ec.setDimension(ind != null ? ind.getDimension() : "A");
            ec.setDisplayOrder(ind != null ? ind.getOrder() : (int) id);
            ec.setCriteriaName(ind != null ? ind.getText() : dto.getCriterionCode());
            ec.setPoints(dto.getPoints());
            list.add(ec);
        }
        return list;
    }

    @Test
    @DisplayName("Catálogo y escala: reproduce exactamente las tablas oficiales 2,0-7,0 al 60% de exigencia")
    void testCatalogoYTablasConversionOficiales() {
        assertEquals(8, rubricCatalog.getAllProfessionalRubrics().size());
        assertEquals(4, rubricCatalog.getCollaboratorRubrics().size());
        assertEquals(4, rubricCatalog.getTutorRubrics().size());

        assertEquals(133, rubricCatalog.findRubric(EvaluationType.COLAB_ESP_1).orElseThrow().getMaxPoints());
        assertEquals(161, rubricCatalog.findRubric(EvaluationType.COLAB_ESP_2).orElseThrow().getMaxPoints());
        assertEquals(126, rubricCatalog.findRubric(EvaluationType.COLAB_ORI_1).orElseThrow().getMaxPoints());
        assertEquals(161, rubricCatalog.findRubric(EvaluationType.COLAB_ORI_2).orElseThrow().getMaxPoints());
        assertEquals(133, rubricCatalog.findRubric(EvaluationType.TUTOR_ESP_1).orElseThrow().getMaxPoints());
        assertEquals(161, rubricCatalog.findRubric(EvaluationType.TUTOR_ESP_2).orElseThrow().getMaxPoints());
        assertEquals(126, rubricCatalog.findRubric(EvaluationType.TUTOR_ORI_1).orElseThrow().getMaxPoints());
        assertEquals(161, rubricCatalog.findRubric(EvaluationType.TUTOR_ORI_2).orElseThrow().getMaxPoints());

        // Tabla M = 133 (19 indicadores)
        assertEquals(3.0f, EvaluationRubricCatalog.calculateOfficialGrade(38, 133));
        assertEquals(4.0f, EvaluationRubricCatalog.calculateOfficialGrade(78, 133));
        assertEquals(4.0f, EvaluationRubricCatalog.calculateOfficialGrade(80, 133));
        assertEquals(7.0f, EvaluationRubricCatalog.calculateOfficialGrade(133, 133));

        // Tabla M = 126 (18 indicadores)
        assertEquals(3.0f, EvaluationRubricCatalog.calculateOfficialGrade(36, 126));
        assertEquals(4.0f, EvaluationRubricCatalog.calculateOfficialGrade(74, 126));
        assertEquals(4.0f, EvaluationRubricCatalog.calculateOfficialGrade(76, 126));
        assertEquals(7.0f, EvaluationRubricCatalog.calculateOfficialGrade(126, 126));

        // Tabla M = 161 (23 indicadores)
        assertEquals(3.0f, EvaluationRubricCatalog.calculateOfficialGrade(46, 161));
        assertEquals(4.0f, EvaluationRubricCatalog.calculateOfficialGrade(95, 161));
        assertEquals(4.0f, EvaluationRubricCatalog.calculateOfficialGrade(97, 161));
        assertEquals(7.0f, EvaluationRubricCatalog.calculateOfficialGrade(161, 161));
    }

    @Test
    @DisplayName("HU43: El tutor asignado accede a sus cuatro pautas y un tutor ajeno o estudiante es rechazado")
    void testHu43AccesoCatalogoTutorYPermisos() {
        when(userRepository.findByEmail("tutor@ubb.cl")).thenReturn(tutor);
        when(userRepository.findByEmail("otro.tutor@ubb.cl")).thenReturn(otherTutor);
        when(userRepository.findByEmail("estudiante@ubb.cl")).thenReturn(student);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        List<RubricDefinition> tutorRubrics = rubricCatalog.getApplicableRubrics("tutor@ubb.cl", 10L);
        assertEquals(4, tutorRubrics.size());
        assertTrue(tutorRubrics.stream().allMatch(r -> r.getType().name().startsWith("TUTOR_")));
        assertTrue(tutorRubrics.stream().allMatch(r -> "2025-2-v1".equals(r.getRubricVersion())));

        assertThrows(AccessDeniedException.class, () -> rubricCatalog.getApplicableRubrics("otro.tutor@ubb.cl", 10L));
        assertThrows(AccessDeniedException.class, () -> rubricCatalog.getApplicableRubrics("estudiante@ubb.cl", 10L));
    }

    @Test
    @DisplayName("HU46: Pauta completa, evaluador asignado y revisión vigente produce cierre oficial, nota de servidor y huella SHA-256")
    void testCierreExitosoConNotaCalculadaEnServidor() {
        when(userRepository.findByEmail("tutor@ubb.cl")).thenReturn(tutor);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(evaluationRepository.findByPracticeAssignmentIdAndType(10L, EvaluationType.TUTOR_ESP_1))
                .thenReturn(Optional.empty());

        List<EvaluationCriteria> storedCriteria = new ArrayList<>();
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation ev = inv.getArgument(0);
            if (ev.getId() == null) ev.setId(100L);
            return ev;
        });
        when(evaluationCriteriaRepository.save(any(EvaluationCriteria.class))).thenAnswer(inv -> {
            EvaluationCriteria c = inv.getArgument(0);
            storedCriteria.add(c);
            return c;
        });

        // 1. Guardar borrador completo intentando imponer una nota falsa (1.5) desde el cliente
        EvaluationCreateDTO draftDto = new EvaluationCreateDTO();
        draftDto.setPracticeAssignmentId(10L);
        draftDto.setType(EvaluationType.TUTOR_ESP_1);
        draftDto.setRubricVersion("2025-2-v1");
        draftDto.setExpectedRevision(null);
        draftDto.setScore(1.5f); // El servidor debe ignorar este valor
        draftDto.setQualitativeComments("Desempeño sólido en la primera visita de especialidad.");
        draftDto.setCriteria(buildCompleteCriteria(EvaluationType.TUTOR_ESP_1, 7));

        Evaluation draft = evaluationService.crearEvaluacion(draftDto, "tutor@ubb.cl");
        assertNotNull(draft);
        assertFalse(draft.getIsFinalized());
        assertEquals(1L, draft.getRevision());
        assertEquals(133, draft.getTotalPoints());
        assertEquals(133, draft.getMaxPoints());
        assertEquals(7.0f, draft.getScore(), "El servidor debe calcular 7.0 e ignorar la nota enviada por el cliente");

        // 2. Finalizar y firmar atómicamente con la revisión vigente
        when(evaluationRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(evaluationCriteriaRepository.findByEvaluationId(100L)).thenReturn(storedCriteria);

        Evaluation finalized = evaluationService.finalizarEvaluacion(
                100L,
                new EvaluationFinalizeDTO(1L, true),
                "tutor@ubb.cl"
        );

        assertTrue(finalized.getIsFinalized());
        assertEquals(2L, finalized.getRevision());
        assertEquals(7.0f, finalized.getScore());
        assertEquals(tutor.getId(), finalized.getFinalizedById());
        assertEquals("tutor@ubb.cl", finalized.getFinalizedByEmail());
        assertEquals("Mg. Claudia Arriagada", finalized.getFinalizedByName());
        assertEquals("Tutor", finalized.getFinalizedByRole());
        assertNotNull(finalized.getFinalizedAt());
        assertNotNull(finalized.getSnapshotJson());
        assertNotNull(finalized.getSnapshotHash());
        assertEquals(64, finalized.getSnapshotHash().length());
        verify(auditService, times(1)).record(eq("tutor@ubb.cl"), eq("FINALIZAR_PUBLICAR_EVALUACION"), anyString(), eq(200), anyString());
    }

    @Test
    @DisplayName("HU46: Rechaza el antiguo POST /api/evaluaciones con isFinalized=true para impedir saltar validaciones")
    void testRechazaIsFinalizedEnEndpointDeBorrador() {
        EvaluationCreateDTO dto = new EvaluationCreateDTO();
        dto.setPracticeAssignmentId(10L);
        dto.setType(EvaluationType.COLAB_ESP_1);
        dto.setIsFinalized(true);

        assertThrows(BadRequestException.class, () -> evaluationService.crearEvaluacion(dto, "colaborador@ubb.cl"));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU46: Rechaza cierre si faltan indicadores, hay duplicados, indicadores ajenos o puntajes fuera de escala")
    void testRechazaIndicadoresInvalidosOIncompletos() {
        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(collaborator);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        // 1. Indicador ajeno en borrador
        EvaluationCreateDTO foreignDto = new EvaluationCreateDTO();
        foreignDto.setPracticeAssignmentId(10L);
        foreignDto.setType(EvaluationType.COLAB_ESP_1);
        EvaluationCriteriaDTO badCrit = new EvaluationCriteriaDTO();
        badCrit.setCriterionCode("Z99");
        badCrit.setPoints(5f);
        foreignDto.setCriteria(List.of(badCrit));
        assertThrows(BadRequestException.class, () -> evaluationService.crearEvaluacion(foreignDto, "colaborador@ubb.cl"));

        // 2. Puntaje fuera de escala (1 punto o 8 puntos o decimal)
        EvaluationCreateDTO outOfScaleDto = new EvaluationCreateDTO();
        outOfScaleDto.setPracticeAssignmentId(10L);
        outOfScaleDto.setType(EvaluationType.COLAB_ESP_1);
        EvaluationCriteriaDTO scaleCrit = new EvaluationCriteriaDTO();
        scaleCrit.setCriterionCode("A1");
        scaleCrit.setPoints(1f);
        outOfScaleDto.setCriteria(List.of(scaleCrit));
        assertThrows(BadRequestException.class, () -> evaluationService.crearEvaluacion(outOfScaleDto, "colaborador@ubb.cl"));

        // 3. Indicador duplicado
        EvaluationCreateDTO dupDto = new EvaluationCreateDTO();
        dupDto.setPracticeAssignmentId(10L);
        dupDto.setType(EvaluationType.COLAB_ESP_1);
        EvaluationCriteriaDTO c1 = new EvaluationCriteriaDTO();
        c1.setCriterionCode("A1");
        c1.setPoints(5f);
        EvaluationCriteriaDTO c2 = new EvaluationCriteriaDTO();
        c2.setCriterionCode("A1");
        c2.setPoints(6f);
        dupDto.setCriteria(List.of(c1, c2));
        assertThrows(BadRequestException.class, () -> evaluationService.crearEvaluacion(dupDto, "colaborador@ubb.cl"));

        // 4. Intentar finalizar un borrador parcial (solo 2 de 19 indicadores)
        Evaluation partialDraft = new Evaluation();
        partialDraft.setId(55L);
        partialDraft.setPracticeAssignment(assignment);
        partialDraft.setEvaluator(collaborator);
        partialDraft.setType(EvaluationType.COLAB_ESP_1);
        partialDraft.setRubricVersion("2025-2-v1");
        partialDraft.setRevision(1L);
        partialDraft.setIsFinalized(false);

        EvaluationCriteria savedA1 = new EvaluationCriteria();
        savedA1.setCriterionCode("A1");
        savedA1.setPoints(5f);

        when(evaluationRepository.findById(55L)).thenReturn(Optional.of(partialDraft));
        when(evaluationCriteriaRepository.findByEvaluationId(55L)).thenReturn(List.of(savedA1));

        assertThrows(
                BadRequestException.class,
                () -> evaluationService.finalizarEvaluacion(55L, new EvaluationFinalizeDTO(1L, true), "colaborador@ubb.cl")
        );
        assertFalse(partialDraft.getIsFinalized());
    }

    @Test
    @DisplayName("HU46: Exige confirmación explícita y revisión esperada al finalizar")
    void testExigeConfirmacionYRevisionAlFinalizar() {
        assertThrows(
                BadRequestException.class,
                () -> evaluationService.finalizarEvaluacion(100L, new EvaluationFinalizeDTO(1L, false), "colaborador@ubb.cl")
        );
        assertThrows(
                BadRequestException.class,
                () -> evaluationService.finalizarEvaluacion(100L, new EvaluationFinalizeDTO(null, true), "colaborador@ubb.cl")
        );
    }

    @Test
    @DisplayName("HU46: Concurrencia - Dos cierres simultáneos o guardado con revisión desactualizada producen 409 sin sobrescribir")
    void testConcurrenciaDosCierresYGuardadoSobreFinalizada() {
        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(collaborator);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        Evaluation draft = new Evaluation();
        draft.setId(77L);
        draft.setPracticeAssignment(assignment);
        draft.setEvaluator(collaborator);
        draft.setType(EvaluationType.COLAB_ORI_1);
        draft.setRubricVersion("2025-2-v1");
        draft.setRevision(3L);
        draft.setIsFinalized(false);

        List<EvaluationCriteria> fullCriteria = toPersistedCriteria(
                draft,
                buildCompleteCriteria(EvaluationType.COLAB_ORI_1, 2),
                EvaluationType.COLAB_ORI_1
        );

        when(evaluationRepository.findById(77L)).thenReturn(Optional.of(draft));
        when(evaluationCriteriaRepository.findByEvaluationId(77L)).thenReturn(fullCriteria);
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Revisión obsoleta (expectedRevision=2 cuando actual es 3) -> 409
        assertThrows(
                ResourceConflictException.class,
                () -> evaluationService.finalizarEvaluacion(77L, new EvaluationFinalizeDTO(2L, true), "colaborador@ubb.cl")
        );

        // Primer cierre con expectedRevision=3 -> éxito (36 puntos de 126 -> nota 3.0, revisión pasa a 4)
        Evaluation closed = evaluationService.finalizarEvaluacion(77L, new EvaluationFinalizeDTO(3L, true), "colaborador@ubb.cl");
        assertTrue(closed.getIsFinalized());
        assertEquals(3.0f, closed.getScore());
        assertEquals(4L, closed.getRevision());

        // Segundo cierre simultáneo con expectedRevision=3 o 4 -> 409 Conflict
        assertThrows(
                ResourceConflictException.class,
                () -> evaluationService.finalizarEvaluacion(77L, new EvaluationFinalizeDTO(3L, true), "colaborador@ubb.cl")
        );
        assertThrows(
                ResourceConflictException.class,
                () -> evaluationService.finalizarEvaluacion(77L, new EvaluationFinalizeDTO(4L, true), "colaborador@ubb.cl")
        );

        // Intento de guardar borrador sobre la evaluación ya finalizada -> 409 Conflict
        when(evaluationRepository.findByPracticeAssignmentIdAndType(10L, EvaluationType.COLAB_ORI_1))
                .thenReturn(Optional.of(closed));
        EvaluationCreateDTO overwriteAttempt = new EvaluationCreateDTO();
        overwriteAttempt.setPracticeAssignmentId(10L);
        overwriteAttempt.setType(EvaluationType.COLAB_ORI_1);
        overwriteAttempt.setExpectedRevision(4L);
        overwriteAttempt.setCriteria(buildCompleteCriteria(EvaluationType.COLAB_ORI_1, 7));

        assertThrows(
                ResourceConflictException.class,
                () -> evaluationService.crearEvaluacion(overwriteAttempt, "colaborador@ubb.cl")
        );
        assertEquals(3.0f, closed.getScore(), "La nota oficial no puede ser alterada tras el cierre");
    }

    @Test
    @DisplayName("HU46 y HU47: Inmutabilidad ante cambio posterior de plantilla y privacidad estricta de borradores")
    void testInstantaneaInmutableYPrivacidadDeBorradores() {
        when(userRepository.findByEmail("tutor@ubb.cl")).thenReturn(tutor);
        when(userRepository.findByEmail("profesor@ubb.cl")).thenReturn(professor);
        when(userRepository.findByEmail("colaborador@ubb.cl")).thenReturn(collaborator);
        when(userRepository.findByEmail("estudiante@ubb.cl")).thenReturn(student);
        when(userRepository.findByEmail("otro.estudiante@ubb.cl")).thenReturn(otherStudent);
        when(practiceAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        Evaluation draft = new Evaluation();
        draft.setId(88L);
        draft.setPracticeAssignment(assignment);
        draft.setEvaluator(tutor);
        draft.setType(EvaluationType.TUTOR_ORI_1);
        draft.setRubricVersion("2025-2-v1");
        draft.setRevision(1L);
        draft.setQualitativeComments("Borrador privado del tutor");
        draft.setIsFinalized(false);

        List<EvaluationCriteria> criteria = toPersistedCriteria(
                draft,
                buildCompleteCriteria(EvaluationType.TUTOR_ORI_1, 5),
                EvaluationType.TUTOR_ORI_1
        );

        when(evaluationRepository.findById(88L)).thenReturn(Optional.of(draft));
        when(evaluationRepository.findByPracticeAssignmentId(10L)).thenReturn(List.of(draft));
        when(evaluationCriteriaRepository.findByEvaluationId(88L)).thenReturn(criteria);

        // Mientras es borrador:
        // - El tutor propietario sí lo ve
        assertEquals(1, evaluationService.listarEvaluacionesPorAsignacion(10L, "tutor@ubb.cl").size());
        assertNotNull(evaluationService.obtenerEvaluacionPorId(88L, "tutor@ubb.cl"));

        // - El estudiante NO lo ve en el listado ni por ID directo
        assertTrue(evaluationService.listarEvaluacionesPorAsignacion(10L, "estudiante@ubb.cl").isEmpty());
        assertThrows(AccessDeniedException.class, () -> evaluationService.obtenerEvaluacionPorId(88L, "estudiante@ubb.cl"));

        // - El profesor de asignatura y el colaborador TAMPOCO pueden leer ni firmar el borrador privado del tutor
        assertTrue(evaluationService.listarEvaluacionesPorAsignacion(10L, "profesor@ubb.cl").isEmpty());
        assertThrows(AccessDeniedException.class, () -> evaluationService.obtenerEvaluacionPorId(88L, "profesor@ubb.cl"));
        assertThrows(AccessDeniedException.class, () -> evaluationService.obtenerEvaluacionPorId(88L, "colaborador@ubb.cl"));
        assertThrows(
                AccessDeniedException.class,
                () -> evaluationService.finalizarEvaluacion(88L, new EvaluationFinalizeDTO(1L, true), "colaborador@ubb.cl")
        );

        // Ahora el tutor firma y publica la evaluación
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));
        Evaluation published = evaluationService.finalizarEvaluacion(88L, new EvaluationFinalizeDTO(1L, true), "tutor@ubb.cl");
        String originalFirstIndicatorText = published.getCriteria().get(0).getText();

        // El estudiante propietario y el profesor de asignatura ya pueden consultarla
        assertEquals(1, evaluationService.listarEvaluacionesPorAsignacion(10L, "estudiante@ubb.cl").size());
        assertEquals(1, evaluationService.listarEvaluacionesPorAsignacion(10L, "profesor@ubb.cl").size());
        Evaluation studentView = evaluationService.obtenerEvaluacionPorId(88L, "estudiante@ubb.cl");
        assertEquals(published.getScore(), studentView.getScore());
        assertEquals(18, studentView.getCriteria().size());

        // Otro estudiante ajeno a la práctica sigue bloqueado (403)
        assertThrows(AccessDeniedException.class, () -> evaluationService.obtenerEvaluacionPorId(88L, "otro.estudiante@ubb.cl"));
        assertThrows(AccessDeniedException.class, () -> evaluationService.listarEvaluacionesPorAsignacion(10L, "otro.estudiante@ubb.cl"));

        // Si la plantilla futura cambia en el catálogo, la evaluación firmada conserva intacta su instantánea original
        RubricDefinition mutatedRubric = new RubricDefinition();
        mutatedRubric.setType(EvaluationType.TUTOR_ORI_1);
        mutatedRubric.setName("Plantilla Modificada 2027");
        mutatedRubric.setRubricVersion("2027-1-v2");
        mutatedRubric.setMaxPoints(70);
        mutatedRubric.setIndicatorCount(1);
        mutatedRubric.setIndicators(List.of(new IndicatorDefinition("A1", "A1", "A1", "A", 1, "TEXTO ALTERADO EN PLANTILLA FUTURA")));
        rubricCatalog.overrideRubricForTest(EvaluationType.TUTOR_ORI_1, mutatedRubric);

        Evaluation historicalRead = evaluationService.obtenerEvaluacionPorId(88L, "estudiante@ubb.cl");
        assertEquals("2025-2-v1", historicalRead.getRubricVersion());
        assertEquals(126, historicalRead.getMaxPoints());
        assertEquals(18, historicalRead.getCriteria().size());
        assertEquals(originalFirstIndicatorText, historicalRead.getCriteria().get(0).getText());
    }
}
