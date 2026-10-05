package com.ubb.practicas.integration;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import com.ubb.practicas.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 4-Tier E2E Opaque-Box Test Suite for HU30 - Historial de Versiones de Planificaciones Didácticas.
 * Covers:
 * Tier 1: Feature Coverage (>=5 tests: v1 initial, v+1 increment, v1 immutability, detail read-only, history list)
 * Tier 2: Boundary & Corner Cases (>=5 tests: draft in-place, multi-generation v3, file attachments, permissions)
 * Tier 3: Cross-Feature Combinations
 * Tier 4: Real-World Application Scenarios
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("HU30 E2E Integration Suite: Historial de Versiones de Planificaciones")
public class LessonPlanVersioningE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LessonPlanRepository lessonPlanRepository;

    @Autowired
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PracticeCenterRepository practiceCenterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StorageService storageService;

    private User student1;
    private User student2;
    private AcademicPeriod activePeriod;
    private Subject subject;
    private PracticeCenter center;
    private PracticeAssignment assignment1;
    private PracticeAssignment assignment2;

    @BeforeEach
    void setUp() {
        cleanupData();

        Role studentRole = roleRepository.findByName("Estudiante");
        if (studentRole == null) {
            studentRole = roleRepository.save(new Role(null, "Estudiante"));
        }

        student1 = new User();
        student1.setEmail("estudiante.plan1@ubb.cl");
        student1.setFullName("Estudiante Planificador Uno");
        student1.setPasswordHash("pass123");
        student1.setActive(true);
        student1.setRole(studentRole);
        student1 = userRepository.save(student1);

        student2 = new User();
        student2.setEmail("estudiante.plan2@ubb.cl");
        student2.setFullName("Estudiante Planificador Dos");
        student2.setPasswordHash("pass123");
        student2.setActive(true);
        student2.setRole(studentRole);
        student2 = userRepository.save(student2);

        activePeriod = new AcademicPeriod();
        activePeriod.setName("Periodo Planificaciones 2026-1");
        activePeriod.setYear(2026);
        activePeriod.setSemesterNumber(1);
        activePeriod.setStartDate(LocalDate.of(2026, 3, 1));
        activePeriod.setEndDate(LocalDate.of(2026, 7, 31));
        activePeriod.setStatus(PeriodStatus.ACTIVO);
        activePeriod = academicPeriodRepository.save(activePeriod);

        center = new PracticeCenter();
        center.setName("Liceo Marta Brunet");
        center.setCity("Chillán");
        center = practiceCenterRepository.save(center);

        subject = new Subject();
        subject.setName("Didáctica de la Geometría");
        subject.setCode("MAT-204");
        subject.setCycle(PracticeCycle.INTERMEDIO);
        subject = subjectRepository.save(subject);

        assignment1 = new PracticeAssignment();
        assignment1.setStudent(student1);
        assignment1.setSubject(subject);
        assignment1.setCenter(center);
        assignment1.setPeriod(activePeriod);
        assignment1.setStatus(AssignmentStatus.ACTIVA);
        assignment1.setCreatedAt(LocalDateTime.now());
        assignment1 = practiceAssignmentRepository.save(assignment1);

        assignment2 = new PracticeAssignment();
        assignment2.setStudent(student2);
        assignment2.setSubject(subject);
        assignment2.setCenter(center);
        assignment2.setPeriod(activePeriod);
        assignment2.setStatus(AssignmentStatus.ACTIVA);
        assignment2.setCreatedAt(LocalDateTime.now());
        assignment2 = practiceAssignmentRepository.save(assignment2);
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        lessonPlanRepository.deleteAll();
        portfolioDocumentRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
        subjectRepository.deleteAll();
        practiceCenterRepository.deleteAll();
        academicPeriodRepository.deleteAll();
        userRepository.deleteAll();
    }

    private LessonPlan createBasePlan(User student, PracticeAssignment assignment, PlanStatus status, Integer version, LessonPlan parent) {
        LessonPlan plan = new LessonPlan();
        plan.setPracticeAssignment(assignment);
        plan.setStatus(status);
        plan.setVersion(version != null ? version : 1);
        plan.setParentPlan(parent);
        plan.setSchoolGrade("7mo Básico");
        plan.setMathAxis(MathAxis.GEOMETRIA);
        plan.setBaseLearningObjective("OA 12: Construir figuras geométricas");
        plan.setObjectives("Identificar ángulos y polígonos regulares");
        plan.setContentJson("{\"inicio\":\"Motivación con Geogebra\",\"desarrollo\":\"Trazado de diagonales\",\"cierre\":\"Ticket de salida\"}");
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        return lessonPlanRepository.save(plan);
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Feature Coverage (Primary Behavior / Happy Path)")
    class Tier1FeatureCoverage {

        @Test
        @DisplayName("TC-HU30-T1-01: Creación inicial de planificación inicializa version=1 y parentPlan=null")
        void testCreateInitialLessonPlan_SetsVersion1AndNullParent() throws Exception {
            mockMvc.perform(multipart("/api/portafolio/planificaciones/upload")
                            .param("schoolGrade", "8vo Básico")
                            .param("mathAxis", "ALGEBRA")
                            .param("objective", "Resolver ecuaciones de primer grado")
                            .param("specificObjectives", "Plantear ecuaciones a partir de problemas")
                            .param("classActivities", "{\"inicio\":\"Repaso\",\"desarrollo\":\"Ejercicios\",\"cierre\":\"Evaluación\"}")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.version").value(1));

            List<LessonPlan> plans = lessonPlanRepository.findAll();
            assertEquals(1, plans.size());
            assertEquals(1, plans.get(0).getVersion());
            assertNull(plans.get(0).getParentPlan());
        }

        @Test
        @DisplayName("TC-HU30-T1-02: Editar planificación enviada (no borrador) crea nuevo registro con version v+1 y parentPlan")
        void testEditSubmittedLessonPlan_CreatesNewVersionV2() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.EN_REVISION_DOCENTE, 1, null);

            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v1.getId() + "/editar")
                            .param("schoolGrade", "7mo Básico")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Objetivo Mejorado para V2")
                            .param("specificObjectives", "Nuevos objetivos específicos")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.version").value(2))
                    .andExpect(jsonPath("$.parentPlanId").value(v1.getId()));

            List<LessonPlan> allPlans = lessonPlanRepository.findAll();
            assertEquals(2, allPlans.size());
        }

        @Test
        @DisplayName("TC-HU30-T1-03: Editar planificación enviada preserva la versión v1 completamente intacta en BD")
        void testEditSubmittedLessonPlan_MaintainsV1CompletelyIntact() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.EN_REVISION_DOCENTE, 1, null);
            String originalObjective = v1.getBaseLearningObjective();
            PlanStatus originalStatus = v1.getStatus();

            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v1.getId() + "/editar")
                            .param("schoolGrade", "7mo Básico - Editado")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Nuevo objetivo v2")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            LessonPlan refreshedV1 = lessonPlanRepository.findById(v1.getId()).orElseThrow();
            assertEquals(1, refreshedV1.getVersion());
            assertEquals(originalStatus, refreshedV1.getStatus());
            assertEquals(originalObjective, refreshedV1.getBaseLearningObjective());
            assertNull(refreshedV1.getParentPlan());
        }

        @Test
        @DisplayName("TC-HU30-T1-04: Consultar planificación por ID retorna detalle completo para modo solo lectura")
        void testGetPlanById_ReturnsPlanDetailsForReadOnlyView() throws Exception {
            LessonPlan plan = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);

            mockMvc.perform(get("/api/portafolio/planificaciones/" + plan.getId())
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(plan.getId()))
                    .andExpect(jsonPath("$.schoolGrade").value("7mo Básico"))
                    .andExpect(jsonPath("$.mathAxis").value("GEOMETRIA"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("TC-HU30-T1-05: Consultar historial de versiones retorna el linaje cronológico de versiones")
        void testGetVersionHistory_ReturnsAllVersionsInOrder() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);
            LessonPlan v2 = createBasePlan(student1, assignment1, PlanStatus.EN_REVISION_DOCENTE, 2, v1);

            mockMvc.perform(get("/api/portafolio/planificaciones/" + v2.getId() + "/historial")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].version").value(1))
                    .andExpect(jsonPath("$[1].version").value(2));
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2 - Boundary & Corner Cases")
    class Tier2BoundaryCornerCases {

        @Test
        @DisplayName("TC-HU30-T2-01: Editar planificación en estado BORRADOR actualiza in-situ sin incrementar versión")
        void testEditDraftPlan_UpdatesInPlaceWithoutIncrement() throws Exception {
            LessonPlan borrador = createBasePlan(student1, assignment1, PlanStatus.BORRADOR, 1, null);

            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + borrador.getId() + "/editar")
                            .param("schoolGrade", "8vo Básico - Actualizado")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Objetivo modificado en borrador")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.version").value(1))
                    .andExpect(jsonPath("$.id").value(borrador.getId()));

            // No se debe haber creado un nuevo registro
            assertEquals(1, lessonPlanRepository.count());
        }

        @Test
        @DisplayName("TC-HU30-T2-02: Versionado multigeneracional sucesivo: v1 -> v2 -> v3")
        void testMultiGenerationVersioning_V1toV2toV3() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);
            LessonPlan v2 = createBasePlan(student1, assignment1, PlanStatus.REQUIERE_CAMBIOS, 2, v1);

            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v2.getId() + "/editar")
                            .param("schoolGrade", "7mo Básico")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Objetivo corregido tercera entrega")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.version").value(3))
                    .andExpect(jsonPath("$.parentPlanId").value(v2.getId()));

            assertEquals(3, lessonPlanRepository.count());
        }

        @Test
        @DisplayName("TC-HU30-T2-03: Inmutabilidad histórica: intento de editar una versión histórica antigua crea nueva rama o versión superior")
        void testHistoricalVersionImmutability_DirectTamperingAttemptProtected() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);
            createBasePlan(student1, assignment1, PlanStatus.BORRADOR, 2, v1);

            // Intento de llamar editar sobre v1 (que ya tiene un hijo v2)
            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v1.getId() + "/editar")
                            .param("schoolGrade", "Intento Alterar Pasado")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "No debe sobreescribir v1")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            // v1 debe seguir intacto
            LessonPlan v1Check = lessonPlanRepository.findById(v1.getId()).orElseThrow();
            assertEquals("7mo Básico", v1Check.getSchoolGrade());
            assertEquals(1, v1Check.getVersion());
        }

        @Test
        @DisplayName("TC-HU30-T2-04: Editar plan adjuntando nuevo archivo preserva el archivo original de v1")
        void testEditPlan_WithNewFileAttachment_PreservesV1File() throws Exception {
            // Documento de v1
            PortfolioDocument docV1 = new PortfolioDocument();
            docV1.setPracticeAssignment(assignment1);
            docV1.setType(DocumentType.PLANIFICACION);
            docV1.setFileName("plan_v1.pdf");
            docV1.setStoredFileName("uuid-v1.pdf");
            docV1.setUploadDate(LocalDateTime.now());
            docV1 = portfolioDocumentRepository.save(docV1);

            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);
            v1.setDocument(docV1);
            v1 = lessonPlanRepository.save(v1);

            MockMultipartFile newFile = new MockMultipartFile(
                    "file", "plan_v2.pdf", "application/pdf", "contenido v2".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v1.getId() + "/editar")
                            .file(newFile)
                            .param("schoolGrade", "7mo Básico")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Con nuevo archivo")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            LessonPlan v1After = lessonPlanRepository.findById(v1.getId()).orElseThrow();
            assertNotNull(v1After.getDocument());
            assertEquals("plan_v1.pdf", v1After.getDocument().getFileName());
        }

        @Test
        @DisplayName("TC-HU30-T2-05: Rechazar edición no autorizada por otro estudiante (403 Forbidden)")
        void testUnauthorizedEdit_ThrowsAccessDenied() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);

            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v1.getId() + "/editar")
                            .param("schoolGrade", "Intruso")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Hack")
                            .principal(() -> student2.getEmail()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TC-HU30-T2-06: Endpoint de historial devuelve versiones ordenadas ascendentemente por version")
        void testHistoryEndpoint_ReturnsStrictlyOrderedByVersion() throws Exception {
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);
            LessonPlan v2 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 2, v1);
            LessonPlan v3 = createBasePlan(student1, assignment1, PlanStatus.BORRADOR, 3, v2);

            mockMvc.perform(get("/api/portafolio/planificaciones/" + v3.getId() + "/historial")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].version").value(1))
                    .andExpect(jsonPath("$[1].version").value(2))
                    .andExpect(jsonPath("$[2].version").value(3));
        }
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Nested
    @DisplayName("Tier 3 - Cross-Feature Combinations")
    class Tier3CrossFeatureCombinations {

        @Test
        @DisplayName("TC-HU30-T3-01: Estudiante en dupla puede versionar sus planificaciones sin alterar las de su pareja")
        void testLessonPlanVersionWithStudentInDupla() throws Exception {
            // Estudiante 1 tiene v1 y v2
            LessonPlan planEst1_v1 = createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);
            createBasePlan(student1, assignment1, PlanStatus.BORRADOR, 2, planEst1_v1);

            // Estudiante 2 tiene su propia planificación v1
            LessonPlan planEst2_v1 = createBasePlan(student2, assignment2, PlanStatus.APROBADO, 1, null);

            // Consultar historial de estudiante 1 debe contener solo sus versiones
            mockMvc.perform(get("/api/portafolio/planificaciones/" + planEst1_v1.getId() + "/historial")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].practiceAssignment.id", everyItem(is(assignment1.getId().intValue()))));

            // Plan de estudiante 2 sigue en version 1
            assertEquals(1, planEst2_v1.getVersion());
        }

        @Test
        @DisplayName("TC-HU30-T3-02: Las versiones de planificaciones coexisten con documentos finales de término")
        void testLessonPlanVersioningCoexistsWithFinalDocument() throws Exception {
            createBasePlan(student1, assignment1, PlanStatus.APROBADO, 1, null);

            PortfolioDocument docFinal = new PortfolioDocument();
            docFinal.setPracticeAssignment(assignment1);
            docFinal.setType(DocumentType.INFORME_FINAL);
            docFinal.setFileName("informe_final.pdf");
            docFinal.setStoredFileName("uuid-doc.pdf");
            portfolioDocumentRepository.save(docFinal);

            mockMvc.perform(get("/api/portafolio/planificaciones")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Tier 4 - Real-World Application Scenarios")
    class Tier4RealWorldScenarios {

        @Test
        @DisplayName("TC-HU30-T4-01: Flujo completo: Borrador inicial -> Envío -> Retroalimentación -> Edición v2 -> Consulta de Historial")
        void testScenario_FullStudentPlanWorkflow_Draft_Submit_Review_Version2_History() throws Exception {
            // 1. Estudiante crea plan inicial (v1)
            LessonPlan v1 = createBasePlan(student1, assignment1, PlanStatus.EN_REVISION_DOCENTE, 1, null);

            // 2. Docente proporciona retroalimentación (REQUIERE_CAMBIOS)
            v1.setStatus(PlanStatus.REQUIERE_CAMBIOS);
            v1.setTeacherFeedback("Por favor ajustar actividades de inicio e incorporar material concreto.");
            lessonPlanRepository.save(v1);

            // 3. Estudiante edita desde la interfaz para subsanar observaciones -> Genera v2
            mockMvc.perform(multipart("/api/portafolio/planificaciones/" + v1.getId() + "/editar")
                            .param("schoolGrade", "7mo Básico")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Objetivo adaptado con material concreto")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.version").value(2));

            // 4. Estudiante abre el historial para ver sus versiones
            mockMvc.perform(get("/api/portafolio/planificaciones/" + v1.getId() + "/historial")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].version").value(1))
                    .andExpect(jsonPath("$[1].version").value(2));

            // 5. Estudiante abre la versión 1 en modo solo lectura para contrastar
            mockMvc.perform(get("/api/portafolio/planificaciones/" + v1.getId())
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(v1.getId()))
                    .andExpect(jsonPath("$.teacherFeedback").value("Por favor ajustar actividades de inicio e incorporar material concreto."));
        }

        @Test
        @DisplayName("TC-HU30-T4-02: Resistencia adversarial: rechazar referencias a parentPlan inexistente")
        void testScenario_AdversarialNonExistentParent() throws Exception {
            mockMvc.perform(multipart("/api/portafolio/planificaciones/999999/editar")
                            .param("schoolGrade", "Inexistente")
                            .param("mathAxis", "GEOMETRIA")
                            .param("objective", "Fantasma")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is4xxClientError());
        }
    }
}
