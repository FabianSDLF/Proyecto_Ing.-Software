package com.ubb.practicas.repository;

import com.ubb.practicas.model.PortfolioDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioDocumentRepository extends JpaRepository<PortfolioDocument, Long> {
    List<PortfolioDocument> findByPracticeAssignment_Student_Email(String email);
}
