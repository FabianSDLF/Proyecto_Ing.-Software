package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanReviewServiceTest {
    @Mock LessonPlanRepository plans;
    @Mock ChatbotLogRepository reviews;
    @Mock ObjectMapper mapper;
    @InjectMocks PlanReviewService service;
    LessonPlan plan;
    ChatbotLog review;

    @BeforeEach void setup() {
        User student = new User(); student.setEmail("student@example.test");
        PracticeAssignment assignment = new PracticeAssignment(); assignment.setStudent(student); assignment.setStatus(AssignmentStatus.ACTIVA);
        plan = new LessonPlan(); plan.setId(10L); plan.setPracticeAssignment(assignment); plan.setVersion(1); plan.setContentJson("{\"inicio\":\"original\"}");
        review = new ChatbotLog(); review.setAiApproved(true); review.setIsMock(false); review.setContentHash(PlanReviewService.fingerprint(plan));
        when(plans.findById(10L)).thenReturn(Optional.of(plan));
    }

    @Test void unreviewedVersionCannotBeSent() {
        when(reviews.findFirstByLessonPlanIdOrderByReviewDateDesc(10L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.submit(10L, "student@example.test"));
        verify(plans, never()).save(any());
    }

    @Test void modifiedContentInvalidatesPreviousReview() {
        plan.setContentJson("{\"inicio\":\"modificado\"}");
        when(reviews.findFirstByLessonPlanIdOrderByReviewDateDesc(10L)).thenReturn(Optional.of(review));
        assertThrows(IllegalArgumentException.class, () -> service.submit(10L, "student@example.test"));
    }

    @Test void localFallbackCannotCertifyAiValidation() {
        review.setIsMock(true);
        when(reviews.findFirstByLessonPlanIdOrderByReviewDateDesc(10L)).thenReturn(Optional.of(review));
        assertThrows(IllegalArgumentException.class, () -> service.submit(10L, "student@example.test"));
    }

    @Test void currentApprovedAiReviewAllowsTeacherSubmission() {
        when(reviews.findFirstByLessonPlanIdOrderByReviewDateDesc(10L)).thenReturn(Optional.of(review));
        when(plans.save(plan)).thenReturn(plan);
        assertEquals(PlanStatus.EN_REVISION_DOCENTE, service.submit(10L, "student@example.test").getStatus());
    }
}
