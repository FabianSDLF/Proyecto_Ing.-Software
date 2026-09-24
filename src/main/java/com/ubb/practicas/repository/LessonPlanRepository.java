package com.ubb.practicas.repository;

import com.ubb.practicas.model.LessonPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {

    @Query("SELECT lp FROM LessonPlan lp " +
           "LEFT JOIN FETCH lp.document " +
           "JOIN FETCH lp.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email")
    List<LessonPlan> findByPracticeAssignment_Student_Email(@Param("email") String email);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"practiceAssignment", "practiceAssignment.student", "document"})
    Optional<LessonPlan> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"practiceAssignment", "document"})
    Optional<LessonPlan> findByDocumentId(Long documentId);
}
