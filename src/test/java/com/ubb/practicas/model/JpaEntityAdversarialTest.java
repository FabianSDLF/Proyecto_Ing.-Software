package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Test 1: JPA Entity Behavior & Hygiene")
public class JpaEntityAdversarialTest {

    @Test
    @DisplayName("LessonPlan.toString() with parentPlan cycle does NOT cause StackOverflowError")
    void testLessonPlanToStringCycleDoesNotCauseStackOverflow() {
        LessonPlan parent = new LessonPlan();
        parent.setId(1L);
        parent.setSchoolGrade("8vo Basico");
        parent.setStatus(PlanStatus.APROBADO);

        LessonPlan child = new LessonPlan();
        child.setId(2L);
        child.setSchoolGrade("8vo Basico");
        child.setStatus(PlanStatus.BORRADOR);
        child.setParentPlan(parent);

        // Circular reference: parent also references child
        parent.setParentPlan(child);

        // Self reference: child references child
        LessonPlan selfRef = new LessonPlan();
        selfRef.setId(3L);
        selfRef.setParentPlan(selfRef);

        // Deep chain of 1000 nested plans
        LessonPlan current = new LessonPlan();
        current.setId(100L);
        for (long i = 101; i <= 1100; i++) {
            LessonPlan next = new LessonPlan();
            next.setId(i);
            next.setParentPlan(current);
            current = next;
        }
        final LessonPlan deepChainPlan = current;

        assertDoesNotThrow(() -> {
            String childStr = child.toString();
            assertNotNull(childStr);
            assertTrue(childStr.contains("id=2"));
            assertFalse(childStr.contains("parentPlan"));

            String parentStr = parent.toString();
            assertNotNull(parentStr);
            assertTrue(parentStr.contains("id=1"));
            assertFalse(parentStr.contains("parentPlan"));

            String selfRefStr = selfRef.toString();
            assertNotNull(selfRefStr);
            assertTrue(selfRefStr.contains("id=3"));
            assertFalse(selfRefStr.contains("parentPlan"));

            String deepStr = deepChainPlan.toString();
            assertNotNull(deepStr);
            assertTrue(deepStr.contains("id=1100"));
        }, "Calling toString() on recursive LessonPlan structures must not trigger StackOverflowError");
    }

    @Test
    @DisplayName("User.toString() does NOT leak passwordHash")
    void testUserToStringDoesNotExposePasswordHash() {
        String sensitiveHash = "$2a$12$e8Y/P7ZXgqF4VjTjL9Z2Ou1jK6R3qX7bV9M8aK5wP2Y8nZ7mQ6lWe";
        User user = new User();
        user.setId(42L);
        user.setFullName("Profesor Evaluador");
        user.setEmail("profesor@ubb.cl");
        user.setPhone("+56912345678");
        user.setPasswordHash(sensitiveHash);
        user.setActive(true);

        String userStr = user.toString();

        assertNotNull(userStr);
        assertFalse(userStr.contains("passwordHash"), "User.toString() must exclude passwordHash field");
        assertFalse(userStr.contains(sensitiveHash), "User.toString() must never contain password hash value");
        assertTrue(userStr.contains("id=42"), "User.toString() should contain id");
        assertTrue(userStr.contains("profesor@ubb.cl"), "User.toString() should contain email");
        assertTrue(userStr.contains("Profesor Evaluador"), "User.toString() should contain fullName");
    }

    @Test
    @DisplayName("equals() and hashCode() respect ID equality in Set and Map collections")
    void testEqualsAndHashCodeIdContractInCollections() {
        User u1 = new User();
        u1.setId(10L);
        u1.setFullName("Nombre A");
        u1.setEmail("a@ubb.cl");

        User u2 = new User();
        u2.setId(10L);
        u2.setFullName("Nombre B Totalmente Diferente");
        u2.setEmail("b@ubb.cl");

        User u3 = new User();
        u3.setId(20L);
        u3.setFullName("Nombre A");
        u3.setEmail("a@ubb.cl");

        // Identity and ID equality
        assertEquals(u1, u1);
        assertEquals(u1, u2, "Entities with identical ID must be equal");
        assertEquals(u1.hashCode(), u2.hashCode(), "Entities with identical ID must have identical hashCode");
        assertNotEquals(u1, u3, "Entities with distinct ID must not be equal");
        assertNotEquals(null, u1);
        assertNotEquals("string", u1);

        // Set collection behavior
        Set<User> set = new HashSet<>();
        assertTrue(set.add(u1));
        assertTrue(set.contains(u2), "Set must contain u2 since ID matches u1");
        assertFalse(set.add(u2), "Set must deduplicate entity with same ID");
        assertEquals(1, set.size());
        assertTrue(set.add(u3));
        assertEquals(2, set.size());

        // Map collection behavior
        Map<User, String> map = new HashMap<>();
        map.put(u1, "payload_user_10");
        assertEquals("payload_user_10", map.get(u2), "Map lookup by equivalent ID must succeed");
        assertNull(map.get(u3));

        // Mutation of non-ID field must not break Set membership
        u1.setFullName("Nombre Modificado");
        u1.setActive(false);
        assertTrue(set.contains(u1), "Set must still contain mutated entity because hashCode is based strictly on ID");
        assertTrue(set.contains(u2));
    }

