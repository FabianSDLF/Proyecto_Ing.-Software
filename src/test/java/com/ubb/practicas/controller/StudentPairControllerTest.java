package com.ubb.practicas.controller;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.PeriodStatus;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration / Security Tests: StudentPairController")
public class StudentPairControllerTest {

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

    private AcademicPeriod activePeriod;
    private PracticeAssignment asigIntermedio1;
    private PracticeAssignment asigIntermedio2;
    private PracticeAssignment asigInicial;

    @BeforeEach
    void setUp() {
        studentPairRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
        subjectRepository.deleteAll();
        practiceCenterRepository.deleteAll();
        academicPeriodRepository.deleteAll();
        userRepository.deleteAll();

        Role studentRole = roleRepository.findByName("Estudiante");
        if (studentRole == null) {
            studentRole = roleRepository.save(new Role(null, "Estudiante"));
        }

        User s1 = new User();
        s1.setEmail("est1@ubb.cl");
        s1.setFullName("Estudiante Uno");
        s1.setPasswordHash("hash");
        s1.setActive(true);
        s1.setRole(studentRole);
        s1 = userRepository.save(s1);

        User s2 = new User();
        s2.setEmail("est2@ubb.cl");
        s2.setFullName("Estudiante Dos");
        s2.setPasswordHash("hash");
        s2.setActive(true);
        s2.setRole(studentRole);
        s2 = userRepository.save(s2);

        activePeriod = new AcademicPeriod();
        activePeriod.setName("2026-1");
        activePeriod.setYear(2026);
        activePeriod.setSemesterNumber(1);
        activePeriod.setStartDate(LocalDate.of(2026, 3, 1));
        activePeriod.setEndDate(LocalDate.of(2026, 7, 31));
        activePeriod.setStatus(PeriodStatus.ACTIVO);
        activePeriod = academicPeriodRepository.save(activePeriod);

        PracticeCenter center = new PracticeCenter();
        center.setName("Liceo Prueba");
        center.setCity("Chillán");
        center = practiceCenterRepository.save(center);

        Subject subIntermedio = new Subject();
        subIntermedio.setName("Práctica Intermedia");
        subIntermedio.setCode("INT-101");
        subIntermedio.setCycle(PracticeCycle.INTERMEDIO);
        subIntermedio = subjectRepository.save(subIntermedio);

        Subject subInicial = new Subject();
        subInicial.setName("Práctica Inicial");
        subInicial.setCode("INI-101");
        subInicial.setCycle(PracticeCycle.INICIAL);
        subInicial = subjectRepository.save(subInicial);

        asigIntermedio1 = new PracticeAssignment();
        asigIntermedio1.setStudent(s1);
        asigIntermedio1.setSubject(subIntermedio);
        asigIntermedio1.setCenter(center);
        asigIntermedio1.setPeriod(activePeriod);
        asigIntermedio1.setStatus(AssignmentStatus.ACTIVA);
        asigIntermedio1.setCreatedAt(LocalDateTime.now());
        asigIntermedio1 = practiceAssignmentRepository.save(asigIntermedio1);

        asigIntermedio2 = new PracticeAssignment();
        asigIntermedio2.setStudent(s2);
        asigIntermedio2.setSubject(subIntermedio);
        asigIntermedio2.setCenter(center);
        asigIntermedio2.setPeriod(activePeriod);
        asigIntermedio2.setStatus(AssignmentStatus.ACTIVA);
        asigIntermedio2.setCreatedAt(LocalDateTime.now());
        asigIntermedio2 = practiceAssignmentRepository.save(asigIntermedio2);

        asigInicial = new PracticeAssignment();
        asigInicial.setStudent(s1);
        asigInicial.setSubject(subInicial);
        asigInicial.setCenter(center);
        asigInicial.setPeriod(activePeriod);
        asigInicial.setStatus(AssignmentStatus.ACTIVA);
        asigInicial.setCreatedAt(LocalDateTime.now());
        asigInicial = practiceAssignmentRepository.save(asigInicial);
    }

    @AfterEach
    void tearDown() {
        studentPairRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
        subjectRepository.deleteAll();
        practiceCenterRepository.deleteAll();
        academicPeriodRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Crear dupla con rol ADMINISTRADOR retorna 201 Created")
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearDupla_ComoAdministrador_Retorna201() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.assignment1Id").value(asigIntermedio1.getId()))
                .andExpect(jsonPath("$.assignment2Id").value(asigIntermedio2.getId()));
    }

    @Test
    @DisplayName("Crear dupla con rol Coordinador de practicas retorna 201 Created")
    @WithMockUser(roles = "Coordinador de practicas")
    void testCrearDupla_ComoCoordinador_Retorna201() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Crear dupla sin autenticación retorna 401 Unauthorized")
    void testCrearDupla_SinAutenticacion_Retorna401() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Crear dupla con rol no autorizado (Estudiante) retorna 403 Forbidden")
    @WithMockUser(roles = "Estudiante")
    void testCrearDupla_RolEstudiante_Retorna403() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crear dupla con rol Profesor de Asignatura retorna 403 Forbidden")
    @WithMockUser(roles = "Profesor de Asignatura")
    void testCrearDupla_RolProfesor_Retorna403() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigIntermedio1.getId(), asigIntermedio2.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auto-emparejamiento retorna 400 Bad Request")
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearDupla_AutoEmparejamiento_Retorna400() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigIntermedio1.getId(), asigIntermedio1.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ciclo no intermedio retorna 400 Bad Request")
    @WithMockUser(roles = "ADMINISTRADOR")
    void testCrearDupla_CicloNoIntermedio_Retorna400() throws Exception {
        String payload = String.format(
                "{\"assignment1Id\": %d, \"assignment2Id\": %d, \"periodId\": %d}",
                asigInicial.getId(), asigIntermedio2.getId(), activePeriod.getId()
        );

        mockMvc.perform(post("/api/duplas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Listar duplas por período retorna 200 OK")
    @WithMockUser(roles = "ADMINISTRADOR")
    void testListarDuplas_Retorna200() throws Exception {
        StudentPair pair = new StudentPair();
        pair.setAssignment1(asigIntermedio1);
        pair.setAssignment2(asigIntermedio2);
        pair.setPeriod(activePeriod);
        studentPairRepository.save(pair);

        mockMvc.perform(get("/api/duplas")
                        .param("periodId", String.valueOf(activePeriod.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Consultar disponibles retorna 200 OK con asignaciones no emparejadas")
    @WithMockUser(roles = "ADMINISTRADOR")
    void testListarDisponibles_Retorna200() throws Exception {
        mockMvc.perform(get("/api/duplas/disponibles")
                        .param("periodId", String.valueOf(activePeriod.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Inmutabilidad: DELETE /api/duplas/{id} es rechazado con 400 Bad Request")
    @WithMockUser(roles = "ADMINISTRADOR")
    void testEliminarDupla_Inmutabilidad_Retorna400() throws Exception {
        StudentPair pair = new StudentPair();
        pair.setAssignment1(asigIntermedio1);
        pair.setAssignment2(asigIntermedio2);
        pair.setPeriod(activePeriod);
        pair = studentPairRepository.save(pair);

        mockMvc.perform(delete("/api/duplas/" + pair.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inmutables")));
    }
}
