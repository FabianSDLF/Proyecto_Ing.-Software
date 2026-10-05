package com.ubb.practicas.repository;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.StudentPair;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentPairRepository extends JpaRepository<StudentPair, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {
            "assignment1", "assignment1.student", "assignment1.subject", "assignment1.center",
            "assignment2", "assignment2.student", "assignment2.subject", "assignment2.center",
            "period"
    })
    List<StudentPair> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {
            "assignment1", "assignment1.student", "assignment1.subject", "assignment1.center",
            "assignment2", "assignment2.student", "assignment2.subject", "assignment2.center",
            "period"
    })
    Optional<StudentPair> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {
            "assignment1", "assignment1.student", "assignment1.subject", "assignment1.center",
            "assignment2", "assignment2.student", "assignment2.subject", "assignment2.center",
            "period"
    })
    List<StudentPair> findByPeriodId(Long periodId);

    @Query("SELECT COUNT(sp) > 0 FROM StudentPair sp " +
           "WHERE sp.period.id = :periodId " +
           "AND (sp.assignment1.id = :assignmentId OR sp.assignment2.id = :assignmentId)")
    boolean existsByAssignmentIdAndPeriodId(@Param("assignmentId") Long assignmentId, @Param("periodId") Long periodId);

    @Query("SELECT COUNT(sp) > 0 FROM StudentPair sp " +
           "WHERE sp.period.id = :periodId " +
           "AND (sp.assignment1.student.id = :studentId OR sp.assignment2.student.id = :studentId)")
    boolean existsByStudentIdAndPeriodId(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    @Query("SELECT sp FROM StudentPair sp " +
           "WHERE sp.period.id = :periodId " +
           "AND (sp.assignment1.id = :assignmentId OR sp.assignment2.id = :assignmentId)")
    Optional<StudentPair> findByAssignmentIdAndPeriodId(@Param("assignmentId") Long assignmentId, @Param("periodId") Long periodId);

    @Query("SELECT pa FROM PracticeAssignment pa " +
           "WHERE pa.period.id = :periodId " +
           "AND pa.status = com.ubb.practicas.model.enums.AssignmentStatus.ACTIVA " +
           "AND pa.subject.cycle = com.ubb.practicas.model.enums.PracticeCycle.INTERMEDIO " +
           "AND pa.student.id NOT IN (" +
           "    SELECT sp.assignment1.student.id FROM StudentPair sp " +
           "    WHERE sp.period.id = :periodId AND sp.assignment1.student.id IS NOT NULL" +
           ") " +
           "AND pa.student.id NOT IN (" +
           "    SELECT sp.assignment2.student.id FROM StudentPair sp " +
           "    WHERE sp.period.id = :periodId AND sp.assignment2.student.id IS NOT NULL" +
           ")")
    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "center", "period"})
    List<PracticeAssignment> findAvailableUnpairedIntermediateAssignments(@Param("periodId") Long periodId);
}
