package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {
    AssignmentScope scope = mock(AssignmentScope.class);
    EvaluationRepository evaluations = mock(EvaluationRepository.class);
    LessonPlanRepository plans = mock(LessonPlanRepository.class);
    StatisticsService service = new StatisticsService(scope, evaluations, plans);

    PracticeAssignment assignment(long id, boolean professional) {
        User student = new User(); student.setId(1L); student.setFullName("Estudiante");
        Subject subject = new Subject(); subject.setId(id); subject.setName("Asignatura"); subject.setCycle(PracticeCycle.AVANZADO); subject.setProfessionalRubrics(professional);
        PracticeCenter center = new PracticeCenter(); center.setId(10L); center.setName("Centro");
        PracticeAssignment a = new PracticeAssignment(); a.setId(id); a.setSubject(subject); a.setStudent(student); a.setCenter(center);
        return a;
    }
    void authority() {
        User user = new User(); user.setRole(new Role(1L, "Administrador"));
        when(scope.user("admin")).thenReturn(user);
    }
    Evaluation evaluation(long id, boolean published, EvaluationType type, float score) {
        Evaluation e = new Evaluation(); e.setId(id); e.setIsFinalized(published); e.setType(type); e.setScore(score); e.setRubricVersion("2025-2-v1"); return e;
    }

    @Test void distinctStudentsPublishedIdsAndOnlyConfiguredPending() {
        authority(); var a = assignment(1, true); var b = assignment(2, false);
        when(scope.visible("admin", null, true)).thenReturn(List.of(a,b));
        when(scope.visible("admin", null, false)).thenReturn(List.of(a,b));
        var published = evaluation(1, true, EvaluationType.COLAB_ESP_1, 5);
        when(evaluations.findByPracticeAssignmentId(1L)).thenReturn(List.of(published, published, evaluation(2, false, EvaluationType.TUTOR_ESP_1, 7)));
        when(evaluations.findByPracticeAssignmentId(2L)).thenReturn(List.of(evaluation(3, true, EvaluationType.RUBRICA_INFORME, 6)));
        var report = service.report("admin", null, false, null, null);
        assertEquals(1, report.get("students")); assertEquals(2, report.get("assignments"));
        assertEquals(1, report.get("centersUsed")); assertEquals(2, report.get("publishedEvaluations"));
        assertEquals(8L, report.get("configuredInstruments")); assertEquals(7L, report.get("pendingEvaluations"));
        assertEquals(2, ((List<?>)report.get("gradesByInstrument")).size());
    }

    @Test void filtersReduceDenominatorAndDoNotExpandScope() {
        authority(); var a=assignment(1, true); var b=assignment(2, false);
        when(scope.visible("admin", null, true)).thenReturn(List.of(a,b));
        when(scope.visible("admin", 5L, false)).thenReturn(List.of(a,b));
        var report=service.report("admin",5L,false,PracticeCycle.AVANZADO,2L);
        assertEquals(1,report.get("assignments")); assertEquals(0L,report.get("configuredInstruments"));
        verify(evaluations,never()).findByPracticeAssignmentId(1L);
    }

    @Test void rejectTeacherEvenIfFrontendHidesPanel() {
        User teacher=new User();teacher.setRole(new Role(1L,"Tutor"));when(scope.user("tutor")).thenReturn(teacher);
        assertThrows(AccessDeniedException.class,()->service.report("tutor",null,true,null,null));
        verifyNoInteractions(evaluations,plans);
    }

    @Test void countLatestLogicalPlanAndSeparateAiFromTeacherApproval() {
        LessonPlan a=new LessonPlan();a.setId(1L);a.setVersion(1);a.setStatus(PlanStatus.APROBADO);
        LessonPlan b=new LessonPlan();b.setId(2L);b.setVersion(2);b.setParentPlan(a);b.setStatus(PlanStatus.APROBADO_IA);
        LessonPlan c=new LessonPlan();c.setId(3L);c.setVersion(1);c.setStatus(PlanStatus.BORRADOR);
        assertEquals(Set.of(b,c),new HashSet<>(StatisticsService.latestPlans(List.of(a,b,c))));
    }
}
