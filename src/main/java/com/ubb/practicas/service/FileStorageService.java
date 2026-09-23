package com.ubb.practicas.service;

import com.ubb.practicas.service.impl.LocalStorageServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @deprecated Refactorizado en Milestone 2 (BK-34). Utilizar {@link StorageService} inyectado por constructor.
 * Mantiene compatibilidad regresiva total con tests y componentes existentes.
 */
@Deprecated
@Service("fileStorageService")
@Primary
public class FileStorageService extends LocalStorageServiceImpl {
    // Hereda todas las operaciones e implementa StorageService de forma transparente.
}
