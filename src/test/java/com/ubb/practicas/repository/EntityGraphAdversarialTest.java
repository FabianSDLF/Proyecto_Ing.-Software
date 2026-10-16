package com.ubb.practicas.repository;

import jakarta.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Test 3: Lazy Loading vs N+1 Queries Prevention")
public class EntityGraphAdversarialTest {

    private static final List<Class<?>> ALL_ENTITY_CLASSES = List.of(
            com.ubb.practicas.model.AcademicPeriod.class,
            com.ubb.practicas.model.ChatbotLog.class,
            com.ubb.practicas.model.ChatbotMessage.class,
            com.ubb.practicas.model.Evaluation.class,
            com.ubb.practicas.model.EvaluationCriteria.class,
            com.ubb.practicas.model.LessonPlan.class,
            com.ubb.practicas.model.Notification.class,
            com.ubb.practicas.model.PasswordResetToken.class,
            com.ubb.practicas.model.PortfolioDocument.class,
            com.ubb.practicas.model.PracticeAssignment.class,
            com.ubb.practicas.model.PracticeCenter.class,
            com.ubb.practicas.model.Role.class,
            com.ubb.practicas.model.StudentPair.class,
            com.ubb.practicas.model.Subject.class,
            com.ubb.practicas.model.User.class
    );

    private static final List<Class<?>> ALL_REPOSITORIES = List.of(
            LessonPlanRepository.class,
            PortfolioDocumentRepository.class,
            PracticeAssignmentRepository.class,
            UserRepository.class,
            SubjectRepository.class,
            RoleRepository.class
    );

    @Test
    @DisplayName("Every @ManyToOne and @OneToOne across all 15 JPA entities MUST be explicitly FetchType.LAZY")
    void testAllToOneRelationshipsAreExplicitlyLazy() {
        int toOneCount = 0;

        for (Class<?> entityClass : ALL_ENTITY_CLASSES) {
            for (Field field : entityClass.getDeclaredFields()) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                if (manyToOne != null) {
                    toOneCount++;
                    assertEquals(FetchType.LAZY, manyToOne.fetch(),
                            String.format("Field %s.%s @ManyToOne must have fetch = FetchType.LAZY to prevent N+1 selects",
                                    entityClass.getSimpleName(), field.getName()));
                }

                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (oneToOne != null) {
                    toOneCount++;
                    assertEquals(FetchType.LAZY, oneToOne.fetch(),
                            String.format("Field %s.%s @OneToOne must have fetch = FetchType.LAZY to prevent N+1 selects",
                                    entityClass.getSimpleName(), field.getName()));
                }
            }
        }

