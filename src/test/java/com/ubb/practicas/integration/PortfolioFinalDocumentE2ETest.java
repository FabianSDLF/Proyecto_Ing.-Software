package com.ubb.practicas.integration;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.PeriodStatus;
import com.ubb.practicas.model.enums.PracticeCycle;
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
 * 4-Tier E2E Opaque-Box Test Suite for HU19 - Subida de Documentos de Término de Práctica.
 * Covers:
 * Tier 1: Feature Coverage (>=5 tests)
 * Tier 2: Boundary & Corner Cases (>=5 tests: 20MB limit, extensions, single final doc rule, replace)
 * Tier 3: Cross-Feature Combinations
 * Tier 4: Real-World Application Scenarios
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("HU19 E2E Integration Suite: Subida de Documentos de Término de Práctica")
public class PortfolioFinalDocumentE2ETest {

    @Autowired
    private MockMvc mockMvc;

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
        student1.setEmail("estudiante.final1@ubb.cl");
        student1.setFullName("Estudiante Finalista Uno");
        student1.setPasswordHash("hash123");
        student1.setActive(true);
        student1.setRole(studentRole);
        student1 = userRepository.save(student1);

        student2 = new User();
        student2.setEmail("estudiante.final2@ubb.cl");
        student2.setFullName("Estudiante Finalista Dos");
        student2.setPasswordHash("hash123");
        student2.setActive(true);
        student2.setRole(studentRole);
        student2 = userRepository.save(student2);

        activePeriod = new AcademicPeriod();
        activePeriod.setName("Periodo Terminal 2026-1");
        activePeriod.setYear(2026);
        activePeriod.setSemesterNumber(1);
        activePeriod.setStartDate(LocalDate.of(2026, 3, 1));
        activePeriod.setEndDate(LocalDate.of(2026, 7, 31));
        activePeriod.setStatus(PeriodStatus.ACTIVO);
        activePeriod = academicPeriodRepository.save(activePeriod);

        center = new PracticeCenter();
        center.setName("Liceo Polivalente San Nicolás");
        center.setCity("San Nicolás");
        center = practiceCenterRepository.save(center);

        subject = new Subject();
        subject.setName("Práctica Profesional de Término");
        subject.setCode("PPT-401");
        subject.setCycle(PracticeCycle.AVANZADO);
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
        portfolioDocumentRepository.deleteAll();
        practiceAssignmentRepository.deleteAll();
        subjectRepository.deleteAll();
        practiceCenterRepository.deleteAll();
        academicPeriodRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Feature Coverage (Primary Behavior / Happy Path)")
    class Tier1FeatureCoverage {

        @Test
        @DisplayName("TC-HU19-T1-01: Subir Informe Final en formato PDF válido retorna HTTP 200/201 y persiste")
        void testUploadInformeFinal_ValidPdf_Returns200() throws Exception {
            byte[] content = "Contenido representativo de informe final de practica pedagógica".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "informe_final_practica.pdf", "application/pdf", content
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.type").value("INFORME_FINAL"))
                    .andExpect(jsonPath("$.fileName").value("informe_final_practica.pdf"));

            List<PortfolioDocument> docs = portfolioDocumentRepository.findAll();
            assertEquals(1, docs.size());
            assertEquals(DocumentType.INFORME_FINAL, docs.get(0).getType());
            assertEquals("informe_final_practica.pdf", docs.get(0).getFileName());
        }

