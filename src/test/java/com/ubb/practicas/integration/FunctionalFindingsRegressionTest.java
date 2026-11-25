package com.ubb.practicas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import com.ubb.practicas.security.JwtUtil;
import com.ubb.practicas.service.LessonPlanService;
import com.ubb.practicas.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FunctionalFindingsRegressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private PracticeCenterRepository practiceCenterRepository;
    @Autowired private PracticeAssignmentRepository practiceAssignmentRepository;
    @Autowired private PortfolioDocumentRepository portfolioDocumentRepository;
    @Autowired private LessonPlanRepository lessonPlanRepository;
    @Autowired private ChatbotLogRepository chatbotLogRepository;
    @Autowired private ChatbotMessageRepository chatbotMessageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private StorageService storageService;
    @Autowired private LessonPlanService lessonPlanService;
    @Autowired private PlatformTransactionManager transactionManager;

    private User coordinator;
    private User professor;
    private User tutor;
    private User otherTutor;
    private User collaborator;
    private User student;
    private User otherStudent;
    private PracticeAssignment assignment;

    private String coordinatorToken;
    private String professorToken;
    private String tutorToken;
    private String otherTutorToken;
    private String collaboratorToken;
    private String studentToken;
    private String otherStudentToken;

    private Role ensureRole(String name) {
        Role r = roleRepository.findByName(name);
        if (r == null) {
            r = new Role();
            r.setName(name);
            r = roleRepository.save(r);
        }
        return r;
    }

    private User ensureUser(String email, String fullName, String phone, Role role) {
        User u = userRepository.findByEmail(email);
        if (u == null) {
            u = new User();
            u.setEmail(email);
            u.setPasswordHash("$2a$10$dummyhashfortesting1234567890123456789012345678901234");
            u.setCreatedAt(LocalDateTime.now());
        }
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        Role coordRole = ensureRole("Coordinador de practicas");
        Role profRole = ensureRole("Profesor de Asignatura");
        Role tutorRole = ensureRole("Tutor");
        Role collabRole = ensureRole("Profesor Colaborador");
        Role studentRole = ensureRole("Estudiante");

        coordinator = ensureUser("qa.coord.reg@ubb.cl", "Coord QA", "+56911110001", coordRole);
        professor = ensureUser("qa.prof.reg@ubb.cl", "Prof QA", "+56911110002", profRole);
        tutor = ensureUser("qa.tutor.reg@ubb.cl", "Tutor QA", "+56911110003", tutorRole);
        otherTutor = ensureUser("qa.othertutor.reg@ubb.cl", "Otro Tutor QA", "+56911110004", tutorRole);
        collaborator = ensureUser("qa.collab.reg@ubb.cl", "Collab QA", "+56911110005", collabRole);
        student = ensureUser("qa.student.reg@ubb.cl", "Student QA", "+56911110006", studentRole);
        otherStudent = ensureUser("qa.otherstudent.reg@ubb.cl", "Other Student QA", "+56911110007", studentRole);

        Subject subject = new Subject();
        subject.setCode("MAT-REG-900");
        subject.setName("Práctica Profesional Regresión");
        subject.setCycle(PracticeCycle.AVANZADO);
        subject.setProfessor(professor);
        subject = subjectRepository.save(subject);

        PracticeCenter center = new PracticeCenter();
        center.setName("Liceo Regresión");
        center.setAddress("Av. Test 123");
        center = practiceCenterRepository.save(center);

        assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setSubject(subject);
        assignment.setCenter(center);
        assignment.setTutor(tutor);
        assignment.setCollaborator(collaborator);
        assignment.setStatus(AssignmentStatus.ACTIVA);
        assignment = practiceAssignmentRepository.save(assignment);

        coordinatorToken = jwtUtil.generateToken(coordinator.getEmail(), coordRole.getName());
        professorToken = jwtUtil.generateToken(professor.getEmail(), profRole.getName());
        tutorToken = jwtUtil.generateToken(tutor.getEmail(), tutorRole.getName());
        otherTutorToken = jwtUtil.generateToken(otherTutor.getEmail(), tutorRole.getName());
        collaboratorToken = jwtUtil.generateToken(collaborator.getEmail(), collabRole.getName());
        studentToken = jwtUtil.generateToken(student.getEmail(), studentRole.getName());
        otherStudentToken = jwtUtil.generateToken(otherStudent.getEmail(), studentRole.getName());
    }

    @AfterEach
    void tearDown() {
        chatbotMessageRepository.deleteAll();
        chatbotLogRepository.deleteAll();
        lessonPlanRepository.deleteAll();
        portfolioDocumentRepository.deleteAll();
        notificationRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
    }

    // =========================================================================
    // F14: Eliminar planificación revisada con IA + preservación ante rollback
    // =========================================================================
    @Test
    void f14_deleteReviewedLessonPlanWithAttachment_succeedsAndDeletesPhysicalFile() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "guia-f14.pdf", "application/pdf", "%PDF-1.4 test content".getBytes(StandardCharsets.UTF_8));
        String storedFileName = storageService.store(pdf);

        PortfolioDocument doc = new PortfolioDocument();
        doc.setPracticeAssignment(assignment);
        doc.setType(DocumentType.PLANIFICACION);
        doc.setFileName("guia-f14.pdf");
        doc.setStoredFileName(storedFileName);
        doc.setContentType("application/pdf");
        doc.setSizeBytes(pdf.getSize());
        doc.setUploadDate(LocalDateTime.now());
        doc = portfolioDocumentRepository.save(doc);

        LessonPlan plan = new LessonPlan();
        plan.setPracticeAssignment(assignment);
        plan.setDocument(doc);
        plan.setStatus(PlanStatus.BORRADOR);
        plan.setVersion(1);
        plan.setSchoolGrade("1° Medio");
        plan.setMathAxis(MathAxis.NUMEROS);
        plan.setBaseLearningObjective("OA F14");
        plan.setObjectives("Objetivo F14");
        plan.setContentJson("{\"inicio\":\"a\",\"desarrollo\":\"b\",\"cierre\":\"c\"}");
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        plan = lessonPlanRepository.save(plan);

        ChatbotLog log = new ChatbotLog(null, plan, "Feedback IA", true, 1, LocalDateTime.now());
        log = chatbotLogRepository.save(log);

        ChatbotMessage msg = new ChatbotMessage();
        msg.setChatbotLog(log);
        msg.setSenderRole(MessageRole.ESTUDIANTE);
        msg.setContent("Consulta de prueba");
        msg.setOrderNum(1);
        msg.setTimestamp(LocalDateTime.now());
        chatbotMessageRepository.save(msg);

        // Antes de eliminar, el archivo existe
        assertTrue(storageService.loadAsResource(storedFileName).exists());

        mockMvc.perform(delete("/api/portafolio/planificaciones/" + plan.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        assertTrue(lessonPlanRepository.findById(plan.getId()).isEmpty());
        assertTrue(chatbotLogRepository.findByLessonPlanId(plan.getId()).isEmpty());
        assertTrue(portfolioDocumentRepository.findById(doc.getId()).isEmpty());
        assertThrows(Exception.class, () -> storageService.loadAsResource(storedFileName));
    }

    @Test
    void f14_whenTransactionRollsBack_physicalAttachmentIsNotDeleted() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "rollback-f14.pdf", "application/pdf", "%PDF-1.4 rollback".getBytes(StandardCharsets.UTF_8));
        String storedFileName = storageService.store(pdf);

        PortfolioDocument doc = new PortfolioDocument();
        doc.setPracticeAssignment(assignment);
        doc.setType(DocumentType.PLANIFICACION);
        doc.setFileName("rollback-f14.pdf");
        doc.setStoredFileName(storedFileName);
        doc.setContentType("application/pdf");
        doc.setSizeBytes(pdf.getSize());
        doc.setUploadDate(LocalDateTime.now());
        doc = portfolioDocumentRepository.save(doc);

        LessonPlan plan = new LessonPlan();
        plan.setPracticeAssignment(assignment);
        plan.setDocument(doc);
        plan.setStatus(PlanStatus.BORRADOR);
        plan.setVersion(1);
        plan.setSchoolGrade("2° Medio");
        plan.setMathAxis(MathAxis.ALGEBRA);
        plan.setBaseLearningObjective("OA Rollback");
        plan.setObjectives("Obj Rollback");
        plan.setContentJson("{}");
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        LessonPlan savedPlan = lessonPlanRepository.save(plan);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        assertThrows(RuntimeException.class, () -> txTemplate.executeWithoutResult(status -> {
            lessonPlanService.eliminarPlanificacion(savedPlan.getId(), student.getEmail());
            throw new RuntimeException("Fallo simulado antes de commit");
        }));

        // La entidad y el archivo físico deben conservarse intactos tras el rollback
        assertTrue(lessonPlanRepository.findById(savedPlan.getId()).isPresent());
        assertTrue(storageService.loadAsResource(storedFileName).exists());

        // Limpieza del archivo sintético
        storageService.deleteFile(storedFileName);
    }

    // =========================================================================
    // F06 y F07: Permisos de comunicados y destinatarios
    // =========================================================================
    @Test
    void f06_comunicadoDeniedForStudentCollaboratorTutorAndAnonymous_allowedForCoordAndProfessor() throws Exception {
        String individualBody = "{\"mensaje\":\"Prueba F06\",\"correoDestinatario\":\"" + student.getEmail() + "\"}";
        String broadcastBody = "{\"mensaje\":\"Aviso F06 todos\",\"correoDestinatario\":\"TODOS\"}";

        // Sin sesión -> 401
        mockMvc.perform(post("/api/notificaciones/comunicado")
                        .contentType(MediaType.APPLICATION_JSON).content(individualBody))
                .andExpect(status().isUnauthorized());

        // Estudiante, Colaborador y Tutor -> 403 tanto individual como TODOS
        for (String token : List.of(studentToken, collaboratorToken, tutorToken)) {
            mockMvc.perform(post("/api/notificaciones/comunicado")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(individualBody))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/notificaciones/comunicado")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(broadcastBody))
                    .andExpect(status().isForbidden());
        }

        // Coordinador -> 200
        mockMvc.perform(post("/api/notificaciones/comunicado")
                        .header("Authorization", "Bearer " + coordinatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(individualBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Profesor de asignatura -> 200 dentro de su alcance, 403 fuera de su alcance
        mockMvc.perform(post("/api/notificaciones/comunicado")
                        .header("Authorization", "Bearer " + professorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(individualBody))
                .andExpect(status().isOk());

        String outOfScopeBody = "{\"mensaje\":\"Fuera de alcance\",\"correoDestinatario\":\"" + otherStudent.getEmail() + "\"}";
        mockMvc.perform(post("/api/notificaciones/comunicado")
                        .header("Authorization", "Bearer " + professorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(outOfScopeBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void f07_destinatariosRestrictedByRoleAndOmitsSensitiveFields() throws Exception {
        // Sin sesión -> 401
        mockMvc.perform(get("/api/notificaciones/destinatarios"))
                .andExpect(status().isUnauthorized());

        // Estudiante, Colaborador, Tutor -> 403
        for (String token : List.of(studentToken, collaboratorToken, tutorToken)) {
            mockMvc.perform(get("/api/notificaciones/destinatarios")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        // Coordinador -> 200 sin exponer teléfonos ni contraseñas
        mockMvc.perform(get("/api/notificaciones/destinatarios")
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        // Profesor de asignatura -> solo su alcance (incluye student, excluye otherStudent)
        mockMvc.perform(get("/api/notificaciones/destinatarios")
                        .header("Authorization", "Bearer " + professorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(student.getEmail())))
                .andExpect(jsonPath("$[*].email", not(hasItem(otherStudent.getEmail()))));
    }

    // =========================================================================
    // F02: Coordinación puede leer planificaciones supervisadas; ajenos reciben 403
    // =========================================================================
    @Test
    void f02_coordinatorCanReadSupervisedLessonPlans_unrelatedUsersStillDenied() throws Exception {
        mockMvc.perform(get("/api/portafolio/asignacion/" + assignment.getId() + "/planificaciones")
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portafolio/asignacion/" + assignment.getId() + "/planificaciones")
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/portafolio/asignacion/" + assignment.getId() + "/planificaciones")
                        .header("Authorization", "Bearer " + otherTutorToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // F08 y F09: Importación masiva vacía rechazada y errores parciales expuestos
    // =========================================================================
    @Test
    void f08_incompleteCsvAndTxtFilesAreRejectedWithBadRequest() throws Exception {
        MockMultipartFile threeColCsv = new MockMultipartFile(
                "archivo", "incompleto.csv", "text/csv",
                "Nombre,Correo,Rol\nAlumno Malo,malo.f08@ubb.cl,Estudiante\n".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/usuarios/carga-masiva")
                        .file(threeColCsv)
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isBadRequest());
        assertNull(userRepository.findByEmail("malo.f08@ubb.cl"));

        MockMultipartFile txtFile = new MockMultipartFile(
                "archivo", "notas.txt", "text/plain",
                "Solo una linea de texto cualquiera".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/usuarios/carga-masiva")
                        .file(txtFile)
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void f09_partialCsvImportReturnsDetailedCountsAndRowErrors() throws Exception {
        String csv = "Nombre,Correo,Contraseña,Rol\n"
                + "Alumno Valido F09,valido.f09@ubb.cl,Pass123!,Estudiante\n"
                + "Alumno Invalido F09,invalido.f09@ubb.cl,Pass123!,RolInexistente\n";
        MockMultipartFile mixedCsv = new MockMultipartFile(
                "archivo", "mixto.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/usuarios/carga-masiva")
                        .file(mixedCsv)
                        .header("Authorization", "Bearer " + coordinatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(2)))
                .andExpect(jsonPath("$.createdCount", is(1)))
                .andExpect(jsonPath("$.failureCount", is(1)))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]", containsString("Fila 3")));

        assertNotNull(userRepository.findByEmail("valido.f09@ubb.cl"));
        assertNull(userRepository.findByEmail("invalido.f09@ubb.cl"));
    }

    // =========================================================================
    // F12: Serialización UTC explícita en finalizedAt
    // =========================================================================
    @Test
    void f12_evaluationFinalizedAtSerializesWithUtcOffsetZ() throws Exception {
        Evaluation ev = new Evaluation();
        ev.setFinalizedAt(LocalDateTime.of(2026, 11, 24, 22, 48, 10));
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(ev);
        assertTrue(json.contains("\"finalizedAt\":\"2026-11-24T22:48:10Z\""),
                "Se esperaba finalizedAt con sufijo UTC Z, pero se obtuvo: " + json);
    }

    // =========================================================================
    // F13: Archivo de evidencia prohibido devuelve 400 (no 500)
    // =========================================================================
    @Test
    void f13_disallowedEvidenceExtensionReturns400Not500() throws Exception {
        MockMultipartFile exe = new MockMultipartFile(
                "file", "qa.exe", "application/octet-stream", "MZ-fake-exe".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/portafolio/evidencias/upload")
                        .file(exe)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Tipo de archivo no permitido")));
    }
}
