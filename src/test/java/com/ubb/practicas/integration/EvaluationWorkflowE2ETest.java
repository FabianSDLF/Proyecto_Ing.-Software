package com.ubb.practicas.integration;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import com.ubb.practicas.service.EvaluationRubricCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("HU43, HU46 y HU47 E2E Integration Suite: Pautas del Tutor, Cierre Oficial y Consulta Estudiantil")
public class EvaluationWorkflowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvaluationRubricCatalog rubricCatalog;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

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

    private User tutor;
    private User otherTutor;
    private User collaborator;
    private User professor;
    private User student;
    private User otherStudent;
    private PracticeAssignment assignment;

    @BeforeEach
    void setUp() {
        cleanup();

        Role rTutor = getOrCreateRole("Tutor");
        Role rColab = getOrCreateRole("Profesor Colaborador");
        Role rProf = getOrCreateRole("Profesor de Asignatura");
        Role rEst = getOrCreateRole("Estudiante");

        tutor = createUser("Mg. Claudia Arriagada", "tutor.e2e@ubb.cl", rTutor);
        otherTutor = createUser("Mg. Otro Tutor", "otro.tutor.e2e@ubb.cl", rTutor);
        collaborator = createUser("Prof. Carlos Riquelme", "colab.e2e@ubb.cl", rColab);
        professor = createUser("Dr. Gonzalo Henríquez", "prof.e2e@ubb.cl", rProf);
        student = createUser("Valentina Castro", "estudiante.e2e@ubb.cl", rEst);
        otherStudent = createUser("Diego Santander", "otro.estudiante.e2e@ubb.cl", rEst);

        AcademicPeriod period = academicPeriodRepository.save(new AcademicPeriod(
                null, "2026-2", 2026, 2,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20),
                PeriodStatus.ACTIVO
        ));

        Subject subject = subjectRepository.save(new Subject(
                null, "Práctica Profesional", PracticeCycle.AVANZADO, "PEM-310", professor
        ));

        subject.setProfessionalRubrics(true);
        subjectRepository.save(subject);

        PracticeCenter center = practiceCenterRepository.save(new PracticeCenter(
                null, "Colegio Concepción", SchoolDependency.PRIVADO,
                "Av. Central 123", "Concepción", "Concepción",
                "+56412223344", "Director", "director@colegio.cl"
        ));

        assignment = practiceAssignmentRepository.save(new PracticeAssignment(
                null, student, subject, center, collaborator, tutor,
                period, AssignmentStatus.ACTIVA, LocalDateTime.now()
        ));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        evaluationCriteriaRepository.deleteAll();
        evaluationRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
    }

    private Role getOrCreateRole(String name) {
        Role r = roleRepository.findByName(name);
        return r != null ? r : roleRepository.save(new Role(null, name));
    }

    private User createUser(String fullName, String email, Role role) {
        User existing = userRepository.findByEmail(email);
        if (existing != null) return existing;
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setActive(true);
        u.setRole(role);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    @Test
    @DisplayName("Recorrido HTTP Completo HU43 -> HU46 -> HU47: catálogo tutor, borrador privado, cierre atómico, 409 y lectura del estudiante")
    void testFlujoCompletoHu43Hu46Hu47() throws Exception {
        // 1. HU43: El tutor asignado consulta sus pautas aplicables a la asignación
        mockMvc.perform(get("/api/evaluaciones/pautas")
                        .param("assignmentId", String.valueOf(assignment.getId()))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].type", is("TUTOR_ESP_1")))
                .andExpect(jsonPath("$[0].rubricVersion", is("2025-2-v1")))
                .andExpect(jsonPath("$[0].maxPoints", is(133)));

        // Un tutor no asignado o un estudiante recibe 403 Forbidden
        mockMvc.perform(get("/api/evaluaciones/pautas")
                        .param("assignmentId", String.valueOf(assignment.getId()))
                        .principal(() -> otherTutor.getEmail()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/evaluaciones/pautas")
                        .param("assignmentId", String.valueOf(assignment.getId()))
                        .principal(() -> student.getEmail()))
                .andExpect(status().isForbidden());

        // 2. Guardar borrador parcial (solo 2 indicadores de 19): score debe ser null
        Map<String, Object> partialPayload = new LinkedHashMap<>();
        partialPayload.put("practiceAssignmentId", assignment.getId());
        partialPayload.put("type", "TUTOR_ESP_1");
        partialPayload.put("rubricVersion", "2025-2-v1");
        partialPayload.put("expectedRevision", null);
        partialPayload.put("qualitativeComments", "Borrador inicial privado de la primera visita");
        partialPayload.put("criteria", List.of(
                Map.of("criterionCode", "A1", "points", 4),
                Map.of("criterionCode", "A2", "points", 4)
        ));

        String partialResp = mockMvc.perform(post("/api/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPayload))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision", is(1)))
                .andExpect(jsonPath("$.isFinalized", is(false)))
                .andExpect(jsonPath("$.totalPoints", is(8)))
                .andExpect(jsonPath("$.maxPoints", is(133)))
                .andExpect(jsonPath("$.score").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        JsonNode partialJson = objectMapper.readTree(partialResp);
        long evalId = partialJson.get("id").asLong();

        // 3. Privacidad de borradores: ni el estudiante, ni el profesor de asignatura, ni el colaborador pueden ver el borrador del tutor
        mockMvc.perform(get("/api/evaluaciones/asignacion/" + assignment.getId())
                        .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/evaluaciones/" + evalId)
                        .principal(() -> student.getEmail()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/evaluaciones/" + evalId)
                        .principal(() -> professor.getEmail()))
                .andExpect(status().isForbidden());

        // 4. Intentar finalizar borrador incompleto -> 400 Bad Request
        mockMvc.perform(post("/api/evaluaciones/" + evalId + "/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectedRevision", 1, "confirmed", true)))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isBadRequest());

        // 5. Actualizar borrador completando los 19 indicadores para sumar 78 puntos (nota esperada: 4.0)
        // 18 indicadores con 4 puntos (72) + 1 indicador con 6 puntos (6) = 78 puntos
        var rubric = rubricCatalog.findRubric(EvaluationType.TUTOR_ESP_1).orElseThrow();
        List<Map<String, Object>> fullCriteria = new ArrayList<>();
        for (int i = 0; i < rubric.getIndicators().size(); i++) {
            var ind = rubric.getIndicators().get(i);
            int pts = (i == 0) ? 6 : 4;
            fullCriteria.add(Map.of("criterionCode", ind.getCriterionCode(), "points", pts));
        }

        Map<String, Object> completeDraftPayload = new LinkedHashMap<>();
        completeDraftPayload.put("practiceAssignmentId", assignment.getId());
        completeDraftPayload.put("type", "TUTOR_ESP_1");
        completeDraftPayload.put("rubricVersion", "2025-2-v1");
        completeDraftPayload.put("expectedRevision", 1);
        completeDraftPayload.put("score", 6.9); // Intento de manipulación desde el cliente: el servidor debe calcular 4.0
        completeDraftPayload.put("qualitativeComments", "Observación definitiva publicada para el estudiante.");
        completeDraftPayload.put("criteria", fullCriteria);

        mockMvc.perform(post("/api/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDraftPayload))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) evalId)))
                .andExpect(jsonPath("$.revision", is(2)))
                .andExpect(jsonPath("$.totalPoints", is(78)))
                .andExpect(jsonPath("$.maxPoints", is(133)))
                .andExpect(jsonPath("$.score", is(4.0)));

        // 6. Finalizar y publicar atómicamente con expectedRevision=2 y confirmed=true
        mockMvc.perform(post("/api/evaluaciones/" + evalId + "/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectedRevision", 2, "confirmed", true)))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFinalized", is(true)))
                .andExpect(jsonPath("$.revision", is(3)))
                .andExpect(jsonPath("$.score", is(4.0)))
                .andExpect(jsonPath("$.finalizedByEmail", is(tutor.getEmail())))
                .andExpect(jsonPath("$.finalizedByName", is(tutor.getFullName())))
                .andExpect(jsonPath("$.snapshotHash", hasLength(64)));

        // 7. Repetición de firma o intento de sobrescritura posterior -> 409 Conflict
        mockMvc.perform(post("/api/evaluaciones/" + evalId + "/finalizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectedRevision", 3, "confirmed", true)))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isConflict());

        completeDraftPayload.put("expectedRevision", 3);
        mockMvc.perform(post("/api/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDraftPayload))
                        .principal(() -> tutor.getEmail()))
                .andExpect(status().isConflict());

        // 8. HU47: El estudiante consulta su evaluación recién publicada en listado y detalle por ID
        mockMvc.perform(get("/api/evaluaciones/asignacion/" + assignment.getId())
                        .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) evalId)))
                .andExpect(jsonPath("$[0].score", is(4.0)))
                .andExpect(jsonPath("$[0].totalPoints", is(78)))
                .andExpect(jsonPath("$[0].maxPoints", is(133)))
                .andExpect(jsonPath("$[0].qualitativeComments", is("Observación definitiva publicada para el estudiante.")));

        mockMvc.perform(get("/api/evaluaciones/" + evalId)
                        .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFinalized", is(true)))
                .andExpect(jsonPath("$.criteria", hasSize(19)))
                .andExpect(jsonPath("$.criteria[0].criterionCode", is("A1")))
                .andExpect(jsonPath("$.criteria[0].points", is(6.0)));

        // Otro estudiante no puede consultar la evaluación por ID ni por asignación (403 Forbidden)
        mockMvc.perform(get("/api/evaluaciones/" + evalId)
                        .principal(() -> otherStudent.getEmail()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/evaluaciones/asignacion/" + assignment.getId())
                        .principal(() -> otherStudent.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Restricción SQL uk_evaluaciones_assignment_type: impide duplicados para misma asignación e instrumento")
    void testRestriccionUnicidadSqlAsignacionYTipo() {
        Evaluation first = new Evaluation();
        first.setPracticeAssignment(assignment);
        first.setEvaluator(tutor);
        first.setType(EvaluationType.TUTOR_ESP_2);
        first.setRevision(1L);
        evaluationRepository.saveAndFlush(first);

        Evaluation duplicate = new Evaluation();
        duplicate.setPracticeAssignment(assignment);
        duplicate.setEvaluator(tutor);
        duplicate.setType(EvaluationType.TUTOR_ESP_2);
        duplicate.setRevision(1L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> evaluationRepository.saveAndFlush(duplicate)
        );
    }
}