        @Test
        @DisplayName("TC-HU19-T1-02: Subir Proyecto de Intervención en formato DOCX válido retorna HTTP 200/201")
        void testUploadProyectoIntervencion_ValidDocx_Returns200() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "proyecto_intervencion_aula.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "Contenido docx de proyecto".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "PROYECTO_INTERVENCION")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.type").value("PROYECTO_INTERVENCION"))
                    .andExpect(jsonPath("$.fileName").value("proyecto_intervencion_aula.docx"));
        }

        @Test
        @DisplayName("TC-HU19-T1-03: Subir documento final en formato Word tradicional DOC retorna HTTP 200/201")
        void testUploadDocumentoFinal_ValidDocFormat_Returns200() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "informe_legado.doc", "application/msword",
                    "Contenido binario Word doc".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.fileName").value("informe_legado.doc"));
        }

        @Test
        @DisplayName("TC-HU19-T1-04: Consultar documentos finales retorna el documento subido por el estudiante")
        void testGetDocumentosFinales_ReturnsUploadedDocument() throws Exception {
            PortfolioDocument doc = new PortfolioDocument();
            doc.setPracticeAssignment(assignment1);
            doc.setType(DocumentType.INFORME_FINAL);
            doc.setFileName("informe_registrado.pdf");
            doc.setStoredFileName("uuid-informe.pdf");
            doc.setContentType("application/pdf");
            doc.setSizeBytes(2048L);
            doc.setUploadDate(LocalDateTime.now());
            portfolioDocumentRepository.save(doc);

            mockMvc.perform(get("/api/portafolio/documentos-finales")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(empty())))
                    .andExpect(jsonPath("$[0].fileName").value("informe_registrado.pdf"))
                    .andExpect(jsonPath("$[0].type").value("INFORME_FINAL"));
        }

        @Test
        @DisplayName("TC-HU19-T1-05: Verificación de persistencia de metadatos exactos en la base de datos")
        void testDatabaseStateVerification_PersistsCorrectMetadata() throws Exception {
            byte[] fileBytes = new byte[4096];
            MockMultipartFile file = new MockMultipartFile(
                    "file", "informe_audit.pdf", "application/pdf", fileBytes
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            PortfolioDocument saved = portfolioDocumentRepository.findAll().get(0);
            assertNotNull(saved.getId());
            assertEquals("informe_audit.pdf", saved.getFileName());
            assertEquals(DocumentType.INFORME_FINAL, saved.getType());
            assertEquals(assignment1.getId(), saved.getPracticeAssignment().getId());
            assertNotNull(saved.getStoredFileName());
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2 - Boundary & Corner Cases")
    class Tier2BoundaryCornerCases {

        @Test
        @DisplayName("TC-HU19-T2-01: Rechazar archivo que supere el límite de 20 MB (20 MB + 1 KB)")
        void testUpload_FileExceeds20MB_Returns400BadRequest() throws Exception {
            // Tamaño 20 MB + 1024 bytes
            int sizeInBytes = 20 * 1024 * 1024 + 1024;
            MockMultipartFile oversizedFile = new MockMultipartFile(
                    "file", "gigante.pdf", "application/pdf", new byte[100]
            ) {
                @Override
                public long getSize() {
                    return sizeInBytes;
                }
            };

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(oversizedFile)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU19-T2-02: Aceptar archivo en el límite permitido de 20 MB (ej. 19.9 MB)")
        void testUpload_FileWithin20MBBoundary_Accepted() throws Exception {
            int sizeInBytes = 19 * 1024 * 1024; // 19 MB
            MockMultipartFile boundaryFile = new MockMultipartFile(
                    "file", "limite_permitido.pdf", "application/pdf", new byte[100]
            ) {
                @Override
                public long getSize() {
                    return sizeInBytes;
                }
            };

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(boundaryFile)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());
        }

        @Test
        @DisplayName("TC-HU19-T2-03: Rechazar formato no permitido: imagen PNG (.png)")
        void testUpload_InvalidExtension_Png_Returns400BadRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "evidencia_foto.png", "image/png", "png data".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU19-T2-04: Rechazar formato no permitido: ejecutable (.exe)")
        void testUpload_InvalidExtension_Exe_Returns400BadRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malware.exe", "application/octet-stream", "malicious".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU19-T2-05: Rechazar formato no permitido: texto plano (.txt)")
        void testUpload_InvalidExtension_Txt_Returns400BadRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "notas.txt", "text/plain", "texto plano".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU19-T2-06: Rechazar segundo documento final sin confirmación de reemplazo (409 Conflict)")
        void testUpload_DuplicateFinalDocumentWithoutConfirmation_Returns409Conflict() throws Exception {
            // 1. Primer documento final ya registrado
            PortfolioDocument doc1 = new PortfolioDocument();
            doc1.setPracticeAssignment(assignment1);
            doc1.setType(DocumentType.INFORME_FINAL);
            doc1.setFileName("primer_informe.pdf");
            doc1.setStoredFileName("uuid-1.pdf");
            doc1.setUploadDate(LocalDateTime.now());
            portfolioDocumentRepository.save(doc1);

            // 2. Intento de subir un segundo documento sin confirmación (replace=false)
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "segundo_informe.pdf", "application/pdf", "data".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file2)
                            .param("type", "INFORME_FINAL")
                            .param("replace", "false")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-HU19-T2-07: Rechazar documento final de diferente tipo (PROYECTO_INTERVENCION) sin confirmación si ya existe INFORME_FINAL")
        void testUpload_CrossTypeDuplicateWithoutConfirmation_Returns409Conflict() throws Exception {
            PortfolioDocument doc1 = new PortfolioDocument();
            doc1.setPracticeAssignment(assignment1);
            doc1.setType(DocumentType.INFORME_FINAL);
            doc1.setFileName("primer_informe.pdf");
            doc1.setStoredFileName("uuid-1.pdf");
            doc1.setUploadDate(LocalDateTime.now());
            portfolioDocumentRepository.save(doc1);

            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "proyecto.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "data".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file2)
                            .param("type", "PROYECTO_INTERVENCION")
                            .param("replace", "false")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-HU19-T2-08: Confirmación de reemplazo (replace=true) sustituye el documento final existente")
        void testUpload_ReplacementConfirmed_ReplacesExistingFile() throws Exception {
            PortfolioDocument doc1 = new PortfolioDocument();
            doc1.setPracticeAssignment(assignment1);
            doc1.setType(DocumentType.INFORME_FINAL);
            doc1.setFileName("informe_antiguo.pdf");
            doc1.setStoredFileName("uuid-antiguo.pdf");
            doc1.setUploadDate(LocalDateTime.now().minusDays(3));
            portfolioDocumentRepository.save(doc1);

            MockMultipartFile nuevoFile = new MockMultipartFile(
                    "file", "informe_actualizado.pdf", "application/pdf", "nuevo contenido".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(nuevoFile)
                            .param("type", "INFORME_FINAL")
                            .param("replace", "true")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.fileName").value("informe_actualizado.pdf"));

            // Debe existir solo 1 documento final para esta asignación
            List<PortfolioDocument> docs = portfolioDocumentRepository.findAll();
            assertEquals(1, docs.size());
            assertEquals("informe_actualizado.pdf", docs.get(0).getFileName());
        }

        @Test
        @DisplayName("TC-HU19-T2-09: Rechazar archivo vacío de 0 bytes")
        void testUpload_ZeroByteFile_Returns400BadRequest() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "vacio.pdf", "application/pdf", new byte[0]
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(emptyFile)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC-HU19-T2-10: Extensiones en mayúsculas (.PDF, .DOCX) son aceptadas")
        void testUpload_UppercaseExtension_Accepted() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "INFORME_FINAL_MAYUSCULA.PDF", "application/pdf", "data".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.fileName").value("INFORME_FINAL_MAYUSCULA.PDF"));
        }
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // =========================================================================

    @Nested
    @DisplayName("Tier 3 - Cross-Feature Combinations")
    class Tier3CrossFeatureCombinations {

        @Test
        @DisplayName("TC-HU19-T3-01: Aislamiento entre estudiantes: documento final de estudiante 1 no genera conflicto para estudiante 2")
        void testFinalDocumentIsolation_BetweenStudents() throws Exception {
            // Estudiante 1 sube su informe
            PortfolioDocument doc1 = new PortfolioDocument();
            doc1.setPracticeAssignment(assignment1);
            doc1.setType(DocumentType.INFORME_FINAL);
            doc1.setFileName("informe_est1.pdf");
            doc1.setStoredFileName("uuid-1.pdf");
            portfolioDocumentRepository.save(doc1);

            // Estudiante 2 debe poder subir su informe sin conflicto
            MockMultipartFile fileEst2 = new MockMultipartFile(
                    "file", "informe_est2.pdf", "application/pdf", "data est2".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(fileEst2)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student2.getEmail()))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath("$.fileName").value("informe_est2.pdf"));

            assertEquals(2, portfolioDocumentRepository.count());
        }

        @Test
        @DisplayName("TC-HU19-T3-02: La subida de documento final coexiste armónicamente con evidencias previas sin colisión")
        void testFinalDocumentCoexistsWithEvidencias() throws Exception {
            PortfolioDocument evidencia = new PortfolioDocument();
            evidencia.setPracticeAssignment(assignment1);
            evidencia.setType(DocumentType.EVIDENCIA_APRENDIZAJE);
            evidencia.setFileName("evidencia_semanal.pdf");
            evidencia.setStoredFileName("uuid-evidencia.pdf");
            portfolioDocumentRepository.save(evidencia);

            MockMultipartFile fileFinal = new MockMultipartFile(
                    "file", "informe_final.pdf", "application/pdf", "data final".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(fileFinal)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            assertEquals(2, portfolioDocumentRepository.count());
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // =========================================================================

    @Nested
    @DisplayName("Tier 4 - Real-World Application Scenarios")
    class Tier4RealWorldScenarios {

        @Test
        @DisplayName("TC-HU19-T4-01: Ciclo de vida real de entrega de documento final de titulación con reemplazo confirmado")
        void testScenario_StudentFullTerminalSubmissionLifecycle() throws Exception {
            // 1. Estudiante sube primera versión de Informe Final
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "informe_v1.pdf", "application/pdf", "version 1".getBytes()
            );
            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file1)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            // 2. Intento de subida accidental sin confirmación -> 409 Conflict
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "informe_v2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "version 2".getBytes()
            );
            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file2)
                            .param("type", "PROYECTO_INTERVENCION")
                            .param("replace", "false")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isConflict());

            // 3. Confirmación de reemplazo por el estudiante -> 200 OK
            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(file2)
                            .param("type", "PROYECTO_INTERVENCION")
                            .param("replace", "true")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().is2xxSuccessful());

            // 4. Verificación en consulta de documentos finales del estudiante
            mockMvc.perform(get("/api/portafolio/documentos-finales")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].fileName").value("informe_v2.docx"))
                    .andExpect(jsonPath("$[0].type").value("PROYECTO_INTERVENCION"));
        }

        @Test
        @DisplayName("TC-HU19-T4-02: Resistencia adversarial: rechazar doble extensión engañosa como 'informe.pdf.exe'")
        void testScenario_AdversarialUploads_DoubleExtension() throws Exception {
            MockMultipartFile sneakyFile = new MockMultipartFile(
                    "file", "informe.pdf.exe", "application/octet-stream", "malicious payload".getBytes()
            );

            mockMvc.perform(multipart("/api/portafolio/documentos-finales/upload")
                            .file(sneakyFile)
                            .param("type", "INFORME_FINAL")
                            .principal(() -> student1.getEmail()))
                    .andExpect(status().isBadRequest());
        }
    }
}
