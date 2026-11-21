package com.ubb.practicas.service;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PortfolioService {
    List<PortfolioDocument> obtenerMisEvidencias(String studentEmail);
    DownloadedDocument descargarDocumento(Long id, String userEmail);
    PortfolioDocument subirEvidencia(String studentEmail, Long assignmentId, MultipartFile file);
    void eliminarDocumento(Long id, String userEmail);

    // ==========================================
    // HU19: Métodos para Documentos de Término
    // ==========================================
    PortfolioDocument subirDocumentoFinal(MultipartFile file, DocumentType type, Boolean replaceConfirmed, User student);
    PortfolioDocument subirDocumentoFinal(String studentEmail, Long assignmentId, MultipartFile file, DocumentType type, Boolean replaceConfirmed);
    List<PortfolioDocument> obtenerMisDocumentosFinales(User student);
    List<PortfolioDocument> obtenerMisDocumentosFinales(String studentEmail);
    List<PortfolioDocument> obtenerDocumentosFinalesPorAsignacion(Long assignmentId);
    List<PortfolioDocument> obtenerEvidenciasPorAsignacion(Long assignmentId, String userEmail);
    void eliminarDocumentoFinal(Long id, String userEmail);
    PortfolioDocument agregarRetroalimentacion(Long id, String feedback, String userEmail);

}
