package com.ubb.practicas.controller;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeCycleDefinitionRepository;
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
 * HU5: Pruebas de integración de PracticeCycleController (H2 + seguridad activa).
 * Los tres ciclos los registra PracticeCycleInitializer al levantar el contexto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PracticeCycleControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PracticeCycleDefinitionRepository cycleRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void losTresCiclosQuedanRegistradosAlIniciar() {
        assertEquals(3, cycleRepository.count());
        for (PracticeCycle code : PracticeCycle.values()) {
            assertTrue(cycleRepository.existsByCode(code), "Falta el ciclo " + code);
        }
    }

    // ---------------- Seguridad ----------------

    @Test
    void listarSinAutenticacion_Retorna401() throws Exception {
        mockMvc.perform(get("/api/ciclos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void actualizarComoEstudiante_Retorna403() throws Exception {
        mockMvc.perform(put("/api/ciclos/INICIAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hackeado\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------------- Consulta ----------------

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    void listarCiclos_RetornaLosTresEnOrden() throws Exception {
        mockMvc.perform(get("/api/ciclos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code").value("INICIAL"))
                .andExpect(jsonPath("$[1].code").value("INTERMEDIO"))
                .andExpect(jsonPath("$[2].code").value("AVANZADO"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void obtenerCiclo_IncluyeAsignaturasAsociadas() throws Exception {
        Subject s = new Subject();
        s.setName("Trabajo colaborativo en el aula HU5");
        s.setCycle(PracticeCycle.INTERMEDIO);
        subjectRepository.save(s);

        mockMvc.perform(get("/api/ciclos/intermedio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INTERMEDIO"))
                .andExpect(jsonPath("$.subjects[*].name", hasItem("Trabajo colaborativo en el aula HU5")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void obtenerCicloDesconocido_Retorna400() throws Exception {
        mockMvc.perform(get("/api/ciclos/SUPERIOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Ciclo desconocido")));
    }

    // ---------------- Edición ----------------

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizarCiclo_ComoAdministrador_Persiste() throws Exception {
        String json = "{\"name\":\"Ciclo Avanzado PEM\",\"description\":\"Prácticas finales\",\"startSemester\":8,\"endSemester\":9}";

        mockMvc.perform(put("/api/ciclos/AVANZADO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ciclo Avanzado PEM"))
                .andExpect(jsonPath("$.startSemester").value(8))
                .andExpect(jsonPath("$.endSemester").value(9));

        assertEquals("Ciclo Avanzado PEM",
                cycleRepository.findByCode(PracticeCycle.AVANZADO).orElseThrow().getName());
    }

    @Test
    @WithMockUser(roles = "COORDINADOR_DE_PRACTICAS")
    void actualizarCiclo_NombreVacio_Retorna400() throws Exception {
        mockMvc.perform(put("/api/ciclos/INICIAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizarCiclo_SemestresInvertidos_Retorna400() throws Exception {
        mockMvc.perform(put("/api/ciclos/INTERMEDIO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Intermedio\",\"startSemester\":7,\"endSemester\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("posterior")));
    }
}
