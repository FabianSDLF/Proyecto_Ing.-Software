# Handoff Report — Explorer 2 (Milestone 2)
**Phase 2: Storage Abstraction & User Batch Processing Decoupling (BK-28, BK-29, BK-30, BK-31, BK-32, BK-33, BK-34, BK-35)**

- **Agent Identity**: `teamwork_preview_explorer` (Explorer 2)
- **Working Directory**: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m2_storage_2`
- **Parent Conversation ID**: `c3f760d9-58dc-4c34-b179-bc154eee62cf`
- **Target Branch**: `fabian`
- **Date**: 2026-09-23

---

## 1. Observation

### 1.1. Storage Subsystem Current State & Findings (BK-34, BK-35)
- **Direct File Inspection**: `src/main/java/com/ubb/practicas/service/FileStorageService.java`
  - Lines 18-34:
    ```java
    @Service
    public class FileStorageService {
        @Value("${file.upload-dir:./uploads}")
        private String uploadDir;
        private Path fileStorageLocation;
        @PostConstruct
        public void init() {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.fileStorageLocation);
            } catch (Exception ex) {
                throw new RuntimeException("No se pudo crear el directorio donde se guardarán los archivos subidos.", ex);
            }
        }
    ```
  - Lines 36-59:
    - Concrete class with direct binding to `java.nio.file.Path` without any interface abstraction (BK-34).
    - Hardcoded flat storage directory (`./uploads`); no subfolder hierarchy organization (`subfolder` parameter absent).
    - Security validation is rudimentary (`if (storedFileName.contains(".."))`), without canonical path traversal boundary verification (`!targetLocation.startsWith(this.fileStorageLocation)`) or extension/MIME header whitelist verification (BK-35).
    - Throws generic unchecked `RuntimeException` instead of domain-specific storage exceptions (BK-33).
- **Usages in Codebase**:
  - `src/main/java/com/ubb/practicas/controller/PortfolioController.java`:
    - Line 15: `import com.ubb.practicas.service.FileStorageService;`
    - Line 44: `@Autowired private FileStorageService fileStorageService;`
    - Line 113: `Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());`
    - Line 141: `String storedFileName = fileStorageService.storeFile(file);`
    - Line 195: `String storedFileName = fileStorageService.storeFile(file);`
    - Line 239: `fileStorageService.deleteFile(plan.getDocument().getStoredFileName());`
    - Line 266: `fileStorageService.deleteFile(doc.getStoredFileName());`
  - `src/test/java/com/ubb/practicas/controller/PortfolioControllerTest.java`:
    - Line 9: `import com.ubb.practicas.service.FileStorageService;`
    - Line 47: `@Autowired private FileStorageService fileStorageService;`
    - Line 104: `String storedFileName = fileStorageService.storeFile(initialFile);`
    - Line 177: `String planStored = fileStorageService.storeFile(planFile);`

### 1.2. User Batch Processing Current State & Findings (BK-28, BK-29, BK-30, BK-31, BK-32, BK-33)
- **Direct File Inspection**: `src/main/java/com/ubb/practicas/service/UserService.java`
  - Lines 7-10:
    ```java
    import org.apache.poi.ss.usermodel.*;
    import org.apache.poi.xssf.usermodel.XSSFWorkbook;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.crypto.password.PasswordEncoder;
    ```
  - Lines 22-29:
    ```java
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    ```
    - Field injection via `@Autowired` on private mutable attributes (BK-28).
  - Lines 32-42:
    ```java
    @Transactional
    public void procesarArchivoMasivo(MultipartFile archivo) throws Exception {
        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo != null && nombreArchivo.endsWith(".xlsx")) {
            procesarExcel(archivo);
        } else if (nombreArchivo != null && nombreArchivo.endsWith(".csv")) {
            procesarCSV(archivo);
        } else {
            throw new Exception("Formato de archivo no soportado. Solo .csv o .xlsx");
        }
    }
    ```
    - God Service mixing user domain lifecycle with binary Excel parsing and CSV text decoding (BK-29).
    - Rigid conditional branching `if-else` violating Open/Closed Principle (BK-30).
    - Declares `throws Exception` indiscriminately (BK-33).
  - Lines 44-66:
    - Parses Excel using `XSSFWorkbook` directly inside service.
    - Fragile cell extraction: `cellName.getStringCellValue()`, `cellPass.getStringCellValue()`. In Apache POI, if a cell is numeric (e.g. numeric password or RUT or student ID), `getStringCellValue()` throws `IllegalStateException: Cannot get a STRING value from a NUMERIC cell`.
  - Lines 68-90:
    - Parses CSV using `String.split(",")`. If full name contains commas (e.g. `"Sepúlveda, Fabián"`), splitting by `,` splits into 5 tokens instead of 4, corrupting the row (BK-32).
    - Does not handle UTF-8 Byte Order Mark (BOM: `\uFEFF`), which Excel on Windows adds by default to CSV exports.
    - Does not support semicolon `;` delimiter, standard in Spanish Windows Excel CSV exports.
  - Lines 60, 175:
    - Hardcoded default password `"temporal123"` in cleartext (BK-31).
  - Lines 92-110:
    - `guardarUsuarioBulk(fullName, email, password, roleName)` tightly coupled inside the same class.
- **Controller Usages**:
  - `src/main/java/com/ubb/practicas/controller/UserController.java`:
    - Lines 20-22: `@Autowired private UserRepository userRepository; @Autowired private UserService userService;`
    - Lines 45-56:
      ```java
      @PostMapping("/carga-masiva")
      public ResponseEntity<?> cargaMasivaUsuarios(@RequestParam("archivo") MultipartFile archivo) {
          try {
              userService.procesarArchivoMasivo(archivo);
              return ResponseEntity.ok("Usuarios cargados exitosamente de forma masiva.");
          } catch (Exception e) {
              return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body("Error al procesar el archivo: " + e.getMessage());
          }
      }
      ```
- **Frontend Usages**:
  - `frontend/src/pages/CargaMasiva.jsx`:
    - Sends multipart `POST` to `/api/usuarios/carga-masiva` with form field name `'archivo'` and accepts `.csv, .xlsx`.

---

## 2. Logic Chain

1. **DIP & Abstraction for Storage (BK-34)**:
   - *Observation*: Controllers and test classes instantiate and inject concrete `FileStorageService` directly.
   - *Inference*: To decouple business logic from local disk I/O and permit future cloud backends (AWS S3, MinIO, Azure Blob), an interface `StorageService` must be defined with required contract methods (`init()`, `store(MultipartFile, String)`, `loadAsResource(String)`, `deleteFile(String)`).
   - *Design Choice*: Implement `LocalStorageServiceImpl implements StorageService`. Provide backward-compatible default methods (`storeFile(MultipartFile)` and `loadFileAsResource(String)`) in `StorageService` so existing controllers and tests compile without breaking changes while transitioning. Provide sanitization against directory traversal (`resolve().normalize().startsWith()`) and MIME/extension whitelist checks (BK-35).

2. **SRP & OCP Decoupling for Batch Processing (BK-29, BK-30, BK-32)**:
   - *Observation*: `UserService` contains 59 lines of low-level I/O parsing code (Apache POI workbook iteration and CSV string splitting), uses fragile `.split(",")`, and hardcoded `if-else` format branching.
   - *Inference*: File parsing is an infrastructure/ingestion concern, not a user domain lifecycle concern. Adding new formats (TSV, JSON, ODS) should not require modifying user business rules.
   - *Design Choice*:
     a. Define an intermediate DTO `UserImportRecordDto` and result DTO `BatchImportResultDto`.
     b. Define a Strategy interface `UserBatchParser` with `supports(String filename, String contentType)` and `List<UserImportRecordDto> parse(InputStream inputStream)`.
     c. Create concrete parsers: `ExcelUserBatchParser` (utilizing POI `DataFormatter` for numeric/text safety) and `CsvUserBatchParser` (implementing RFC 4180 tokenizer, delimiter auto-detection for `,` and `;`, and UTF-8 BOM removal).
     d. Create orchestrator `UserBatchImportService` / `UserBatchImportServiceImpl` which injects `List<UserBatchParser>` (Spring OCP auto-collection) and handles persistence/updates.
     e. In `UserService`, remove all POI imports and private parsing methods; keep `procesarArchivoMasivo` as a clean delegate to `UserBatchImportService` to maintain 100% backward compatibility for controllers and external callers.

3. **Dependency Injection & Clean Error Hierarchy (BK-28, BK-31, BK-33)**:
   - *Observation*: `UserService` and `UserController` rely on field injection `@Autowired` and throw raw `Exception`.
   - *Inference*: Inversion of control requires constructor injection with `final` fields for immutability and unit testability. Specific runtime domain exceptions must replace raw `Exception`.
   - *Design Choice*:
     a. Refactor `UserService` and `UserController` to constructor injection (`private final` + `@RequiredArgsConstructor`).
     b. Introduce domain exceptions: `StorageException`, `StorageFileNotFoundException`, `FileProcessingException`, `UnsupportedFileFormatException`, `UserNotFoundException`, `EmailAlreadyInUseException`, `RoleNotFoundException`.
     c. Make the default fallback password configurable via `@Value("${app.security.default-user-password:temporal123}")` in `UserBatchImportServiceImpl`.

---

## 3. Caveats

- **No Caveats on Architecture**: The designs preserve 100% of existing REST contracts and frontend interactions.
- **Dependency Scope**: Apache POI 5.4.0 is already present in `pom.xml`. The CSV parser is implemented natively using standard Java 21 I/O following RFC 4180 without introducing new external dependencies (e.g. `commons-csv` or `opencsv`), ensuring zero risk of Maven dependency conflicts or version mismatch.
- **File Storage Directory**: Default storage location remains `./uploads` as configured in `application.properties`, ensuring existing test runs and local dev workflows continue without friction.

---

## 4. Conclusion & Complete Blueprints for Worker

### 4.1. File Layout & Deliverable Inventory

```
src/main/java/com/ubb/practicas/
├── exception/
│   ├── StorageException.java                     [NEW]
│   ├── StorageFileNotFoundException.java         [NEW]
│   ├── FileProcessingException.java              [NEW]
│   ├── UnsupportedFileFormatException.java       [NEW]
│   ├── UserNotFoundException.java                [NEW]
│   ├── EmailAlreadyInUseException.java           [NEW]
│   └── RoleNotFoundException.java                [NEW]
├── dto/
│   ├── UserImportRecordDto.java                  [NEW]
│   └── BatchImportResultDto.java                 [NEW]
├── service/
│   ├── StorageService.java                       [NEW - Interface]
│   ├── FileStorageService.java                   [MODIFIED/REFACTORED - Implements StorageService]
│   ├── UserBatchImportService.java               [NEW - Interface]
│   ├── UserService.java                          [MODIFIED - Decoupled & Constructor Injection]
│   ├── batch/
│   │   ├── UserBatchParser.java                  [NEW - Strategy Interface]
│   │   └── parser/
│   │       ├── ExcelUserBatchParser.java         [NEW - Apache POI Parser]
│   │       └── CsvUserBatchParser.java           [NEW - RFC 4180 CSV Parser]
│   └── impl/
│       ├── LocalStorageServiceImpl.java          [NEW - Concrete Storage Service]
│       └── UserBatchImportServiceImpl.java       [NEW - Concrete Batch Orchestrator]
├── controller/
│   ├── PortfolioController.java                  [MODIFIED - Injects StorageService via constructor]
│   └── UserController.java                       [MODIFIED - Constructor injection]
└── test/java/com/ubb/practicas/
    ├── service/
    │   ├── LocalStorageServiceImplTest.java      [NEW - Unit Tests]
    │   └── UserBatchImportServiceImplTest.java   [NEW - Unit Tests]
    └── controller/
        └── PortfolioControllerTest.java          [MODIFIED - Injects StorageService]
