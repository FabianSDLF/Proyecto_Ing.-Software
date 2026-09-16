package com.ubb.practicas.repository;

import com.ubb.practicas.model.PortfolioDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioDocumentRepository extends JpaRepository<PortfolioDocument, Long> {
}
