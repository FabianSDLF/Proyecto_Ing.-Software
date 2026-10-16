package com.ubb.practicas.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

/**
 * Abstracción de operaciones de almacenamiento de archivos (DIP - BK-34).
 * Permite desacoplar el almacenamiento físico de la lógica de negocio y controladores.
 */
public interface StorageService {

    /**
     * Inicializa el almacenamiento (crea directorios base necesarios).
     */
    void init();

    /**
     * Almacena un archivo multipart dentro de una subcarpeta específica.
     * Genera un nombre de archivo único sanitizado (UUID).
     *
     * @param file      Archivo multipart a almacenar.
     * @param subfolder Subcarpeta destino (e.g. "portafolio", "planificaciones") o null/vacío para raíz.
     * @return Nombre o ruta relativa del archivo almacenado.
     */
    String store(MultipartFile file, String subfolder);

    /**
     * Sobrecarga para almacenar un archivo en la raíz del directorio de almacenamiento.
     */
    default String store(MultipartFile file) {
        return store(file, null);
    }

    /**
     * Sobrecarga de compatibilidad con llamadas existentes a storeFile(file).
     */
    default String storeFile(MultipartFile file) {
        return store(file, null);
    }

    /**
     * Carga un archivo como Resource de Spring para lectura o streaming HTTP.
     *
     * @param filename Nombre o ruta relativa del archivo almacenado.
     * @return Resource accesible.
     */
    Resource loadAsResource(String filename);

    /**
     * Sobrecarga de compatibilidad con llamadas existentes a loadFileAsResource(filename).
     */
    default Resource loadFileAsResource(String filename) {
        return loadAsResource(filename);
    }

    /**
     * Elimina físicamente un archivo del almacenamiento.
     *
     * @param filename Nombre o ruta relativa del archivo a eliminar.
     */
    void deleteFile(String filename);

    /**
     * Resuelve la ruta absoluta en disco del archivo almacenado.
     */
    Path load(String filename);
}
