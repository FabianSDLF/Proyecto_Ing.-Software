package com.ubb.practicas.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.EvaluationType;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EvaluationRubricCatalog {

    public static final String CURRENT_RUBRIC_VERSION = "2025-2-v1";
    public static final List<Integer> OFFICIAL_SCALE = List.of(2, 3, 4, 5, 6, 7);

    private final ObjectMapper objectMapper;
    private final AssignmentScope assignmentScope;
    private final PracticeAssignmentRepository assignmentRepository;

    private final Map<EvaluationType, RubricDefinition> catalog = new ConcurrentHashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorDefinition {
        private String id;
        private String code;
        private String criterionCode;
        private String dimension;
        private Integer order;
        private String text;

        public String effectiveCode() {
            if (criterionCode != null && !criterionCode.isBlank()) return criterionCode.trim();
            if (code != null && !code.isBlank()) return code.trim();
            return id != null ? id.trim() : null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RubricDefinition {
        private EvaluationType type;
        private String name;
        private String role;
        private String area;
        private Integer visit;
        private String rubricVersion;
        private List<Integer> scale;
        private Integer indicatorCount;
        private Integer maxPoints;
        private String source;
        private List<IndicatorDefinition> indicators;

        public Map<String, IndicatorDefinition> indicatorMap() {
            Map<String, IndicatorDefinition> map = new LinkedHashMap<>();
            if (indicators != null) {
                int idx = 1;
                for (IndicatorDefinition ind : indicators) {
                    String code = ind.effectiveCode();
                    if (ind.getOrder() == null) {
                        ind.setOrder(idx);
                    }
                    if (ind.getCode() == null) ind.setCode(code);
                    if (ind.getCriterionCode() == null) ind.setCriterionCode(code);
                    if (ind.getId() == null) ind.setId(code);
                    if (code != null) {
                        map.put(code, ind);
                    }
                    idx++;
                }
            }
            return map;
        }
    }

    @PostConstruct
    public void init() {
        loadFromResource("rubrics/collaborator.json");
        loadFromResource("rubrics/tutor.json");
    }

    private void loadFromResource(String resourcePath) {
        try (InputStream in = new ClassPathResource(resourcePath).getInputStream()) {
            List<RubricDefinition> defs = objectMapper.readValue(in, new TypeReference<List<RubricDefinition>>() {});
            for (RubricDefinition def : defs) {
                normalizeDefinition(def);
                catalog.put(def.getType(), def);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el catálogo de pautas desde " + resourcePath, e);
        }
    }

    private void normalizeDefinition(RubricDefinition def) {
        if (def.getRubricVersion() == null || def.getRubricVersion().isBlank()) {
            def.setRubricVersion(CURRENT_RUBRIC_VERSION);
        }
        if (def.getScale() == null || def.getScale().isEmpty()) {
            def.setScale(OFFICIAL_SCALE);
        }
        if (def.getArea() == null && def.getType() != null) {
            def.setArea(def.getType().name().contains("_ORI_") ? "Orientación" : "Especialidad");
        }
        if (def.getVisit() == null && def.getType() != null) {
            def.setVisit(def.getType().name().endsWith("_2") ? 2 : 1);
        }
        List<IndicatorDefinition> indicators = def.getIndicators() != null ? def.getIndicators() : List.of();
        int idx = 1;
        for (IndicatorDefinition ind : indicators) {
            String code = ind.effectiveCode();
            ind.setId(code);
            ind.setCode(code);
            ind.setCriterionCode(code);
            if (ind.getOrder() == null) {
                ind.setOrder(idx);
            }
            idx++;
        }
        def.setIndicatorCount(indicators.size());
        def.setMaxPoints(indicators.size() * 7);
    }

    public Optional<RubricDefinition> findRubric(EvaluationType type) {
        if (type == null) return Optional.empty();
        if (catalog.isEmpty()) {
            init();
        }
        return Optional.ofNullable(catalog.get(type));
    }

    public RubricDefinition requireRubric(EvaluationType type, String requestedVersion) {
        RubricDefinition def = findRubric(type)
                .orElseThrow(() -> new BadRequestException("No existe plantilla oficial configurada para el instrumento: " + type));
        if (requestedVersion != null && !requestedVersion.isBlank() && !def.getRubricVersion().equals(requestedVersion.trim())) {
            throw new BadRequestException("La versión de rúbrica '" + requestedVersion + "' no coincide con la versión oficial vigente '" + def.getRubricVersion() + "'.");
        }
        return def;
    }

    public List<RubricDefinition> getCollaboratorRubrics() {
        if (catalog.isEmpty()) init();
        return List.of(
                catalog.get(EvaluationType.COLAB_ESP_1),
                catalog.get(EvaluationType.COLAB_ORI_1),
                catalog.get(EvaluationType.COLAB_ESP_2),
                catalog.get(EvaluationType.COLAB_ORI_2)
        ).stream().filter(Objects::nonNull).toList();
    }

    public List<RubricDefinition> getTutorRubrics() {
        if (catalog.isEmpty()) init();
        return List.of(
                catalog.get(EvaluationType.TUTOR_ESP_1),
                catalog.get(EvaluationType.TUTOR_ORI_1),
                catalog.get(EvaluationType.TUTOR_ESP_2),
                catalog.get(EvaluationType.TUTOR_ORI_2)
        ).stream().filter(Objects::nonNull).toList();
    }

    public List<RubricDefinition> getAllProfessionalRubrics() {
        List<RubricDefinition> all = new ArrayList<>(getCollaboratorRubrics());
        all.addAll(getTutorRubrics());
        return all;
    }

    public List<RubricDefinition> getApplicableRubrics(String callerEmail, Long assignmentId) {
        User user = assignmentScope.user(callerEmail);
        String role = AssignmentScope.role(user);
        boolean admin = AssignmentScope.isAdmin(user);

        if (assignmentId != null) {
            PracticeAssignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));
            if (!admin && !assignment.hasParticipant(callerEmail)) throw new AccessDeniedException("No pertenece a esta práctica");
            if (!StatisticsService.professional(assignment)) return List.of();
            if (admin || (role.equals("PROFESOR_DE_ASIGNATURA") && assignment.isProfessor(callerEmail))) {
                return getAllProfessionalRubrics();
            }
            if (role.equals("PROFESOR_COLABORADOR") && assignment.isCollaborator(callerEmail)) {
                return getCollaboratorRubrics();
            }
            if (role.equals("TUTOR") && assignment.isTutor(callerEmail)) {
                return getTutorRubrics();
            }
            throw new AccessDeniedException("No tiene autorización para consultar las pautas de evaluación de esta práctica.");
        }

        if (admin || role.equals("PROFESOR_DE_ASIGNATURA")) {
            return getAllProfessionalRubrics();
        }
        if (role.equals("PROFESOR_COLABORADOR")) {
            return getCollaboratorRubrics();
        }
        if (role.equals("TUTOR")) {
            return getTutorRubrics();
        }
        throw new AccessDeniedException("Su rol no tiene acceso al catálogo de pautas de evaluación.");
    }

    /**
     * Reproduce las tablas oficiales 2,0–7,0 al 60% de exigencia con redondeo final HALF_UP a 1 decimal.
     * Si P <= T: nota = 2 + 2 * P / T
     * Si P > T:  nota = 4 + 3 * (P - T) / (M - T)
     */
    public static float calculateOfficialGrade(int totalPoints, int maxPoints) {
        if (maxPoints <= 0) {
            throw new BadRequestException("El puntaje máximo de la pauta no está configurado.");
        }
        if (totalPoints < 0 || totalPoints > maxPoints) {
            throw new BadRequestException("El puntaje total (" + totalPoints + ") está fuera del rango válido [0, " + maxPoints + "].");
        }
        MathContext mc = MathContext.DECIMAL128;
        BigDecimal p = BigDecimal.valueOf(totalPoints);
        BigDecimal m = BigDecimal.valueOf(maxPoints);
        BigDecimal t = m.multiply(new BigDecimal("0.60"), mc);

        BigDecimal unrounded;
        if (p.compareTo(t) <= 0) {
            // 2 + 2 * P / T
            unrounded = BigDecimal.valueOf(2).add(
                    BigDecimal.valueOf(2).multiply(p, mc).divide(t, mc),
                    mc
            );
        } else {
            // 4 + 3 * (P - T) / (M - T)
            BigDecimal num = BigDecimal.valueOf(3).multiply(p.subtract(t, mc), mc);
            BigDecimal den = m.subtract(t, mc);
            unrounded = BigDecimal.valueOf(4).add(num.divide(den, mc), mc);
        }

        BigDecimal rounded = unrounded.setScale(1, RoundingMode.HALF_UP);
        if (rounded.compareTo(new BigDecimal("2.0")) < 0) {
            rounded = new BigDecimal("2.0");
        } else if (rounded.compareTo(new BigDecimal("7.0")) > 0) {
            rounded = new BigDecimal("7.0");
        }
        return rounded.floatValue();
    }

    public static boolean isAllowedPointValue(Float points) {
        if (points == null || !Float.isFinite(points)) return false;
        int intVal = Math.round(points);
        if (points.floatValue() != intVal) return false;
        return OFFICIAL_SCALE.contains(intVal);
    }

    /** Para pruebas de inmutabilidad histórica: permite reemplazar temporalmente una definición en memoria. */
    public void overrideRubricForTest(EvaluationType type, RubricDefinition definition) {
        catalog.put(type, definition);
    }
}
