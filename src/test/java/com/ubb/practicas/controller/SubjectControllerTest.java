package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        practiceAssignmentRepository.deleteAll();
        subjectRepository.deleteAll();
    }

    @Test
    public void testCrearAsignaturaConCicloMayuscula() throws Exception {
        String json = "{\"name\":\"Práctica Profesional\",\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Práctica Profesional")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(1, subjectRepository.count());
    }

    @Test
    public void testCrearAsignaturaConCicloCapitalizado() throws Exception {
        String json = "{\"name\":\"Taller de Práctica\",\"cycle\":\"Inicial\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Taller de Práctica")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(1, subjectRepository.count());
    }

    @Test
    public void testCrearAsignaturaConCicloMinusculaYEspacios() throws Exception {
        String json = "{\"name\":\"Práctica Inicial\",\"cycle\":\"  inicial  \"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Práctica Inicial")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(1, subjectRepository.count());
    }

    @Test
    public void testCrearAsignaturaConCaracteresEspeciales() throws Exception {
        String json = "{\"name\":\"Didáctica de la Geometría & Álgebra (Ñ)\",\"cycle\":\"AVANZADO\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Didáctica de la Geometría & Álgebra (Ñ)")))
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        assertEquals(1, subjectRepository.count());
    }

    @Test
    public void testCrearAsignaturaConCicloIntermedioYAvanzado() throws Exception {
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

        assertEquals(2, subjectRepository.count());
    }

    @Test
    public void testCrearAsignaturaConNombreVacioFalla() throws Exception {
        String json = "{\"name\":\"   \",\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El nombre de la asignatura es obligatorio")));
    }

    @Test
    public void testCrearAsignaturaConNombreNullFalla() throws Exception {
        String json = "{\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El nombre de la asignatura es obligatorio")));
    }

    @Test
    public void testCrearAsignaturaConCicloNullFalla() throws Exception {
        String json = "{\"name\":\"Asignatura Sin Ciclo\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El ciclo de la asignatura es obligatorio")));
    }

    @Test
    public void testCrearAsignaturaConNombreExcesivoFalla() throws Exception {
        String longName = "A".repeat(256);
        String json = String.format("{\"name\":\"%s\",\"cycle\":\"INICIAL\"}", longName);

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no puede superar los 255 caracteres")));
    }

    @Test
    public void testListarAsignaturas() throws Exception {
        Subject s = new Subject();
        s.setName("Matemáticas");
        s.setCycle(PracticeCycle.INICIAL);
        subjectRepository.save(s);

        mockMvc.perform(get("/api/asignaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Matemáticas")));
    }

    @Test
    public void testEliminarAsignatura() throws Exception {
        Subject s = new Subject();
        s.setName("Física");
        s.setCycle(PracticeCycle.INTERMEDIO);
        Subject saved = subjectRepository.save(s);

        mockMvc.perform(delete("/api/asignaturas/" + saved.getId()))
                .andExpect(status().isOk());

        assertFalse(subjectRepository.existsById(saved.getId()));
    }

    @Test
    public void testEliminarAsignaturaInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/asignaturas/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testEliminarAsignaturaAsociadaAPracticaRetorna409Conflict() throws Exception {
        Subject s = new Subject();
        s.setName("Práctica con Asignación");
        s.setCycle(PracticeCycle.AVANZADO);
        Subject savedSubject = subjectRepository.save(s);

        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setSubject(savedSubject);
        practiceAssignmentRepository.save(assignment);

        mockMvc.perform(delete("/api/asignaturas/" + savedSubject.getId()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica.")));

        assertTrue(subjectRepository.existsById(savedSubject.getId()), "Subject must not be deleted when referenced by practice assignments");
    }

    @Test
    public void testCrearAsignaturaConCicloInvalidoFalla() throws Exception {
        String json = "{\"name\":\"Inválida\",\"cycle\":\"CICLO_INEXISTENTE\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCrearAsignaturasConcurrencia() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    String json = String.format("{\"name\":\"Asignatura Concurrente %d\",\"cycle\":\"INICIAL\"}", index);
                    mockMvc.perform(post("/api/asignaturas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent requests should finish within timeout");
        assertEquals(threads, successCount.get(), "All 10 concurrent requests must return 200 OK");
        assertEquals(threads, subjectRepository.count(), "All 10 subjects must be persisted to the database");
    }

    @Test
    public void testCrearAsignaturaConIdIgnoradoYGenerado() throws Exception {
        // Step 1: Create an initial subject
        Subject existing = new Subject();
        existing.setName("Materia Original");
        existing.setCycle(PracticeCycle.INICIAL);
        Subject savedExisting = subjectRepository.save(existing);
        Long originalId = savedExisting.getId();

        // Step 2: Attempt to send POST with id set to the existing entity's ID
        String jsonWithExistingId = String.format("{\"id\":%d,\"name\":\"Materia Nueva Separada\",\"cycle\":\"AVANZADO\"}", originalId);
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithExistingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(is(originalId.intValue()))))
                .andExpect(jsonPath("$.name", is("Materia Nueva Separada")))
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        // Verify the original entity was NOT overwritten
        Subject refetchedOriginal = subjectRepository.findById(originalId).orElseThrow();
        assertEquals("Materia Original", refetchedOriginal.getName());
        assertEquals(PracticeCycle.INICIAL, refetchedOriginal.getCycle());

        // Step 3: Attempt to send POST with non-existent ID (should not crash with 500)
        String jsonWithNonExistentId = "{\"id\":999999,\"name\":\"Materia ID Alto\",\"cycle\":\"INTERMEDIO\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithNonExistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(is(999999))))
                .andExpect(jsonPath("$.name", is("Materia ID Alto")));

        assertEquals(3, subjectRepository.count());
    }

    @Test
    public void testCrearAsignaturaConCodigoExcesivoFalla() throws Exception {
        String longCode = "C".repeat(51);
        String json = String.format("{\"name\":\"Química General\",\"cycle\":\"INICIAL\",\"code\":\"%s\"}", longCode);

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El código de la asignatura no puede superar los 50 caracteres")));
    }

    @Test
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
    public void testCrearAsignaturaConProfesorInexistenteFalla() throws Exception {
        String json = "{\"name\":\"Bioquímica\",\"cycle\":\"INTERMEDIO\",\"professor\":{\"id\":999999}}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El profesor especificado no existe")));
    }
}