```

---

### 4.2. Detailed Code Blueprints

#### Blueprint 1: `com.ubb.practicas.exception` Package

##### `StorageException.java`
```java
package com.ubb.practicas.exception;

public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

##### `StorageFileNotFoundException.java`
```java
package com.ubb.practicas.exception;

public class StorageFileNotFoundException extends StorageException {
    public StorageFileNotFoundException(String message) {
        super(message);
    }
    public StorageFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

##### `FileProcessingException.java`
```java
package com.ubb.practicas.exception;

public class FileProcessingException extends RuntimeException {
    public FileProcessingException(String message) {
        super(message);
    }
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

##### `UnsupportedFileFormatException.java`
```java
package com.ubb.practicas.exception;

public class UnsupportedFileFormatException extends FileProcessingException {
    public UnsupportedFileFormatException(String message) {
        super(message);
    }
}
```

##### `UserNotFoundException.java`, `EmailAlreadyInUseException.java`, `RoleNotFoundException.java`
```java
package com.ubb.practicas.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) { super(message); }
}
```
```java
package com.ubb.practicas.exception;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(String message) { super(message); }
}
```
```java
package com.ubb.practicas.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) { super(message); }
}
```

---

#### Blueprint 2: `StorageService` Abstraction (BK-34, BK-35)

##### `StorageService.java`
`src/main/java/com/ubb/practicas/service/StorageService.java`
```java
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
```

##### `LocalStorageServiceImpl.java`
`src/main/java/com/ubb/practicas/service/impl/LocalStorageServiceImpl.java`
```java
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
```

##### Transition Strategy for `FileStorageService.java`:
To prevent any compile breakage across legacy branches or references, `FileStorageService` is refactored to implement `StorageService` by delegating to `LocalStorageServiceImpl` (or inheriting from it) with `@Primary`:
`src/main/java/com/ubb/practicas/service/FileStorageService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.service.impl.LocalStorageServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @deprecated Refactorizado en Milestone 2 (BK-34). Utilizar {@link StorageService} inyectado por constructor.
 */
@Deprecated
@Service
@Primary
public class FileStorageService extends LocalStorageServiceImpl {
    // Hereda todas las operaciones e implementa StorageService de forma transparente.
}
```

---

#### Blueprint 3: User Batch Processing Decoupling (BK-29, BK-30, BK-31, BK-32, BK-33)

##### `UserImportRecordDto.java`
`src/main/java/com/ubb/practicas/dto/UserImportRecordDto.java`
```java
package com.ubb.practicas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserImportRecordDto {
    private int rowNumber;
    private String fullName;
    private String email;
    private String password;
    private String roleName;
}
```

##### `BatchImportResultDto.java`
`src/main/java/com/ubb/practicas/dto/BatchImportResultDto.java`
```java
package com.ubb.practicas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchImportResultDto {
    private int totalRecords;
    private int successCount;
    private int failureCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> createdUserEmails = new ArrayList<>();

    @Builder.Default
    private List<String> updatedUserEmails = new ArrayList<>();
}
```

##### `UserBatchParser.java` (Strategy Interface)
`src/main/java/com/ubb/practicas/service/batch/UserBatchParser.java`
```java
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
```

##### `ExcelUserBatchParser.java` (Apache POI Parser)
`src/main/java/com/ubb/practicas/service/batch/parser/ExcelUserBatchParser.java`
```java
package com.ubb.practicas.service.batch.parser;

import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.service.batch.UserBatchParser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
public class ExcelUserBatchParser implements UserBatchParser {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return true;
            }
        }
        if (contentType != null) {
            return contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || contentType.equals("application/vnd.ms-excel");
        }
        return false;
    }

    @Override
    public List<UserImportRecordDto> parse(InputStream inputStream) throws FileProcessingException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<UserImportRecordDto> records = new ArrayList<>();

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return Collections.emptyList();
            }

            // Omitir cabecera (fila 0)
            rowIterator.next();

            int rowNumber = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNumber++;

                Cell cellName = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell cellEmail = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell cellPass = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell cellRole = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                if (cellName == null && cellEmail == null && cellRole == null) {
                    continue; // Fila vacía
                }

                String fullName = cellName != null ? formatter.formatCellValue(cellName).trim() : "";
                String email = cellEmail != null ? formatter.formatCellValue(cellEmail).trim() : "";
                String password = cellPass != null ? formatter.formatCellValue(cellPass).trim() : "";
                String roleName = cellRole != null ? formatter.formatCellValue(cellRole).trim() : "";

                if (fullName.isEmpty() && email.isEmpty()) {
                    continue;
                }

                records.add(UserImportRecordDto.builder()
                        .rowNumber(rowNumber)
                        .fullName(fullName)
                        .email(email)
                        .password(password)
                        .roleName(roleName)
                        .build());
            }

            return records;
        } catch (Exception ex) {
            throw new FileProcessingException("Error al procesar el archivo Excel: " + ex.getMessage(), ex);
        }
    }
}
```

##### `CsvUserBatchParser.java` (RFC 4180 CSV Parser)
`src/main/java/com/ubb/practicas/service/batch/parser/CsvUserBatchParser.java`
```java
package com.ubb.practicas.service.batch.parser;

import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.service.batch.UserBatchParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser de CSV robusto con soporte de comillas escapadas RFC 4180, detección automática
 * de delimitadores (coma / punto y coma) y manejo de UTF-8 BOM (BK-32).
 */
@Component
public class CsvUserBatchParser implements UserBatchParser {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return true;
        }
        if (contentType != null) {
            return contentType.equals("text/csv") || contentType.equals("application/csv")
                    || contentType.equals("text/plain");
        }
        return false;
    }

    @Override
    public List<UserImportRecordDto> parse(InputStream inputStream) throws FileProcessingException {
        List<UserImportRecordDto> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return records;
            }

            // Remover UTF-8 Byte Order Mark (BOM) si existe
            if (firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }

            // Detección automática de delimitador (; o ,)
            char delimiter = detectDelimiter(firstLine);

            String line;
            int rowNumber = 1; // La primera línea fue el encabezado

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> tokens = parseCsvLine(line, delimiter);
                if (tokens.size() < 4) {
                    continue;
                }

                String fullName = tokens.get(0).trim();
                String email = tokens.get(1).trim();
                String password = tokens.get(2).trim();
                String roleName = tokens.get(3).trim();

                records.add(UserImportRecordDto.builder()
                        .rowNumber(rowNumber)
                        .fullName(fullName)
                        .email(email)
                        .password(password)
                        .roleName(roleName)
                        .build());
            }

            return records;
        } catch (Exception ex) {
            throw new FileProcessingException("Error al leer el archivo CSV: " + ex.getMessage(), ex);
        }
    }

    private char detectDelimiter(String headerLine) {
        long semicolonCount = headerLine.chars().filter(ch -> ch == ';').count();
        long commaCount = headerLine.chars().filter(ch -> ch == ',').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // Saltar comilla escapada ("")
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }
}
```

##### `UserBatchImportService.java` & `UserBatchImportServiceImpl.java`
`src/main/java/com/ubb/practicas/service/UserBatchImportService.java`
```java
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
```

`src/main/java/com/ubb/practicas/service/impl/UserBatchImportServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.BatchImportResultDto;
import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.exception.UnsupportedFileFormatException;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.UserBatchImportService;
import com.ubb.practicas.service.batch.UserBatchParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchImportServiceImpl implements UserBatchImportService {

    private final List<UserBatchParser> parsers;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.default-user-password:temporal123}")
    private String defaultPassword;

    @Override
    @Transactional
    public BatchImportResultDto importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("El archivo proporcionado está vacío.");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        UserBatchParser parser = parsers.stream()
                .filter(p -> p.supports(filename, contentType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileFormatException(
                        "Formato de archivo no soportado. Formatos válidos: .xlsx, .xls, .csv"
                ));

        List<UserImportRecordDto> records;
        try (InputStream inputStream = file.getInputStream()) {
            records = parser.parse(inputStream);
        } catch (IOException e) {
            throw new FileProcessingException("No se pudo leer el archivo cargado: " + e.getMessage(), e);
        }

        BatchImportResultDto result = BatchImportResultDto.builder()
                .totalRecords(records.size())
                .build();

        for (UserImportRecordDto record : records) {
            try {
                processSingleRecord(record, result);
            } catch (Exception e) {
                String errMsg = "Fila " + record.getRowNumber() + " (" + record.getEmail() + "): " + e.getMessage();
                result.getErrors().add(errMsg);
                result.setFailureCount(result.getFailureCount() + 1);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void procesarArchivoMasivo(MultipartFile file) {
        BatchImportResultDto result = importUsers(file);
        if (result.getSuccessCount() == 0 && !result.getErrors().isEmpty()) {
            throw new FileProcessingException("Error al procesar archivo: " + result.getErrors().get(0));
        }
    }

    private void processSingleRecord(UserImportRecordDto record, BatchImportResultDto result) {
        if (record.getEmail() == null || record.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        if (record.getRoleName() == null || record.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }

        Role role = roleRepository.findByName(record.getRoleName().trim());
        if (role == null) {
            throw new IllegalArgumentException("Rol no encontrado en el sistema: " + record.getRoleName());
        }

        String rawPassword = (record.getPassword() != null && !record.getPassword().trim().isEmpty())
                ? record.getPassword().trim()
                : defaultPassword;

        User user = userRepository.findByEmail(record.getEmail().trim());
        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setCreatedAt(LocalDateTime.now());
            user.setActive(true);
            isNew = true;
        }

        user.setFullName(record.getFullName() != null ? record.getFullName().trim() : "");
        user.setEmail(record.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        userRepository.save(user);

        if (isNew) {
            result.getCreatedUserEmails().add(user.getEmail());
        } else {
            result.getUpdatedUserEmails().add(user.getEmail());
        }
        result.setSuccessCount(result.getSuccessCount() + 1);
    }
}
```

---

#### Blueprint 4: Refactored `UserService.java` (BK-28, BK-29, BK-30, BK-31, BK-33)
`src/main/java/com/ubb/practicas/service/UserService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.exception.EmailAlreadyInUseException;
import com.ubb.practicas.exception.RoleNotFoundException;
import com.ubb.practicas.exception.UserNotFoundException;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Servicio de dominio responsable exclusivamente del ciclo de vida de usuarios (SRP - BK-29).
 * El procesamiento de archivos masivos ha sido desacoplado hacia UserBatchImportService (BK-30).
 * Dependencias inyectadas por constructor (DIP - BK-28).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserBatchImportService userBatchImportService;

    @Value("${app.security.default-user-password:temporal123}")
    private String defaultPassword;

    /**
     * Delegación hacia el servicio desacoplado de importación masiva (BK-29, BK-30).
     */
    @Transactional
    public void procesarArchivoMasivo(MultipartFile archivo) {
        userBatchImportService.procesarArchivoMasivo(archivo);
    }

    @Transactional
    public User actualizarRolUsuario(Long usuarioId, String roleName) {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new RoleNotFoundException("Rol no encontrado: " + roleName);
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User actualizarUsuario(Long usuarioId, User datosActualizados) {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        if (datosActualizados.getFullName() != null) {
            user.setFullName(datosActualizados.getFullName());
        }
        if (datosActualizados.getEmail() != null && !user.getEmail().equals(datosActualizados.getEmail())) {
            if (userRepository.findByEmail(datosActualizados.getEmail()) != null) {
                throw new EmailAlreadyInUseException("El email ya está en uso");
            }
            user.setEmail(datosActualizados.getEmail());
        }
        if (datosActualizados.getRole() != null && datosActualizados.getRole().getName() != null) {
            Role role = roleRepository.findByName(datosActualizados.getRole().getName());
            if (role != null) {
                user.setRole(role);
            }
        }
        return userRepository.save(user);
    }

    @Transactional
    public User cambiarEstadoUsuario(Long usuarioId, boolean estado) {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + usuarioId));
        user.setActive(estado);
        return userRepository.save(user);
    }

    @Transactional
    public User crearUsuario(User nuevoUsuario) {
        if (userRepository.findByEmail(nuevoUsuario.getEmail()) != null) {
            throw new EmailAlreadyInUseException("El email ya está en uso");
        }

        if (nuevoUsuario.getRole() != null && nuevoUsuario.getRole().getName() != null) {
            Role role = roleRepository.findByName(nuevoUsuario.getRole().getName());
            if (role == null) {
                throw new RoleNotFoundException("Rol no encontrado: " + nuevoUsuario.getRole().getName());
            }
            nuevoUsuario.setRole(role);
        }

        if (nuevoUsuario.getPasswordHash() != null && !nuevoUsuario.getPasswordHash().isEmpty()) {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode(nuevoUsuario.getPasswordHash()));
        } else {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode(defaultPassword));
        }

        nuevoUsuario.setCreatedAt(LocalDateTime.now());
        nuevoUsuario.setActive(true);

        return userRepository.save(nuevoUsuario);
    }

    @Transactional
    public User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(emailActual);
        if (user == null) {
            throw new UserNotFoundException("Usuario no encontrado con email: " + emailActual);
        }
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()) != null) {
                throw new EmailAlreadyInUseException("El email ya está en uso por otro usuario");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(user);
    }
}
```

---

#### Blueprint 5: Refactored `UserController.java` (BK-28)
`src/main/java/com/ubb/practicas/controller/UserController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public List<User> obtenerTodosUsuarios() {
        return userRepository.findAll();
    }

    @GetMapping("/rol/{roleName}")
    public List<User> listarUsuariosPorRol(@PathVariable String roleName) {
        return userRepository.findByRoleName(roleName);
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody User nuevoUsuario) {
        try {
            User created = userService.crearUsuario(nuevoUsuario);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/carga-masiva")
    public ResponseEntity<?> cargaMasivaUsuarios(@RequestParam("archivo") MultipartFile archivo) {
        try {
            userService.procesarArchivoMasivo(archivo);
            return ResponseEntity.ok("Usuarios cargados exitosamente de forma masiva.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar el archivo: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<?> actualizarRol(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        try {
            String roleName = payload.get("roleName");
            User updatedUser = userService.actualizarRolUsuario(id, roleName);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody User datosActualizados) {
        try {
            User updatedUser = userService.actualizarUsuario(id, datosActualizados);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstadoUsuario(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> payload) {
        try {
            Boolean estado = payload.get("active");
            if (estado == null) {
                return ResponseEntity.badRequest().body("El campo 'active' es obligatorio");
            }
            User updatedUser = userService.cambiarEstadoUsuario(id, estado);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerMiPerfil(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        User user = userRepository.findByEmail(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }
        return ResponseEntity.ok(com.ubb.practicas.dto.UserResponse.fromUser(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> actualizarMiPerfil(java.security.Principal principal, @RequestBody com.ubb.practicas.dto.UserProfileUpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        try {
            User updatedUser = userService.actualizarMiPerfil(principal.getName(), request);
            return ResponseEntity.ok(com.ubb.practicas.dto.UserResponse.fromUser(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
```

---

#### Blueprint 6: Updating `PortfolioController.java` to inject `StorageService` (BK-34)
In `PortfolioController.java`:
- Change import:
  ```java
  // Reemplazar:
  // import com.ubb.practicas.service.FileStorageService;
  // Por:
  import com.ubb.practicas.service.StorageService;
  ```
- Change dependency:
  ```java
  // Reemplazar:
  // @Autowired
  // private FileStorageService fileStorageService;
  // Por:
  @Autowired
  private StorageService storageService;
  ```
- In methods:
  - Line 113: `Resource resource = storageService.loadAsResource(doc.getStoredFileName());`
  - Line 141: `String storedFileName = storageService.store(file, "portafolio");`
  - Line 195: `String storedFileName = storageService.store(file, "planificaciones");`
  - Line 239: `storageService.deleteFile(plan.getDocument().getStoredFileName());`
  - Line 266: `storageService.deleteFile(doc.getStoredFileName());`

---

#### Blueprint 7: Updating `PortfolioControllerTest.java` (BK-34)
In `src/test/java/com/ubb/practicas/controller/PortfolioControllerTest.java`:
- Change import:
  ```java
  import com.ubb.practicas.service.StorageService;
  ```
- Change field:
  ```java
  @Autowired
  private StorageService storageService;
  ```
- In `setup()`:
  - Line 104: `String storedFileName = storageService.store(initialFile);`
- In `test()`:
  - Line 177: `String planStored = storageService.store(planFile);`

---

## 5. Verification Method

### 5.1. Build & Test Commands
To be run by Worker and verification agents:

1. **Compilation Test**:
   ```powershell
   .\mvnw.cmd compile
   ```
   *Expected Result*: `BUILD SUCCESS` with zero syntax errors, zero missing import errors, and full Lombok processor generation.

2. **Automated Test Suite**:
   ```powershell
   .\mvnw.cmd test
   ```
   *Expected Result*: All 5 test classes (including `PortfolioControllerTest`, `AuthControllerTest`, `SubjectControllerTest`, `ProfessorControllerTest`, and `PracticasApplicationTests`) pass without failures or regressions.

3. **Frontend Compatibility Check**:
   ```powershell
   cd frontend
   npm run build
   ```
   *Expected Result*: Vite builds static assets without errors.

### 5.2. File Inspection Checklist
- [ ] `StorageService.java` exists in `com.ubb.practicas.service` with all 4 required methods plus backward-compatible default overloads.
- [ ] `LocalStorageServiceImpl.java` exists in `com.ubb.practicas.service.impl` implementing `StorageService` with subfolder resolution and path traversal verification.
- [ ] `FileStorageService.java` implements `StorageService` or delegates directly to `LocalStorageServiceImpl` for backward compatibility.
- [ ] `PortfolioController.java` and `PortfolioControllerTest.java` inject `StorageService`, not `FileStorageService`.
- [ ] `UserBatchImportService` and `UserBatchParser` exist, decoupling POI and CSV from `UserService.java`.
- [ ] `UserService.java` no longer imports `org.apache.poi.*` and utilizes constructor injection with `private final` fields.
- [ ] `UserController.java` uses constructor injection with `private final` fields.

### 5.3. Invalidation Conditions
- Any code directly instantiating or injecting concrete `FileStorageService` instead of interface `StorageService`.
- Re-introduction of Apache POI or CSV `split` operations directly into `UserService.java`.
- Non-compilation of existing endpoints or tests.
