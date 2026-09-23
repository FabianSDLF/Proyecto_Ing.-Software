package com.ubb.practicas.service.batch;

import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;

import java.io.InputStream;
import java.util.List;

/**
 * Estrategia de parseo modular para archivos de importación masiva (OCP - BK-30).
 */
public interface UserBatchParser {

    /**
     * Determina si el parser es capaz de procesar el archivo dado su nombre y tipo MIME.
     */
    boolean supports(String filename, String contentType);

    /**
     * Lee y transforma el flujo de entrada en una lista estructurada de UserImportRecordDto.
     *
     * @param inputStream Flujo binario o de texto del archivo.
     * @return Lista de registros normalizados.
     * @throws FileProcessingException si ocurre un error de lectura o formato.
     */
    List<UserImportRecordDto> parse(InputStream inputStream) throws FileProcessingException;
}
