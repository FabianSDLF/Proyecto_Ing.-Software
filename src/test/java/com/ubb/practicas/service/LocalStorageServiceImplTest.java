package com.ubb.practicas.service;

import com.ubb.practicas.exception.StorageException;
import com.ubb.practicas.exception.StorageFileNotFoundException;
import com.ubb.practicas.service.impl.LocalStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LocalStorageServiceImplTest {

    private LocalStorageServiceImpl storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
        storageService.init();
    }

    @Test
    void testStoreAndLoadFileSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "Contenido de prueba para documento".getBytes()
        );

        String storedName = storageService.store(file);
        assertNotNull(storedName);
        assertTrue(storedName.endsWith(".pdf"));

        Resource resource = storageService.loadAsResource(storedName);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void testStoreFileInSubfolderSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Contenido evidencia".getBytes()
        );

        String storedPath = storageService.store(file, "portafolio/estudiante_1");
        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("portafolio/estudiante_1/"));
        assertTrue(storedPath.endsWith(".docx"));

        Resource resource = storageService.loadAsResource(storedPath);
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void testStoreEmptyFileThrowsStorageException() {
        MockMultipartFile file = new MockMultipartFile("file", "vacio.pdf", "application/pdf", new byte[0]);
        assertThrows(StorageException.class, () -> storageService.store(file));
    }

    @Test
    void testStoreForbiddenExtensionThrowsStorageException() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "bin".getBytes());
        assertThrows(StorageException.class, () -> storageService.store(exeFile));

        MockMultipartFile shFile = new MockMultipartFile("file", "script.sh", "text/plain", "echo hi".getBytes());
        assertThrows(StorageException.class, () -> storageService.store(shFile));
    }

    @Test
    void testStorePathTraversalThrowsException() {
        MockMultipartFile traversalFile = new MockMultipartFile("file", "../../../etc/passwd", "text/plain", "data".getBytes());
        assertThrows(StorageException.class, () -> storageService.store(traversalFile));

        MockMultipartFile file = new MockMultipartFile("file", "safe.txt", "text/plain", "data".getBytes());
        assertThrows(StorageException.class, () -> storageService.store(file, "../outside"));
    }

    @Test
    void testDeleteFileSuccess() {
        MockMultipartFile file = new MockMultipartFile("file", "eliminar.pdf", "application/pdf", "temp".getBytes());
        String storedName = storageService.store(file);

        Resource resourceBefore = storageService.loadAsResource(storedName);
        assertTrue(resourceBefore.exists());

        storageService.deleteFile(storedName);

        assertThrows(StorageFileNotFoundException.class, () -> storageService.loadAsResource(storedName));
    }
}