    @Test
    @DisplayName("All 15 JPA entities strictly adhere to ID-only equals/hashCode contract")
    void testAllEntitiesAdhereToIdEqualityContract() {
        List<Object[]> entityPairs = List.of(
                new Object[]{createAcademicPeriod(1L, "2026-1"), createAcademicPeriod(1L, "2026-2")},
                new Object[]{createChatbotLog(2L, "AI feedback A"), createChatbotLog(2L, "AI feedback B")},
                new Object[]{createChatbotMessage(3L, "Hola"), createChatbotMessage(3L, "Chao")},
                new Object[]{createEvaluation(4L, 6.5f), createEvaluation(4L, 4.0f)},
                new Object[]{createEvaluationCriteria(5L, "Crit A"), createEvaluationCriteria(5L, "Crit B")},
                new Object[]{createLessonPlan(6L, "Tema A"), createLessonPlan(6L, "Tema B")},
                new Object[]{createNotification(7L, "Notif A"), createNotification(7L, "Notif B")},
                new Object[]{createPasswordResetToken(8L, "tok-1"), createPasswordResetToken(8L, "tok-2")},
                new Object[]{createPortfolioDocument(9L, "docA.pdf"), createPortfolioDocument(9L, "docB.docx")},
                new Object[]{createPracticeAssignment(10L, AssignmentStatus.PENDIENTE), createPracticeAssignment(10L, AssignmentStatus.ACTIVA)},
                new Object[]{createPracticeCenter(11L, "Colegio A"), createPracticeCenter(11L, "Colegio B")},
                new Object[]{createRole(12L, "Admin"), createRole(12L, "Estudiante")},
                new Object[]{createStudentPair(13L), createStudentPair(13L)},
                new Object[]{createSubject(14L, "Practica I"), createSubject(14L, "Practica II")},
                new Object[]{createUser(15L, "u1@ubb.cl"), createUser(15L, "u2@ubb.cl")}
        );

        for (Object[] pair : entityPairs) {
            Object e1 = pair[0];
            Object e2 = pair[1];

            assertEquals(e1, e2, "Entity " + e1.getClass().getSimpleName() + " must equate based on ID");
            assertEquals(e1.hashCode(), e2.hashCode(), "Entity " + e1.getClass().getSimpleName() + " must have matching hashCodes");

            Set<Object> set = new HashSet<>();
            set.add(e1);
            assertTrue(set.contains(e2), "Set must contain " + e2.getClass().getSimpleName() + " by ID match");
            assertEquals(1, set.size());
        }
    }

    private AcademicPeriod createAcademicPeriod(Long id, String name) {
        AcademicPeriod p = new AcademicPeriod();
        p.setId(id);
        p.setName(name);
        return p;
    }

    private ChatbotLog createChatbotLog(Long id, String feedback) {
        ChatbotLog l = new ChatbotLog();
        l.setId(id);
        l.setAiFeedback(feedback);
        return l;
    }

    private ChatbotMessage createChatbotMessage(Long id, String message) {
        ChatbotMessage m = new ChatbotMessage();
        m.setId(id);
        m.setContent(message);
        return m;
    }

    private Evaluation createEvaluation(Long id, Float score) {
        Evaluation e = new Evaluation();
        e.setId(id);
        e.setScore(score);
        return e;
    }

    private EvaluationCriteria createEvaluationCriteria(Long id, String description) {
        EvaluationCriteria c = new EvaluationCriteria();
        c.setId(id);
        c.setCriteriaName(description);
        return c;
    }

    private LessonPlan createLessonPlan(Long id, String grade) {
        LessonPlan lp = new LessonPlan();
        lp.setId(id);
        lp.setSchoolGrade(grade);
        return lp;
    }

    private Notification createNotification(Long id, String message) {
        Notification n = new Notification();
        n.setId(id);
        n.setMessage(message);
        return n;
    }

    private PasswordResetToken createPasswordResetToken(Long id, String token) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId(id);
        t.setToken(token);
        return t;
    }

    private PortfolioDocument createPortfolioDocument(Long id, String name) {
        PortfolioDocument d = new PortfolioDocument();
        d.setId(id);
        d.setFileName(name);
        return d;
    }

    private PracticeAssignment createPracticeAssignment(Long id, AssignmentStatus status) {
        PracticeAssignment pa = new PracticeAssignment();
        pa.setId(id);
        pa.setStatus(status);
        return pa;
    }

    private PracticeCenter createPracticeCenter(Long id, String name) {
        PracticeCenter pc = new PracticeCenter();
        pc.setId(id);
        pc.setName(name);
        return pc;
    }

    private Role createRole(Long id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }

    private StudentPair createStudentPair(Long id) {
        StudentPair sp = new StudentPair();
        sp.setId(id);
        return sp;
    }

    private Subject createSubject(Long id, String name) {
        Subject s = new Subject();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private User createUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }
}
