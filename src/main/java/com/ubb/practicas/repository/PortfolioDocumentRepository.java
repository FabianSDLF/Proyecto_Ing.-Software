package com.ubb.practicas.repository;

import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.enums.DocumentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioDocumentRepository extends JpaRepository<PortfolioDocument, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {
        "practiceAssignment",
        "practiceAssignment.student",
        "practiceAssignment.collaborator",
        "practiceAssignment.tutor",
        "practiceAssignment.subject",
        "practiceAssignment.subject.professor"
    })
    Optional<PortfolioDocument> findById(@NonNull Long id);

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email")
    List<PortfolioDocument> findByPracticeAssignment_Student_Email(@Param("email") String email);

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email AND d.type = :type")
    List<PortfolioDocument> findByPracticeAssignment_Student_EmailAndType(
            @Param("email") String email,
            @Param("type") DocumentType type
    );

    // ==========================================
    // HU19: Métodos de Documentos Finales
    // ==========================================

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "WHERE pa.id = :assignmentId AND d.type IN :types")
    List<PortfolioDocument> findByPracticeAssignment_IdAndTypeIn(
            @Param("assignmentId") Long assignmentId,
            @Param("types") Collection<DocumentType> types
    );

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email AND d.type IN :types")
    List<PortfolioDocument> findByPracticeAssignment_Student_EmailAndTypeIn(
            @Param("email") String email,
            @Param("types") Collection<DocumentType> types
    );

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "WHERE pa.id = :assignmentId")
    List<PortfolioDocument> findByPracticeAssignment_Id(@Param("assignmentId") Long assignmentId);
}
