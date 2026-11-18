package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

@Service
@RequiredArgsConstructor
public class PortfolioExportService {
    private final AssignmentScope scope;
    private final PortfolioDocumentRepository documents;
    private final EvaluationRepository evaluations;
    private final EvaluationCriteriaRepository criteria;
    private final StorageService storage;

    @Transactional(readOnly = true)
    public void zip(String email, Long periodId, Long subjectId, boolean history, OutputStream output) throws IOException {
        scope.requireExport(email);
        List<PracticeAssignment> visible = scope.visible(email, periodId, history);
        // HU34: informes del curso propio, no de todo el sistema ni de alumnos ajenos.
        visible = visible.stream().filter(a -> AssignmentScope.isAdmin(scope.user(email)) || a.isProfessor(email))
                .filter(a -> subjectId == null || a.getSubject() != null && subjectId.equals(a.getSubject().getId())).toList();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (PracticeAssignment a : visible) {
                for (PortfolioDocument d : documents.findByPracticeAssignment_IdAndTypeIn(a.getId(), List.of(DocumentType.INFORME_FINAL))) {
                    String name = "Informes/" + a.getId() + "_" + d.getId() + "_" + safeName(d.getFileName());
                    try {
                        var resource = storage.loadAsResource(d.getStoredFileName());
                        try (InputStream in = resource.getInputStream()) {
                            zip.putNextEntry(new ZipEntry(name));
                            in.transferTo(zip);
                            zip.closeEntry();
                        }
                    } catch (com.ubb.practicas.exception.StorageFileNotFoundException | SecurityException ex) {
                        zip.putNextEntry(new ZipEntry(name + ".faltante.txt"));
                        zip.write("Archivo no disponible. Contacte al administrador indicando el identificador del documento.".getBytes(StandardCharsets.UTF_8));
                        zip.closeEntry();
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public void csv(String email, Long periodId, Long subjectId, boolean history, Writer out) throws IOException {
        scope.requireExport(email);
        row(out, "Asignacion", "Estudiante", "Correo", "Asignatura", "Ciclo", "Periodo", "Centro", "Estado practica",
                "Evaluacion", "Instrumento", "Nota publicada", "Observaciones", "Indicador", "Puntos", "Justificacion", "Evidencias");
        for (PracticeAssignment a : scope.visible(email, periodId, false)) {
            if (subjectId != null && (a.getSubject() == null || !subjectId.equals(a.getSubject().getId()))) continue;
            String evidence = documents.findByPracticeAssignment_Id(a.getId()).stream()
                    .map(d -> "/api/portafolio/documentos/" + d.getId() + "/descargar [" + d.getFileName() + "]")
                    .collect(java.util.stream.Collectors.joining(" | "));
            List<Evaluation> published = evaluations.findByPracticeAssignmentId(a.getId()).stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsFinalized())).toList();
            if (published.isEmpty()) exportRow(out, a, null, null, evidence);
            for (Evaluation e : published) {
                List<EvaluationCriteria> details = criteria.findByEvaluationId(e.getId());
                if (details.isEmpty()) exportRow(out, a, e, null, evidence);
                for (EvaluationCriteria c : details) exportRow(out, a, e, c, evidence);
            }
        }
        out.flush();
    }

    private void exportRow(Writer out, PracticeAssignment a, Evaluation e, EvaluationCriteria c, String evidence) throws IOException {
        row(out, a.getId(), a.getStudent() == null ? "" : a.getStudent().getFullName(),
                a.getStudent() == null ? "" : a.getStudent().getEmail(), a.getSubject() == null ? "" : a.getSubject().getName(),
                a.getSubject() == null ? "" : a.getSubject().getCycle(), a.getPeriod() == null ? "Sin periodo" : a.getPeriod().getName(),
                a.getCenter() == null ? "" : a.getCenter().getName(), a.getStatus(), e == null ? "" : e.getId(),
                e == null ? "" : e.getType(), e == null ? "" : e.getScore(), e == null ? "" : e.getQualitativeComments(),
                c == null ? "" : c.getCriteriaName(), c == null ? "" : c.getPoints(), c == null ? "" : c.getJustification(), evidence);
    }

    static String safeName(String name) {
        return name == null ? "informe" : name.replaceAll("[\\\\/\\p{Cntrl}:]", "_").replace("..", "_");
    }

    static void row(Writer writer, Object... fields) throws IOException {
        List<String> escaped = new ArrayList<>();
        for (Object field : fields) {
            String text = field == null ? "" : field.toString();
            // Evitar fórmulas al abrir el CSV en una hoja de cálculo.
            if (text.stripLeading().matches("^[=+@-].*")) text = "'" + text;
            escaped.add("\"" + text.replace("\"", "\"\"") + "\"");
        }
        writer.write(String.join(";", escaped) + "\r\n");
    }
}
