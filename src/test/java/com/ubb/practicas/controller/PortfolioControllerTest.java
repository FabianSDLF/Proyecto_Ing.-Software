package com.ubb.practicas.controller;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.*;
import com.ubb.practicas.service.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private LessonPlanRepository lessonPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StorageService storageService;

    private User student;
    private User otherStudent;
    private User admin;
    private PracticeAssignment assignment;
    private PortfolioDocument document;

    @BeforeEach
    public void setup() {
        cleanup();

        Role adminRole = roleRepository.findByName("Administrador");
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("Administrador");
            adminRole = roleRepository.save(adminRole);
        }

        Role studentRole = roleRepository.findByName("Estudiante");
        if (studentRole == null) {
            studentRole = new Role();
            studentRole.setName("Estudiante");
            studentRole = roleRepository.save(studentRole);
        }

        student = new User();
        student.setEmail("estudiante.test@ubb.cl");
        student.setFullName("Estudiante Test");
        student.setPasswordHash("hash");
        student.setActive(true);
        student.setRole(studentRole);
        student = userRepository.save(student);

        otherStudent = new User();
        otherStudent.setEmail("otro.estudiante@ubb.cl");
        otherStudent.setFullName("Otro Estudiante");
        otherStudent.setPasswordHash("hash");
        otherStudent.setActive(true);
        otherStudent.setRole(studentRole);
        otherStudent = userRepository.save(otherStudent);

        admin = new User();
        admin.setEmail("admin.test@ubb.cl");
        admin.setFullName("Admin Test");
        admin.setPasswordHash("hash");
        admin.setActive(true);
        admin.setRole(adminRole);
        admin = userRepository.save(admin);

        assignment = new PracticeAssignment();
        assignment.setStudent(student);
        assignment.setStatus(AssignmentStatus.ACTIVA);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment = practiceAssignmentRepository.save(assignment);

        MockMultipartFile initialFile = new MockMultipartFile("file", "evidencia.pdf", "application/pdf", "test-content".getBytes());
        String storedFileName = storageService.storeFile(initialFile);

        document = new PortfolioDocument();
        document.setPracticeAssignment(assignment);
        document.setType(DocumentType.EVIDENCIA_APRENDIZAJE);
        document.setFileName("evidencia.pdf");
        document.setStoredFileName(storedFileName);
        document.setContentType("application/pdf");
        document.setSizeBytes(1024L);
        document.setUploadDate(LocalDateTime.now());
        document = portfolioDocumentRepository.save(document);
    }

    @AfterEach
    public void cleanup() {
        lessonPlanRepository.deleteAll();
        portfolioDocumentRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testDescargarDocumentoPorPropietarioPermitido() throws Exception {
        mockMvc.perform(get("/api/portafolio/documentos/" + document.getId() + "/descargar")
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"evidencia.pdf\""));
    }

    @Test
    public void testDescargarDocumentoPorAdminPermitido() throws Exception {
        mockMvc.perform(get("/api/portafolio/documentos/" + document.getId() + "/descargar")
                .principal(() -> admin.getEmail()))
                .andExpect(status().isOk());
    }

    @Test
    public void testDescargarDocumentoPorUsuarioAjenoRetorna403() throws Exception {
        mockMvc.perform(get("/api/portafolio/documentos/" + document.getId() + "/descargar")
                .principal(() -> otherStudent.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testEliminarDocumentoPorUsuarioAjenoRetorna403() throws Exception {
        mockMvc.perform(delete("/api/portafolio/documentos/" + document.getId())
                .principal(() -> otherStudent.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testEliminarDocumentoPorPropietarioPermitido() throws Exception {
        mockMvc.perform(delete("/api/portafolio/documentos/" + document.getId())
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk());
    }

    @Test
    public void testEliminarDocumentoConPlanSinArchivoNoLanzaNPE() throws Exception {
        // Planificación sin archivo adjunto (document = null)
        LessonPlan planSinArchivo = new LessonPlan();
        planSinArchivo.setPracticeAssignment(assignment);
        planSinArchivo.setStatus(PlanStatus.BORRADOR);
        planSinArchivo.setSchoolGrade("8vo Básico");
        planSinArchivo.setMathAxis(MathAxis.GEOMETRIA);
        planSinArchivo.setBaseLearningObjective("OA1");
        planSinArchivo.setDocument(null);
        planSinArchivo.setCreatedAt(LocalDateTime.now());
        planSinArchivo.setUpdatedAt(LocalDateTime.now());
        lessonPlanRepository.save(planSinArchivo);

        // Documento tipo PLANIFICACION con archivo real
        MockMultipartFile planFile = new MockMultipartFile("file", "plan.pdf", "application/pdf", "plan-content".getBytes());
        String planStored = storageService.storeFile(planFile);

        PortfolioDocument docPlan = new PortfolioDocument();
        docPlan.setPracticeAssignment(assignment);
        docPlan.setType(DocumentType.PLANIFICACION);
        docPlan.setFileName("plan.pdf");
        docPlan.setStoredFileName(planStored);
        docPlan.setUploadDate(LocalDateTime.now());
        docPlan = portfolioDocumentRepository.save(docPlan);

        // Eliminar el documento debe ejecutar sin NPE incluso habiendo planes sin documento
        mockMvc.perform(delete("/api/portafolio/documentos/" + docPlan.getId())
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk());
    }

    @Test
    public void testSubirEvidenciaAsignaAPracticaActiva() throws Exception {
        // Asignación finalizada más antigua
        PracticeAssignment finalizada = new PracticeAssignment();
        finalizada.setStudent(student);
        finalizada.setStatus(AssignmentStatus.FINALIZADA);
        finalizada.setCreatedAt(LocalDateTime.now().minusMonths(6));
        practiceAssignmentRepository.save(finalizada);

        MockMultipartFile file = new MockMultipartFile("file", "tarea.pdf", "application/pdf", "dummy data".getBytes());

        mockMvc.perform(multipart("/api/portafolio/evidencias/upload")
                .file(file)
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practiceAssignment.id").value(assignment.getId()));
    }

    @Test
    public void testSubirDocumentoFinal_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "informe.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                .file(file)
                .param("type", "INFORME_FINAL")
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INFORME_FINAL"))
                .andExpect(jsonPath("$.fileName").value("informe.pdf"));
    }

    @Test
    public void testSubirDocumentoFinal_SinConfirmacion_Retorna409() throws Exception {
        PortfolioDocument initialDoc = new PortfolioDocument();
        initialDoc.setPracticeAssignment(assignment);
        initialDoc.setType(DocumentType.INFORME_FINAL);
        initialDoc.setFileName("previo.pdf");
        initialDoc.setStoredFileName("uuid-previo.pdf");
        initialDoc.setUploadDate(LocalDateTime.now());
        portfolioDocumentRepository.save(initialDoc);

        MockMultipartFile file = new MockMultipartFile("file", "nuevo.pdf", "application/pdf", "data".getBytes());

        mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                .file(file)
                .param("type", "INFORME_FINAL")
                .param("replace", "false")
                .principal(() -> student.getEmail()))
                .andExpect(status().isConflict());
    }

    @Test
    public void testSubirDocumentoFinal_ConReemplazo_Retorna200() throws Exception {
        PortfolioDocument initialDoc = new PortfolioDocument();
        initialDoc.setPracticeAssignment(assignment);
        initialDoc.setType(DocumentType.INFORME_FINAL);
        initialDoc.setFileName("previo.pdf");
        initialDoc.setStoredFileName("uuid-previo.pdf");
        initialDoc.setUploadDate(LocalDateTime.now());
        portfolioDocumentRepository.save(initialDoc);

        MockMultipartFile file = new MockMultipartFile("file", "reemplazo.pdf", "application/pdf", "nuevo contenido".getBytes());

        mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                .file(file)
                .param("type", "INFORME_FINAL")
                .param("replace", "true")
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("reemplazo.pdf"));
    }

    @Test
    public void testObtenerDocumentosFinales_RetornaLista() throws Exception {
        PortfolioDocument finalDoc = new PortfolioDocument();
        finalDoc.setPracticeAssignment(assignment);
        finalDoc.setType(DocumentType.INFORME_FINAL);
        finalDoc.setFileName("informe_registrado.pdf");
        finalDoc.setStoredFileName("uuid-reg.pdf");
        finalDoc.setUploadDate(LocalDateTime.now());
        portfolioDocumentRepository.save(finalDoc);

        mockMvc.perform(get("/api/portafolio/documentos-finales")
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("informe_registrado.pdf"))
                .andExpect(jsonPath("$[0].type").value("INFORME_FINAL"));
    }

    @Test
    public void testEliminarDocumentoFinal_Success() throws Exception {
        PortfolioDocument finalDoc = new PortfolioDocument();
        finalDoc.setPracticeAssignment(assignment);
        finalDoc.setType(DocumentType.INFORME_FINAL);
        finalDoc.setFileName("informe_para_borrar.pdf");
        finalDoc.setStoredFileName("uuid-borrar.pdf");
        finalDoc.setUploadDate(LocalDateTime.now());
        finalDoc = portfolioDocumentRepository.save(finalDoc);

        mockMvc.perform(delete("/api/portafolio/documentos-finales/" + finalDoc.getId())
                .principal(() -> student.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Documento final eliminado exitosamente"));
    }
}
