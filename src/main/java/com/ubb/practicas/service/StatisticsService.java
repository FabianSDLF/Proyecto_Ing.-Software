package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final AssignmentScope scope;
    private final EvaluationRepository evaluations;
    private final LessonPlanRepository plans;

    @Transactional(readOnly = true)
    public Map<String, Object> report(String email, Long periodId, boolean history, PracticeCycle cycle, Long subjectId) {
        if (!AssignmentScope.isAdmin(scope.user(email))) throw new AccessDeniedException("Solo administración y coordinación pueden consultar estadísticas institucionales");
        var available = scope.visible(email, null, true);
        var assignments = scope.visible(email, periodId, history).stream()
                .filter(a -> cycle == null || a.getSubject() != null && a.getSubject().getCycle() == cycle)
                .filter(a -> subjectId == null || a.getSubject() != null && subjectId.equals(a.getSubject().getId())).toList();
        Set<Long> students = new HashSet<>(), centers = new HashSet<>(), publishedIds = new HashSet<>();
        List<Map<String, Object>> detail = new ArrayList<>();
        Map<String, List<Float>> grades = new TreeMap<>();
        Map<String, List<Float>> series = new TreeMap<>();
        Map<String, Long> planStates = new TreeMap<>();
        long pending = 0, configured = 0;
        for (var a : assignments) {
            if (a.getStudent() != null) students.add(a.getStudent().getId());
            if (a.getCenter() != null) centers.add(a.getCenter().getId());
            var official = evaluations.findByPracticeAssignmentId(a.getId()).stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsFinalized())).toList();
            for (var e : official) {
                if (!publishedIds.add(e.getId())) continue;
                if (e.getType() != null && e.getScore() != null && Float.isFinite(e.getScore())) {
                    String instrument = e.getType().name() + " · " + Objects.toString(e.getRubricVersion(), "histórica/manual");
                    grades.computeIfAbsent(instrument, k -> new ArrayList<>()).add(e.getScore());
                    String period = a.getPeriod() == null ? "Sin periodo" : a.getPeriod().getName() + " (#" + a.getPeriod().getId() + ")";
                    series.computeIfAbsent(period + " | " + instrument, k -> new ArrayList<>()).add(e.getScore());
                }
            }
            if (professional(a)) {
                configured += 8;
                var types = official.stream().map(Evaluation::getType).filter(t -> t != null && t.isProfessionalRubric()).collect(Collectors.toSet());
                pending += 8 - types.size();
            }
            for (var p : latestPlans(plans.findByPracticeAssignmentId(a.getId()))) {
                planStates.merge(Objects.toString(p.getStatus(), "SIN_ESTADO"), 1L, Long::sum);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("student", a.getStudent() == null ? "Sin estudiante" : a.getStudent().getFullName());
            row.put("subject", a.getSubject() == null ? "Sin asignatura" : a.getSubject().getName());
            row.put("period", a.getPeriod() == null ? "Sin periodo" : a.getPeriod().getName());
            row.put("center", a.getCenter() == null ? "Sin centro" : a.getCenter().getName());
            row.put("publishedEvaluations", official.stream().map(Evaluation::getId).distinct().count());
            row.put("professionalRubrics", professional(a));
            detail.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("students", students.size()); result.put("assignments", assignments.size());
        result.put("centersUsed", centers.size()); result.put("publishedEvaluations", publishedIds.size());
        result.put("configuredInstruments", configured); result.put("pendingEvaluations", pending);
        result.put("gradesByInstrument", averages(grades)); result.put("series", averages(series));
        result.put("latestPlanStates", planStates); result.put("detail", detail);
        result.put("periods", available.stream().map(PracticeAssignment::getPeriod).filter(Objects::nonNull)
                .collect(Collectors.toMap(AcademicPeriod::getId, p -> Map.of("id", p.getId(), "name", p.getName()), (x,y) -> x, TreeMap::new)).values());
        result.put("subjects", available.stream().map(PracticeAssignment::getSubject).filter(Objects::nonNull)
                .collect(Collectors.toMap(Subject::getId, s -> Map.of("id", s.getId(), "name", s.getName()), (x,y) -> x, TreeMap::new)).values());
        return result;
    }

    public static boolean professional(PracticeAssignment a) {
        return a.getSubject() != null && Boolean.TRUE.equals(a.getSubject().getProfessionalRubrics());
    }

    /** Una fila por planificación lógica; las versiones históricas no inflan las aprobaciones. */
    static List<LessonPlan> latestPlans(List<LessonPlan> all) {
        Map<Long, LessonPlan> byId = all.stream().collect(Collectors.toMap(LessonPlan::getId, Function.identity()));
        Map<Long, LessonPlan> latest = new HashMap<>();
        Comparator<LessonPlan> order = Comparator.comparing((LessonPlan p) -> Objects.requireNonNullElse(p.getVersion(), 0)).thenComparing(LessonPlan::getId);
        for (var p : all) {
            var root = p; Set<Long> seen = new HashSet<>();
            while (root.getParentPlanId() != null && seen.add(root.getId()) && byId.containsKey(root.getParentPlanId())) root = byId.get(root.getParentPlanId());
            latest.merge(root.getId(), p, (x, y) -> order.compare(x,y) >= 0 ? x : y);
        }
        return new ArrayList<>(latest.values());
    }

    private static List<Map<String, Object>> averages(Map<String, List<Float>> grouped) {
        return grouped.entrySet().stream().map(entry -> {
            BigDecimal sum = entry.getValue().stream().map(v -> new BigDecimal(v.toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
            return Map.<String, Object>of("instrument", entry.getKey(), "count", entry.getValue().size(),
                    "mean", sum.divide(BigDecimal.valueOf(entry.getValue().size()), 1, RoundingMode.HALF_UP));
        }).toList();
    }
}
