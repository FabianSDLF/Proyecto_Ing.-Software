package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioExportServiceTest {
    @Mock AssignmentScope scope;
    @Mock PortfolioDocumentRepository documents;
    @Mock EvaluationRepository evaluations;
    @Mock EvaluationCriteriaRepository criteria;
    @Mock StorageService storage;
    @InjectMocks PortfolioExportService service;

    @Test void csvIncludesPublishedGradesAndEvidenceButNotDrafts() throws Exception {
        PracticeAssignment a = new PracticeAssignment(); a.setId(1L);
        User student = new User(); student.setFullName("=FORMULA;\"apellido\""); a.setStudent(student);
        when(scope.visible("teacher", null, false)).thenReturn(List.of(a));
        PortfolioDocument d = new PortfolioDocument(); d.setId(7L); d.setFileName("informe.pdf");
        when(documents.findByPracticeAssignment_Id(1L)).thenReturn(List.of(d));
        Evaluation published = new Evaluation(); published.setId(3L); published.setScore(6.5f); published.setIsFinalized(true);
        Evaluation draft = new Evaluation(); draft.setScore(2f); draft.setQualitativeComments("BORRADOR_PRIVADO"); draft.setIsFinalized(false);
        when(evaluations.findByPracticeAssignmentId(1L)).thenReturn(List.of(draft, published));
        EvaluationCriteria c = new EvaluationCriteria(); c.setCriteriaName("Indicador real"); c.setPoints(6f);
        when(criteria.findByEvaluationId(3L)).thenReturn(List.of(c));
        StringWriter output = new StringWriter(); service.csv("teacher", null, null, false, output);
        String csv = output.toString();
        assertTrue(csv.contains("6.5")); assertTrue(csv.contains("Indicador real"));
        assertTrue(csv.contains("/api/portafolio/documentos/7/descargar"));
        assertFalse(csv.contains("BORRADOR_PRIVADO")); assertTrue(csv.contains("'=FORMULA;\"\"apellido\"\""));
    }

    @Test void zipContainsOnlyOwnCourseAndSanitizesEntryNames() throws Exception {
        User teacher = new User(); teacher.setEmail("teacher"); Role role = new Role(); role.setName("Profesor de Asignatura"); teacher.setRole(role);
        PracticeAssignment own = new PracticeAssignment(); own.setId(1L); Subject subject = new Subject(); subject.setProfessor(teacher); own.setSubject(subject);
        PracticeAssignment other = new PracticeAssignment(); other.setId(2L);
        when(scope.user("teacher")).thenReturn(teacher);
        when(scope.visible("teacher", null, false)).thenReturn(List.of(own, other));
        PortfolioDocument document = new PortfolioDocument(); document.setId(3L); document.setFileName("../informe.pdf"); document.setStoredFileName("safe.pdf");
        when(documents.findByPracticeAssignment_IdAndTypeIn(1L, List.of(DocumentType.INFORME_FINAL))).thenReturn(List.of(document));
        when(storage.loadAsResource("safe.pdf")).thenReturn(new ByteArrayResource("test".getBytes()));
        ByteArrayOutputStream output = new ByteArrayOutputStream(); service.zip("teacher", null, null, false, output);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            ZipEntry entry = zip.getNextEntry(); assertNotNull(entry); assertFalse(entry.getName().contains(".."));
            assertEquals("test", new String(zip.readAllBytes())); assertNull(zip.getNextEntry());
        }
        verify(documents, never()).findByPracticeAssignment_IdAndTypeIn(eq(2L), any());
    }
}
