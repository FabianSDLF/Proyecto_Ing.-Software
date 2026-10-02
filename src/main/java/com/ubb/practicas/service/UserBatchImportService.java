package com.ubb.practicas.service;

import com.ubb.practicas.dto.BatchImportResultDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserBatchImportService {

    /**
     * Importa usuarios masivamente desde un archivo (.xlsx, .csv).
     */
    BatchImportResultDto importUsers(MultipartFile file);

    /**
     * Método de compatibilidad para el controlador /api/usuarios/carga-masiva.
     */
    void procesarArchivoMasivo(MultipartFile file);
}
