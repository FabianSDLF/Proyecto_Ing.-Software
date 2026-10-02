package com.ubb.practicas.service.impl;

import com.ubb.practicas.exception.StorageException;
import com.ubb.practicas.exception.StorageFileNotFoundException;
import com.ubb.practicas.service.StorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Implementación de StorageService basada en el sistema de archivos local.
 * Incluye protección contra Path Traversal y validación de tipos de archivos permitidos (BK-34, BK-35).
 */
@Service("storageService")
public class LocalStorageServiceImpl implements StorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private Path rootLocation;

    private static final List<String> FORBIDDEN_EXTENSIONS = Arrays.asList(
            ".exe", ".bat", ".cmd", ".sh", ".jsp", ".jspx", ".php", ".phtml",
            ".asp", ".aspx", ".cgi", ".pl", ".py", ".jar", ".war", ".ear"
    );

    @Override
    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
        } catch (IOException ex) {
            throw new StorageException("No se pudo inicializar el directorio de almacenamiento en " + uploadDir, ex);
        }
    }

    @Override
    public String store(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("No se puede almacenar un archivo vacío.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo_sin_nombre"
        );

        if (originalFilename.contains("..")) {
            throw new StorageException("Secuencia de ruta relativa inválida en el nombre del archivo: " + originalFilename);
        }

        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            fileExtension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (FORBIDDEN_EXTENSIONS.contains(fileExtension)) {
            throw new StorageException("Tipo de archivo no permitido por políticas de seguridad: " + fileExtension);
        }

        String storedFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetDirectory = this.rootLocation;
        String relativeResultPath = storedFileName;

        if (subfolder != null && !subfolder.trim().isEmpty()) {
            String sanitizedSubfolder = subfolder.replace('\\', '/').trim();
            if (sanitizedSubfolder.startsWith("/")) {
                sanitizedSubfolder = sanitizedSubfolder.substring(1);
            }
            if (sanitizedSubfolder.contains("..")) {
                throw new StorageException("Secuencia de ruta inválida en subcarpeta: " + subfolder);
            }

            targetDirectory = this.rootLocation.resolve(sanitizedSubfolder).normalize();
            if (!targetDirectory.startsWith(this.rootLocation)) {
                throw new SecurityException("Intento de violación de directorio fuera de la raíz de almacenamiento.");
            }

            try {
                Files.createDirectories(targetDirectory);
            } catch (IOException e) {
                throw new StorageException("No se pudo crear la subcarpeta de almacenamiento: " + subfolder, e);
            }

            relativeResultPath = sanitizedSubfolder + "/" + storedFileName;
        }

        Path destinationFile = targetDirectory.resolve(storedFileName).normalize();
        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new SecurityException("Intento de almacenamiento fuera del directorio raíz.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return relativeResultPath;
        } catch (IOException ex) {
            throw new StorageException("Fallo al guardar el archivo: " + storedFileName, ex);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new StorageFileNotFoundException("El nombre del archivo no puede ser nulo ni vacío.");
        }

        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("No se pudo encontrar o leer el archivo: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new StorageFileNotFoundException("URL malformada para el archivo: " + filename, ex);
        }
    }

    @Override
    public void deleteFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return;
        }
        try {
            Path file = load(filename);
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new StorageException("No se pudo eliminar el archivo: " + filename, ex);
        }
    }

    @Override
    public Path load(String filename) {
        Path filePath = this.rootLocation.resolve(filename).normalize();
        if (!filePath.startsWith(this.rootLocation)) {
            throw new SecurityException("Intento de acceso a archivo fuera del directorio raíz: " + filename);
        }
        return filePath;
    }
}