        assertTrue(toOneCount >= 20, "Should have verified at least 20 @ManyToOne/@OneToOne associations across the domain model");
    }

    @Test
    @DisplayName("Every attributePath in repository @EntityGraph annotations resolves to a valid entity property path")
    void testAllEntityGraphAttributePathsAreValid() {
        int graphCount = 0;
        int pathCount = 0;

        for (Class<?> repoClass : ALL_REPOSITORIES) {
            Class<?> entityClass = getDomainClassFromRepository(repoClass);
            assertNotNull(entityClass, "Repository must have a domain entity class: " + repoClass.getSimpleName());

            for (Method method : repoClass.getDeclaredMethods()) {
                EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);
                if (entityGraph != null) {
                    graphCount++;
                    for (String path : entityGraph.attributePaths()) {
                        pathCount++;
                        assertAttributePathExists(entityClass, path, repoClass.getSimpleName() + "." + method.getName());
                    }
                }
            }
        }

        assertTrue(graphCount >= 10, "Should have found at least 10 @EntityGraph annotated repository methods");
        assertTrue(pathCount >= 25, "Should have verified at least 25 individual attribute paths");
    }

    @Test
    @DisplayName("PracticeAssignmentRepository queries use EntityGraph to fetch 8-level relationship tree in a single query")
    void testPracticeAssignmentRepositoryGraphCompleteness() {
        Method findAllMethod = findMethod(PracticeAssignmentRepository.class, "findAll");
        assertNotNull(findAllMethod);
        EntityGraph egFindAll = findAllMethod.getAnnotation(EntityGraph.class);
        assertNotNull(egFindAll, "findAll must declare @EntityGraph");

        Set<String> paths = new HashSet<>(Arrays.asList(egFindAll.attributePaths()));
        assertTrue(paths.contains("student"), "findAll must fetch student");
        assertTrue(paths.contains("student.role"), "findAll must fetch student.role");
        assertTrue(paths.contains("subject"), "findAll must fetch subject");
        assertTrue(paths.contains("subject.professor"), "findAll must fetch subject.professor");
        assertTrue(paths.contains("center"), "findAll must fetch center");
        assertTrue(paths.contains("collaborator"), "findAll must fetch collaborator");
        assertTrue(paths.contains("tutor"), "findAll must fetch tutor");
        assertTrue(paths.contains("period"), "findAll must fetch period");
    }

    @Test
    @DisplayName("PortfolioDocumentRepository and LessonPlanRepository use JOIN FETCH or EntityGraph on critical methods")
    void testPortfolioAndLessonPlanRepositoryEagerJoinStrategies() {
        // LessonPlanRepository findById
        Method lpFindById = findMethod(LessonPlanRepository.class, "findById", Long.class);
        assertNotNull(lpFindById);
        EntityGraph lpEg = lpFindById.getAnnotation(EntityGraph.class);
        assertNotNull(lpEg, "LessonPlanRepository.findById must declare @EntityGraph");
        List<String> lpPaths = Arrays.asList(lpEg.attributePaths());
        assertTrue(lpPaths.contains("practiceAssignment"));
        assertTrue(lpPaths.contains("practiceAssignment.student"));
        assertTrue(lpPaths.contains("document"));

        // LessonPlanRepository query with JOIN FETCH
        Method lpByEmail = findMethod(LessonPlanRepository.class, "findByPracticeAssignment_Student_Email", String.class);
        assertNotNull(lpByEmail);
        Query lpQuery = lpByEmail.getAnnotation(Query.class);
        assertNotNull(lpQuery, "LessonPlanRepository query must declare @Query with JOIN FETCH");
        String queryString = lpQuery.value();
        assertTrue(queryString.contains("JOIN FETCH lp.practiceAssignment"), "Query must JOIN FETCH practiceAssignment");
        assertTrue(queryString.contains("JOIN FETCH pa.student"), "Query must JOIN FETCH student");

        // PortfolioDocumentRepository findById
        Method docFindById = findMethod(PortfolioDocumentRepository.class, "findById", Long.class);
        assertNotNull(docFindById);
        EntityGraph docEg = docFindById.getAnnotation(EntityGraph.class);
        assertNotNull(docEg, "PortfolioDocumentRepository.findById must declare @EntityGraph");
        List<String> docPaths = Arrays.asList(docEg.attributePaths());
        assertTrue(docPaths.contains("practiceAssignment"));
        assertTrue(docPaths.contains("practiceAssignment.student"));
        assertTrue(docPaths.contains("practiceAssignment.subject.professor"));
    }

    private void assertAttributePathExists(Class<?> rootEntity, String attributePath, String context) {
        String[] parts = attributePath.split("\\.");
        Class<?> currentType = rootEntity;

        for (String segment : parts) {
            Field field = findFieldInHierarchy(currentType, segment);
            assertNotNull(field, String.format(
                    "Invalid attributePath '%s' in %s: property '%s' does NOT exist on %s",
                    attributePath, context, segment, currentType.getSimpleName()));

            currentType = field.getType();
            if (Collection.class.isAssignableFrom(currentType)) {
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType pt) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elemClass) {
                        currentType = elemClass;
                    }
                }
            }
        }
    }

    private Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Class<?> getDomainClassFromRepository(Class<?> repoClass) {
        for (Type genericInterface : repoClass.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt) {
                if (JpaRepository.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    Type domainType = pt.getActualTypeArguments()[0];
                    if (domainType instanceof Class<?> clazz) {
                        return clazz;
                    }
                }
            }
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                if (paramTypes.length == 0 || Arrays.equals(m.getParameterTypes(), paramTypes)) {
                    return m;
                }
            }
        }
        return null;
    }
}
