package com.ubb.practicas.integration;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.PeriodStatus;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
 * 4-Tier E2E Opaque-Box Test Suite for HU9 - Duplas de Estudiantes en Ciclo Intermedio.
 * Covers:
 * Tier 1: Feature Coverage (>=5 tests)
 * Tier 2: Boundary & Corner Cases (>=5 tests)
 * Tier 3: Cross-Feature Combinations
 * Tier 4: Real-World Application Scenarios
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(username = "admin@ubb.cl", roles = {"Administrador", "ADMINISTRADOR"})
@DisplayName("HU9 E2E Integration Suite: Duplas de Estudiantes en Ciclo Intermedio")
public class StudentPairE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentPairRepository studentPairRepository;

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

    private User adminUser;
    private User student1;
    private User student2;
    private User student3;
    private User student4;

    private AcademicPeriod activePeriod;
    private AcademicPeriod previousPeriod;

    private Subject subjectIntermedio1;
    private Subject subjectIntermedio2;
    private Subject subjectInicial;
    private Subject subjectAvanzado;

    private PracticeCenter center;

    private PracticeAssignment asigIntermedio1;
    private PracticeAssignment asigIntermedio2;
    private PracticeAssignment asigIntermedio3;
    private PracticeAssignment asigIntermedio4;
    private PracticeAssignment asigInicial;
    private PracticeAssignment asigAvanzado;
    private PracticeAssignment asigInactive;
    private PracticeAssignment asigPreviousPeriod;

    @BeforeEach
    void setUp() {
        cleanupData();

        Role adminRole = roleRepository.findByName("Coordinador de practicas");
        if (adminRole == null) {
            adminRole = roleRepository.findByName("Administrador");
        }
        if (adminRole == null) {
            adminRole = roleRepository.save(new Role(null, "Coordinador de practicas"));
        }

        Role studentRole = roleRepository.findByName("Estudiante");
        if (studentRole == null) {
            studentRole = roleRepository.save(new Role(null, "Estudiante"));
        }

        adminUser = new User();
        adminUser.setEmail("coordinador.duplas@ubb.cl");
        adminUser.setFullName("Coordinador Duplas Test");
        adminUser.setPasswordHash("secretHash");
        adminUser.setActive(true);
        adminUser.setRole(adminRole);
        adminUser = userRepository.save(adminUser);

        student1 = createUser("estudiante1.dupla@ubb.cl", "Estudiante Uno", studentRole);
        student2 = createUser("estudiante2.dupla@ubb.cl", "Estudiante Dos", studentRole);
        student3 = createUser("estudiante3.dupla@ubb.cl", "Estudiante Tres", studentRole);
        student4 = createUser("estudiante4.dupla@ubb.cl", "Estudiante Cuatro", studentRole);

        activePeriod = new AcademicPeriod();
        activePeriod.setName("Periodo 2026-1");
        activePeriod.setYear(2026);
        activePeriod.setSemesterNumber(1);
        activePeriod.setStartDate(LocalDate.of(2026, 3, 1));
        activePeriod.setEndDate(LocalDate.of(2026, 7, 31));
        activePeriod.setStatus(PeriodStatus.ACTIVO);
        activePeriod = academicPeriodRepository.save(activePeriod);

        previousPeriod = new AcademicPeriod();
        previousPeriod.setName("Periodo 2025-2");
        previousPeriod.setYear(2025);
        previousPeriod.setSemesterNumber(2);
        previousPeriod.setStartDate(LocalDate.of(2025, 8, 1));
        previousPeriod.setEndDate(LocalDate.of(2025, 12, 31));
        previousPeriod.setStatus(PeriodStatus.CERRADO);
        previousPeriod = academicPeriodRepository.save(previousPeriod);

        center = new PracticeCenter();
        center.setName("Colegio Bicentenario Concepción");
        center.setCity("Concepción");
        center = practiceCenterRepository.save(center);

        subjectIntermedio1 = createSubject("Práctica Pedagógica Intermedia I", "PPI-301", PracticeCycle.INTERMEDIO);
        subjectIntermedio2 = createSubject("Práctica Pedagógica Intermedia II", "PPI-302", PracticeCycle.INTERMEDIO);
        subjectInicial = createSubject("Práctica Inicial de Observación", "PIO-101", PracticeCycle.INICIAL);
        subjectAvanzado = createSubject("Práctica Profesional Avanzada", "PPA-501", PracticeCycle.AVANZADO);

        asigIntermedio1 = createAssignment(student1, subjectIntermedio1, activePeriod, AssignmentStatus.ACTIVA);
        asigIntermedio2 = createAssignment(student2, subjectIntermedio2, activePeriod, AssignmentStatus.ACTIVA);
        asigIntermedio3 = createAssignment(student3, subjectIntermedio1, activePeriod, AssignmentStatus.ACTIVA);
        asigIntermedio4 = createAssignment(student4, subjectIntermedio2, activePeriod, AssignmentStatus.ACTIVA);

        asigInicial = createAssignment(student1, subjectInicial, activePeriod, AssignmentStatus.ACTIVA);
        asigAvanzado = createAssignment(student2, subjectAvanzado, activePeriod, AssignmentStatus.ACTIVA);
        asigInactive = createAssignment(student3, subjectIntermedio1, activePeriod, AssignmentStatus.FINALIZADA);
        asigPreviousPeriod = createAssignment(student4, subjectIntermedio2, previousPeriod, AssignmentStatus.ACTIVA);
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        studentPairRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
        subjectRepository.deleteAll();
        practiceCenterRepository.deleteAll();
        academicPeriodRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash("hashedPass");
        u.setActive(true);
        u.setRole(role);
        return userRepository.save(u);
    }

    private Subject createSubject(String name, String code, PracticeCycle cycle) {
        Subject s = new Subject();
        s.setName(name);
        s.setCode(code);
        s.setCycle(cycle);
        return subjectRepository.save(s);
    }

    private PracticeAssignment createAssignment(User student, Subject subject, AcademicPeriod period, AssignmentStatus status) {
        PracticeAssignment pa = new PracticeAssignment();
        pa.setStudent(student);
        pa.setSubject(subject);
        pa.setCenter(center);
        pa.setPeriod(period);
        pa.setStatus(status);
        pa.setCreatedAt(LocalDateTime.now());
        return practiceAssignmentRepository.save(pa);
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Feature Coverage (Primary Behavior / Happy Path)")
    class Tier1FeatureCoverage {

        @Test
        @DisplayName("TC-HU9-T1-01: Crear dupla válida con dos estudiantes de Ciclo Intermedio retorna 201 Created")
        void testCreateValidStudentPair_CycleIntermedio_Returns201Created() throws Exception {
            String requestJson = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.assignment1.id").value(asigIntermedio1.getId()))
                    .andExpect(jsonPath("$.assignment2.id").value(asigIntermedio2.getId()));

            List<StudentPair> saved = studentPairRepository.findAll();
            assertEquals(1, saved.size());
            assertEquals(asigIntermedio1.getId(), saved.get(0).getAssignment1().getId());
            assertEquals(asigIntermedio2.getId(), saved.get(0).getAssignment2().getId());
        }

        @Test
        @DisplayName("TC-HU9-T1-02: Listar duplas por periodo académico retorna las duplas existentes")
        void testListPairsByPeriod_ReturnsAllPairs() throws Exception {
            StudentPair pair = new StudentPair();
            pair.setAssignment1(asigIntermedio1);
            pair.setAssignment2(asigIntermedio2);
            pair.setPeriod(activePeriod);
            studentPairRepository.save(pair);

            mockMvc.perform(get("/api/duplas")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].assignment1.student.email").value(student1.getEmail()))
                    .andExpect(jsonPath("$[0].assignment2.student.email").value(student2.getEmail()));
        }

        @Test
        @DisplayName("TC-HU9-T1-03: Consultar estudiantes disponibles retorna solo estudiantes de ciclo intermedio sin dupla")
        void testGetAvailableStudents_ReturnsOnlyUnpairedIntermediateAssignments() throws Exception {
            mockMvc.perform(get("/api/duplas/disponibles")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(empty())))
                    .andExpect(jsonPath("$[*].subject.cycle", everyItem(is("INTERMEDIO"))));
        }

        @Test
        @DisplayName("TC-HU9-T1-04: Una vez creada la dupla, ambos estudiantes desaparecen de disponibles")
        void testGetAvailableStudents_ExcludesAlreadyPairedStudents() throws Exception {
            StudentPair pair = new StudentPair();
            pair.setAssignment1(asigIntermedio1);
            pair.setAssignment2(asigIntermedio2);
            pair.setPeriod(activePeriod);
            studentPairRepository.save(pair);

            mockMvc.perform(get("/api/duplas/disponibles")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", not(hasItem(asigIntermedio1.getId().intValue()))))
                    .andExpect(jsonPath("$[*].id", not(hasItem(asigIntermedio2.getId().intValue()))));
        }

        @Test
        @DisplayName("TC-HU9-T1-05: Verificación de persistencia en base de datos de StudentPair")
        void testDatabaseStateVerification_PersistedCorrectly() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio3.getId(), asigIntermedio4.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated());

            List<StudentPair> pairs = studentPairRepository.findAll();
            assertFalse(pairs.isEmpty());
            StudentPair persisted = pairs.get(0);
            assertNotNull(persisted.getId());
            assertEquals(asigIntermedio3.getId(), persisted.getAssignment1().getId());
            assertEquals(asigIntermedio4.getId(), persisted.getAssignment2().getId());
            assertEquals(activePeriod.getId(), persisted.getPeriod().getId());
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2 - Boundary & Corner Cases")
    class Tier2BoundaryCornerCases {

        @Test
        @DisplayName("TC-HU9-T2-01: Rechazar auto-emparejamiento (mismo assignment1Id == assignment2Id)")
        void testCreatePair_SelfPairing_ReturnsBadRequest() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigIntermedio1.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU9-T2-02: Rechazar duplas donde ambas asignaciones sean de Ciclo INICIAL")
        void testCreatePair_BothStudentsCycleInicial_ReturnsBadRequest() throws Exception {
            PracticeAssignment asigInicial2 = createAssignment(student3, subjectInicial, activePeriod, AssignmentStatus.ACTIVA);

            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigInicial.getId(), asigInicial2.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU9-T2-03: Rechazar duplas con ciclos mixtos (INICIAL + INTERMEDIO)")
        void testCreatePair_MixedCycles_InicialAndIntermedio_ReturnsBadRequest() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigInicial.getId(), asigIntermedio1.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU9-T2-04: Rechazar duplas con ciclos mixtos (AVANZADO + INTERMEDIO)")
        void testCreatePair_MixedCycles_AvanzadoAndIntermedio_ReturnsBadRequest() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigAvanzado.getId(), asigIntermedio2.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU9-T2-05: Rechazar duplas duplicadas asimétricas (A,B existente -> intento B,A)")
        void testCreatePair_DuplicateAsymmetricPair_ReturnsConflictOrBadRequest() throws Exception {
            StudentPair pair = new StudentPair();
            pair.setAssignment1(asigIntermedio1);
            pair.setAssignment2(asigIntermedio2);
            pair.setPeriod(activePeriod);
            studentPairRepository.save(pair);

            // Intento inverso: assignment1=asigIntermedio2, assignment2=asigIntermedio1
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio2.getId(), asigIntermedio1.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("TC-HU9-T2-06: Rechazar asignación si estudiante 1 ya forma parte de otra dupla activa")
        void testCreatePair_StudentAlreadyInExistingPair_ReturnsConflictOrBadRequest() throws Exception {
            StudentPair pair = new StudentPair();
            pair.setAssignment1(asigIntermedio1);
            pair.setAssignment2(asigIntermedio2);
            pair.setPeriod(activePeriod);
            studentPairRepository.save(pair);

            // Intento de asociar estudiante 1 con estudiante 3
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigIntermedio3.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("TC-HU9-T2-07: Rechazar asignación si una de las asignaciones no está ACTIVA")
        void testCreatePair_InactiveAssignment_ReturnsBadRequest() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigInactive.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU9-T2-08: Rechazar creación de duplas entre asignaciones de distintos periodos")
        void testCreatePair_MismatchedPeriods_ReturnsBadRequest() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigPreviousPeriod.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU9-T2-09: Restricción de inmutabilidad: no permitir eliminación de duplas")
        void testPermanenceConstraint_DeletePair_Rejected() throws Exception {
            StudentPair pair = new StudentPair();
            pair.setAssignment1(asigIntermedio1);
            pair.setAssignment2(asigIntermedio2);
            pair.setPeriod(activePeriod);
            pair = studentPairRepository.save(pair);

            mockMvc.perform(delete("/api/duplas/" + pair.getId())
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 403 || status == 405,
                                "Eliminación de dupla debe ser rechazada por inmutabilidad semestral");
                    });
        }
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Nested
    @DisplayName("Tier 3 - Cross-Feature Combinations")
    class Tier3CrossFeatureCombinations {

        @Test
        @DisplayName("TC-HU9-T3-01: Aislamiento por periodo académico: estudiante en dupla en periodo 1 está disponible en periodo 2")
        void testPairIsolationAcrossAcademicPeriods() throws Exception {
            StudentPair pair = new StudentPair();
            pair.setAssignment1(asigPreviousPeriod);
            pair.setAssignment2(asigIntermedio1);
            pair.setPeriod(previousPeriod);
            studentPairRepository.save(pair);

            // En el periodo activo 2026-1, estudiante 1 aún debe figurar disponible si no tiene dupla en 2026-1
            mockMvc.perform(get("/api/duplas/disponibles")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(asigIntermedio1.getId().intValue())));
        }

        @Test
        @DisplayName("TC-HU9-T3-02: Creación válida de duplas compartiendo centro de práctica y tutor")
        void testPairWithSameCenterAndTutor_Valid() throws Exception {
            String payload = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigIntermedio3.getId(), activePeriod.getId()
            );

            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated());
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Tier 4 - Real-World Application Scenarios")
    class Tier4RealWorldScenarios {

        @Test
        @DisplayName("TC-HU9-T4-01: Flujo completo de Administrador: consulta disponibles, crea múltiples duplas y agota lista")
        void testScenario_AdminCreatesMultipleDuplasAndExhaustsAvailableList() throws Exception {
            // 1. Consultar disponibles iniciales (esperamos 4 estudiantes intermedios)
            mockMvc.perform(get("/api/duplas/disponibles")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)));

            // 2. Crear primera dupla (1 y 2)
            String payload1 = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
            );
            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload1))
                    .andExpect(status().isCreated());

            // 3. Consultar disponibles tras primera dupla (esperamos 2 estudiantes restantes: 3 y 4)
            mockMvc.perform(get("/api/duplas/disponibles")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            // 4. Crear segunda dupla (3 y 4)
            String payload2 = String.format(
                    "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                    asigIntermedio3.getId(), asigIntermedio4.getId(), activePeriod.getId()
            );
            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload2))
                    .andExpect(status().isCreated());

            // 5. Consultar disponibles finales (esperamos lista vacía)
            mockMvc.perform(get("/api/duplas/disponibles")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));

            // 6. Listado total de duplas creadas
            mockMvc.perform(get("/api/duplas")
                            .param("periodId", String.valueOf(activePeriod.getId()))
                            .principal(() -> adminUser.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("TC-HU9-T4-02: Resistencia adversarial: rechazar payloads malformados o IDs inexistentes")
        void testScenario_AdversarialInputHandling() throws Exception {
            // IDs inexistentes
            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"assignment1Id\": 99999, \"assignment2Id\": 88888, \"periodId\": 77777}"))
                    .andExpect(status().is4xxClientError());

            // IDs negativos
            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"assignment1Id\": -1, \"assignment2Id\": -2, \"periodId\": -3}"))
                    .andExpect(status().is4xxClientError());

            // Payload vacío
            mockMvc.perform(post("/api/duplas")
                            .principal(() -> adminUser.getEmail())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
