package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SubjectController.
 * Resolves:
 * - TS-01 & TS-02: Isolated H2 in-memory DB and @Transactional automatic rollback (no deleteAll teardown).
 * - TS-03: Security filters enabled with @WithMockUser RBAC testing.
 * - TS-04: Deterministic transactional tests replacing flaky CountDownLatch.
 * - TS-05: Structured JSONPath assertions against ApiErrorResponse.
 */
@SpringBootTest
@AutoConfigureMockMvc // TS-03: Filters active (addFilters = false removed)
@ActiveProfiles("test") // TS-01: H2 in-memory database
@Transactional // TS-02: Automatic rollback, eliminating destructive deleteAll()
public class SubjectControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @BeforeEach
    public void setupSecurity() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ==========================================
    // SECURITY & RBAC VERIFICATION (TS-03)
    // ==========================================

    @Test
    public void testListarAsignaturasSinAutenticacionFalla() throws Exception {
        mockMvc.perform(get("/api/asignaturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCrearAsignaturaSinAutenticacionFalla() throws Exception {
        String json = "{\"name\":\"Práctica No Autenticada\",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    public void testCrearAsignaturaConRolEstudianteRetorna403Forbidden() throws Exception {
        String json = "{\"name\":\"Práctica No Autorizada\",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    public void testEliminarAsignaturaConRolEstudianteRetorna403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/asignaturas/1"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // FUNCTIONAL TESTS (ROLE = ADMINISTRADOR)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloMayuscula() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Práctica Profesional\",\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Práctica Profesional")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloCapitalizado() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Taller de Práctica\",\"cycle\":\"Inicial\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Taller de Práctica")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloMinusculaYEspacios() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Práctica Inicial Test\",\"cycle\":\"  inicial  \"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Práctica Inicial Test")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCaracteresEspeciales() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Didáctica de la Geometría & Álgebra (Ñ)\",\"cycle\":\"AVANZADO\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Didáctica de la Geometría & Álgebra (Ñ)")))
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloIntermedioYAvanzado() throws Exception {
        long initialCount = subjectRepository.count();
        String json1 = "{\"name\":\"Asignatura 2\",\"cycle\":\"Intermedio\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycle", is("INTERMEDIO")));

        String json2 = "{\"name\":\"Asignatura 3\",\"cycle\":\"Avanzado\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        assertEquals(initialCount + 2, subjectRepository.count());
    }

    // ==========================================
    // STRUCTURED JSON VALIDATION ASSERTIONS (TS-05)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConNombreVacioFalla() throws Exception {
        String json = "{\"name\":\"   \",\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El nombre de la asignatura es obligatorio"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConNombreNullFalla() throws Exception {
        String json = "{\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El nombre de la asignatura es obligatorio"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloNullFalla() throws Exception {
        String json = "{\"name\":\"Asignatura Sin Ciclo\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El ciclo de la asignatura es obligatorio"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConNombreExcesivoFalla() throws Exception {
        String longName = "A".repeat(256);
        String json = String.format("{\"name\":\"%s\",\"cycle\":\"INICIAL\"}", longName);

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El nombre de la asignatura no puede superar los 255 caracteres"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCodigoExcesivoFalla() throws Exception {
        String longCode = "C".repeat(51);
        String json = String.format("{\"name\":\"Química General\",\"cycle\":\"INICIAL\",\"code\":\"%s\"}", longCode);

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El código de la asignatura no puede superar los 50 caracteres"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCodigoValido() throws Exception {
        String json = "{\"name\":\"Física Mecánica\",\"cycle\":\"INICIAL\",\"code\":\"  FIS-101  \"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("FIS-101")))
                .andExpect(jsonPath("$.name", is("Física Mecánica")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConProfesorInexistenteFalla() throws Exception {
        String json = "{\"name\":\"Bioquímica\",\"cycle\":\"INTERMEDIO\",\"professor\":{\"id\":999999}}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El profesor especificado no existe"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloInvalidoFalla() throws Exception {
        String json = "{\"name\":\"Inválida\",\"cycle\":\"CICLO_INEXISTENTE\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ==========================================
    // CRUD & CONFLICT ASSERTIONS (TS-05)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testListarAsignaturas() throws Exception {
        Subject s = new Subject();
        s.setName("Matemáticas Integrales");
        s.setCycle(PracticeCycle.INICIAL);
        subjectRepository.save(s);

        mockMvc.perform(get("/api/asignaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.name == 'Matemáticas Integrales')].cycle").value("INICIAL"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarAsignatura() throws Exception {
        Subject s = new Subject();
        s.setName("Física Cuántica");
        s.setCycle(PracticeCycle.INTERMEDIO);
        Subject saved = subjectRepository.save(s);

        mockMvc.perform(delete("/api/asignaturas/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Asignatura eliminada exitosamente")));

        assertFalse(subjectRepository.existsById(saved.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarAsignaturaInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/asignaturas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Asignatura no encontrada"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas/999999"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarAsignaturaAsociadaAPracticaRetorna409Conflict() throws Exception {
        Subject s = new Subject();
        s.setName("Práctica con Asignación Activa");
        s.setCycle(PracticeCycle.AVANZADO);
        Subject savedSubject = subjectRepository.save(s);

        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setSubject(savedSubject);
        practiceAssignmentRepository.save(assignment);

        mockMvc.perform(delete("/api/asignaturas/" + savedSubject.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica."))
                .andExpect(jsonPath("$.path").value("/api/asignaturas/" + savedSubject.getId()));

        assertTrue(subjectRepository.existsById(savedSubject.getId()), "Subject must not be deleted when referenced by practice assignments");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConIdIgnoradoYGenerado() throws Exception {
        Subject existing = new Subject();
        existing.setName("Materia Original Existente");
        existing.setCycle(PracticeCycle.INICIAL);
        Subject savedExisting = subjectRepository.save(existing);
        Long originalId = savedExisting.getId();

        String jsonWithExistingId = String.format("{\"id\":%d,\"name\":\"Materia Nueva Separada\",\"cycle\":\"AVANZADO\"}", originalId);
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithExistingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(is(originalId.intValue()))))
                .andExpect(jsonPath("$.name", is("Materia Nueva Separada")))
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        Subject refetchedOriginal = subjectRepository.findById(originalId).orElseThrow();
        assertEquals("Materia Original Existente", refetchedOriginal.getName());
    }

    // ==========================================
    // DETERMINISTIC TRANSACTIONAL TESTS (TS-04)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearMultiplesAsignaturasTransaccional() throws Exception {
        int count = 5;
        long initialCount = subjectRepository.count();

        for (int i = 0; i < count; i++) {
            String json = String.format("{\"name\":\"Asignatura Secuencial %d\",\"cycle\":\"INICIAL\",\"code\":\"SEC-%03d\"}", i, i);
            mockMvc.perform(post("/api/asignaturas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(String.format("Asignatura Secuencial %d", i))))
                    .andExpect(jsonPath("$.code", is(String.format("SEC-%03d", i))));
        }

        assertEquals(initialCount + count, subjectRepository.count(), "All sequential transactional subjects must be persisted");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testAislamientoTransaccionalConRollbackEnFallo() throws Exception {
        long initialCount = subjectRepository.count();

        // 1. Successful creation
        String validJson = "{\"name\":\"Asignatura Valida Transaccion\",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isOk());

        assertEquals(initialCount + 1, subjectRepository.count());

        // 2. Failed creation must not alter persisted state beyond the failed request
        String invalidJson = "{\"name\":\"   \",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertEquals(initialCount + 1, subjectRepository.count());
    }
}
