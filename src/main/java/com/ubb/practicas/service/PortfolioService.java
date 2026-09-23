package com.ubb.practicas.service;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.model.PortfolioDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PortfolioService {
    List<PortfolioDocument> obtenerMisEvidencias(String studentEmail);
    DownloadedDocument descargarDocumento(Long id, String userEmail);
    PortfolioDocument subirEvidencia(String studentEmail, Long assignmentId, MultipartFile file);
    void eliminarDocumento(Long id, String userEmail);
}
