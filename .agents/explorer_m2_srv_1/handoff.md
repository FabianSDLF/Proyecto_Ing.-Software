# Handoff Report — Milestone 2 (Phase 2: Layered Architecture, SOLID Services & DIP)

**Author:** Explorer 1 (`teamwork_preview_explorer`)  
**Date:** 2026-09-23  
**Working Directory:** `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m2_srv_1`  
**Parent Task:** Milestone 2 Dispatch (Conversation ID: `c3f760d9-58dc-4c34-b179-bc154eee62cf`)  
**Scope:** Backend Service Layer Extraction & Dependency Inversion Principle (DIP)

---

## 1. Observation

### 1.1. Catalog of Direct `@Autowired` Field Injections
A full scan across `src/main/java/com/ubb/practicas/` identified exactly **22 occurrences of `@Autowired` on private fields across 8 classes**:
1. `PortfolioController.java`:
   - Line 34: `@Autowired private PortfolioDocumentRepository portfolioDocumentRepository;`
   - Line 37: `@Autowired private PracticeAssignmentRepository practiceAssignmentRepository;`
   - Line 40: `@Autowired private LessonPlanRepository lessonPlanRepository;`
   - Line 43: `@Autowired private FileStorageService fileStorageService;`
   - Line 46: `@Autowired private UserRepository userRepository;`
2. `AuthController.java`:
   - Line 32: `@Autowired private PasswordResetTokenRepository tokenRepository;` (mixed with constructor injection on lines 35-39: BK-10)
3. `SubjectController.java`:
   - Line 21: `@Autowired private SubjectRepository subjectRepository;`
   - Line 24: `@Autowired private PracticeAssignmentRepository practiceAssignmentRepository;`
   - Line 29: `@Autowired private com.ubb.practicas.repository.UserRepository userRepository;` (FQCN smell: BK-15)
4. `PracticeAssignmentController.java`:
   - Line 17: `@Autowired private PracticeAssignmentRepository assignmentRepository;`
   - Line 35: `@Autowired private com.ubb.practicas.repository.UserRepository userRepository;` (interleaved in methods: BK-17)
   - Line 38: `@Autowired private com.ubb.practicas.repository.SubjectRepository subjectRepository;`
   - Line 41: `@Autowired private com.ubb.practicas.repository.PracticeCenterRepository centerRepository;`
5. `PracticeCenterController.java`:
   - Line 15: `@Autowired private PracticeCenterRepository centerRepository;`
6. `RoleController.java`:
   - Line 18: `@Autowired private RoleRepository roleRepository;`
7. `UserController.java`:
   - Line 19: `@Autowired private UserRepository userRepository;`
   - Line 21: `@Autowired private UserService userService;`
8. `ProfessorController.java`:
   - Line 21: `@Autowired private PracticeAssignmentRepository practiceAssignmentRepository;`
   - Line 24: `@Autowired private UserRepository userRepository;`
9. `UserService.java`:
   - Line 22: `@Autowired private UserRepository userRepository;`
   - Line 25: `@Autowired private RoleRepository roleRepository;`
   - Line 28: `@Autowired private PasswordEncoder passwordEncoder;`

### 1.2. Direct Business and Persistence Logic in Controllers
- **`PortfolioController.java` (Lines 51-278):**
  - Contains assignment resolution `resolveAssignment` (lines 51-70) and admin check `isUserAdmin` (lines 72-75).
  - Contains file upload handling, entity construction, double-save for `fileUrl`, and storage coordination for evidences (`subirEvidencia`, lines 128-161).
  - Contains lesson plan construction, parsing of `MathAxis`, attachment handling, and persistence (`crearPlanificacion`, lines 163-218).
  - Contains file streaming, HTTP header assembly, and BOLA/IDOR authorization check `isAccessibleBy` (`descargarDocumento`, lines 95-126).
  - Contains cascading deletion of physical files, linked lesson plans, and documents with `isOwnedBy` ownership checks (`eliminarPlanificacion`, lines 220-246; `eliminarDocumento`, lines 248-278).
- **`AuthController.java` (Lines 41-112):**
  - Direct credentials verification with `passwordEncoder.matches`, active check, role extraction, JWT token issuance (`login`, lines 41-62).
  - UUID token generation, expiry date calculation (`plusMinutes(30)`), repository persistence, and server logging (`forgotPassword`, lines 64-87).
  - Token lookup, expiry validation, user password re-encoding, and token cleanup (`resetPassword`, lines 89-112).
- **`SubjectController.java` (Lines 37-92):**
  - Name whitespace trimming, length validations (255 chars), cycle presence, code trimming and validation (50 chars), teacher existence verification via `userRepository.existsById` (`crearAsignatura`, lines 37-74).
  - Integrity violation checks against practice assignments (`practiceAssignmentRepository.existsBySubjectId`) before deletion (`eliminarAsignatura`, lines 76-92).
  - Redundant `@CrossOrigin(origins = "http://localhost:5173")` (line 17) and unused imports (`EntityManager`, `Transactional`, lines 6-7).
- **`PracticeAssignmentController.java` (Lines 20-78):**
  - Validations checking existence of Student, Subject, and PracticeCenter in repositories before assignment (`crearAsignacion`, lines 44-69).
  - Default status assignment (`AssignmentStatus.ACTIVA`) and timestamp creation.
  - Redundant `@CrossOrigin(origins = "http://localhost:5173")` (line 13).
- **`PracticeCenterController.java` (Lines 18-22):**
  - Direct `centerRepository.findAll()` without service layer.
  - Redundant `@CrossOrigin(origins = "http://localhost:5173")` (line 11).
- **`RoleController.java` (Lines 21-25):**
  - Direct `roleRepository.findAll()` without service layer.
  - Redundant `@CrossOrigin(origins = "http://localhost:5173")` (line 14).
- **`UserController.java` (Lines 24-102):**
  - Direct invocation of `userRepository.findAll()` in `obtenerTodosUsuarios()` (line 26).
  - Direct invocation of `userRepository.findByRoleName(roleName)` in `listarUsuariosPorRol()` (line 31).
  - Direct invocation of `userRepository.findByEmail(principal.getName())` in `obtenerMiPerfil()` (line 100).
  - Colloquial comments: `// Le pasamos la pelota al cerebro (el Service)` (line 47).
- **`ProfessorController.java` (Lines 27-60):**
  - Directly queries `userRepository` and `practiceAssignmentRepository`.

### 1.3. Test Suite Baseline Requirements
Inspected all tests in `src/test/java/com/ubb/practicas/controller/`:
- `AuthControllerTest.java`: Tests `POST /api/auth/forgot-password` with non-existing and existing emails, requiring uniform response `{"message": "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."}` and 0 exposed tokens/links.
- `PortfolioControllerTest.java`: Tests `GET /api/portafolio/documentos/{id}/descargar` (200 for owner & admin, 403 for other students), `DELETE /api/portafolio/documentos/{id}` (403 for other students, 200 for owner, safe deletion when plans have null documents), and active assignment resolution.
- `SubjectControllerTest.java`: Tests `POST /api/asignaturas` (200 on valid inputs with uppercase/capitalized/whitespace cycle strings, 400 on missing or invalid fields), and `DELETE /api/asignaturas/{id}` (409 CONFLICT on subjects associated with assignments).
- `ProfessorControllerTest.java`: Tests `GET /api/profesor/{id}/estudiantes` (200 for self professor & admin, 403 for other users, 404 for missing professor).

---

## 2. Logic Chain

1. **DIP & Construction Inversion:** The 22 field-injected `@Autowired` occurrences violate DIP (BK-28, BK-10, BK-17) and make classes fragile and difficult to unit test. By declaring all collaborating components as `private final` and annotating classes with Lombok's `@RequiredArgsConstructor` (or explicit constructors), Spring 3+ will automatically inject dependencies via constructor.
2. **Layered Architecture & SRP:** The controllers are currently functioning as "God Controllers" (BK-01, BK-09, BK-14, BK-18, BK-22, BK-23, BK-24), violating SRP. Business logic, validations, database queries, and storage operations must reside strictly within domain service interfaces (`com.ubb.practicas.service.*`) and their concrete implementations (`com.ubb.practicas.service.impl.*`).
3. **Preservation of Phase 1 Security Controls:** Milestone 1 established critical security invariants:
   - `PortfolioDocument.isAccessibleBy(email)` and `PortfolioDocument.isOwnedBy(email)`
   - `LessonPlan.isOwnedBy(email)`
   - `PracticeAssignment.isStudent(email)` and `AssignmentStatus.ACTIVA` check in `resolveAssignment`
   - Admin authorization bypass (`"Administrador".equalsIgnoreCase(role.getName())`)
   - Uniform response in `forgotPassword` without account enumeration
   - Deletion safety when `LessonPlan.document == null`
   These checks must be cleanly retained inside the service layer (`PortfolioServiceImpl`, `LessonPlanServiceImpl`, `AuthServiceImpl`) and controllers, ensuring 100% compatibility with the existing test suite.
4. **Storage Service Abstraction:** In coordination with Explorer 2 (`StorageService` interface), `PortfolioServiceImpl` and `LessonPlanServiceImpl` must depend on `StorageService` (interface) rather than the concrete `FileStorageService`, eliminating local filesystem tight coupling (BK-34).
5. **Decoupling `UserController` from `UserRepository`:** By moving `findAll()`, `findByRoleName(...)`, and `findByEmail(...)` into `UserService` (interface) and `UserServiceImpl`, `UserController` has 0 repository dependencies and only interacts with `UserService`.

---

## 3. Caveats

1. **Storage Service Interface:** `PortfolioServiceImpl` and `LessonPlanServiceImpl` are designed to inject `StorageService`. If Worker implements `StorageService` interface in Phase 2, `FileStorageService` should implement `StorageService`. The method names (`storeFile`, `loadFileAsResource`, `deleteFile`) are identical to existing `FileStorageService`, ensuring zero breaking changes.
2. **Batch Import Processing:** In `UserService`, `procesarArchivoMasivo` remains in the interface for `UserController` compatibility, but can delegate internally to Explorer 2's `UserBatchImportService` / parsers.
3. **No Code Written to src/:** This report is strictly read-only. Detailed designs and complete replacement code snippets are provided below for Worker to implement.

---

## 4. Conclusion & Complete Technical Specification

We have designed:
1. **7 Service Interfaces** + **7 Service Implementations** (plus DTO `DownloadedDocument`):
   - `PortfolioService` & `PortfolioServiceImpl`
   - `LessonPlanService` & `LessonPlanServiceImpl`
   - `AuthService` & `AuthServiceImpl`
   - `SubjectService` & `SubjectServiceImpl`
   - `PracticeAssignmentService` & `PracticeAssignmentServiceImpl`
   - `PracticeCenterService` & `PracticeCenterServiceImpl`
   - `RoleService` & `RoleServiceImpl`
   - `UserService` (Interface) & `UserServiceImpl`
2. **Elimination of all 22 `@Autowired` field injections** in favor of `private final` constructor injection (`@RequiredArgsConstructor`).
3. **Refactoring of 8 Controllers** into thin HTTP endpoints.
4. **Preservation of all Phase 1 security invariants**.

---

### Detailed Implementation Blueprints for Worker

#### A. New DTO: `src/main/java/com/ubb/practicas/dto/DownloadedDocument.java`
```java
package com.ubb.practicas.dto;

import org.springframework.core.io.Resource;

public record DownloadedDocument(
        Resource resource,
        String fileName,
        String contentType
) {}
```

---

#### B. Service 1: `PortfolioService` & `PortfolioServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/PortfolioService.java`
```java
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
```

**File:** `src/main/java/com/ubb/practicas/service/impl/PortfolioServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.FileStorageService;
import com.ubb.practicas.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioDocumentRepository portfolioDocumentRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final LessonPlanRepository lessonPlanRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService; // Or StorageService once abstracted

    private PracticeAssignment resolveAssignment(String email, Long explicitAssignmentId) {
        if (explicitAssignmentId != null) {
            Optional<PracticeAssignment> opt = practiceAssignmentRepository.findById(explicitAssignmentId);
            if (opt.isPresent()) {
                PracticeAssignment a = opt.get();
                if (a.isStudent(email) && a.getStatus() == AssignmentStatus.ACTIVA) {
                    return a;
                }
            }
            return null;
        }

        List<PracticeAssignment> activeAssignments = practiceAssignmentRepository.findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA);
        if (activeAssignments.isEmpty()) {
            return null;
        }
        return activeAssignments.stream()
                .max((a1, a2) -> Long.compare(a1.getId() != null ? a1.getId() : 0L, a2.getId() != null ? a2.getId() : 0L))
                .orElse(activeAssignments.get(0));
    }

    private boolean isUserAdmin(String email) {
        User user = userRepository.findByEmail(email);
        return user != null && user.getRole() != null && "Administrador".equalsIgnoreCase(user.getRole().getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDocument> obtenerMisEvidencias(String studentEmail) {
        return portfolioDocumentRepository.findByPracticeAssignment_Student_EmailAndType(
                studentEmail, DocumentType.EVIDENCIA_APRENDIZAJE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedDocument descargarDocumento(Long id, String userEmail) {
        PortfolioDocument doc = portfolioDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !doc.isAccessibleBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para acceder a este documento");
        }

        Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());
        String contentType = doc.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new DownloadedDocument(resource, doc.getFileName(), contentType);
    }

    @Override
    @Transactional
    public PortfolioDocument subirEvidencia(String studentEmail, Long assignmentId, MultipartFile file) {
        PracticeAssignment assignment = resolveAssignment(studentEmail, assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("No tienes una asignación de práctica activa.");
        }

        String storedFileName = fileStorageService.storeFile(file);

        PortfolioDocument doc = new PortfolioDocument();
        doc.setPracticeAssignment(assignment);
        doc.setType(DocumentType.EVIDENCIA_APRENDIZAJE);
        doc.setFileName(file.getOriginalFilename());
        doc.setStoredFileName(storedFileName);
        doc.setContentType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setUploadDate(LocalDateTime.now());

        portfolioDocumentRepository.save(doc);
        doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
        return portfolioDocumentRepository.save(doc);
    }

    @Override
    @Transactional
    public void eliminarDocumento(Long id, String userEmail) {
        PortfolioDocument doc = portfolioDocumentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !doc.isOwnedBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para eliminar este documento");
        }

        fileStorageService.deleteFile(doc.getStoredFileName());

        if (doc.getType() == DocumentType.PLANIFICACION) {
            lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete);
        }

        portfolioDocumentRepository.delete(doc);
    }
}
```

---

#### C. Service 2: `LessonPlanService` & `LessonPlanServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/LessonPlanService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.model.LessonPlan;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LessonPlanService {
    List<LessonPlan> obtenerMisPlanificaciones(String studentEmail);
    LessonPlan crearPlanificacion(
            String studentEmail,
            Long assignmentId,
            String schoolGrade,
            String mathAxis,
            String objective,
            String specificObjectives,
            String classActivities,
            MultipartFile file
    );
    void eliminarPlanificacion(Long id, String userEmail);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/LessonPlanServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.FileStorageService;
import com.ubb.practicas.service.LessonPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonPlanServiceImpl implements LessonPlanService {

    private final LessonPlanRepository lessonPlanRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final PortfolioDocumentRepository portfolioDocumentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private PracticeAssignment resolveAssignment(String email, Long explicitAssignmentId) {
        if (explicitAssignmentId != null) {
            Optional<PracticeAssignment> opt = practiceAssignmentRepository.findById(explicitAssignmentId);
            if (opt.isPresent()) {
                PracticeAssignment a = opt.get();
                if (a.isStudent(email) && a.getStatus() == AssignmentStatus.ACTIVA) {
                    return a;
                }
            }
            return null;
        }

        List<PracticeAssignment> activeAssignments = practiceAssignmentRepository.findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA);
        if (activeAssignments.isEmpty()) {
            return null;
        }
        return activeAssignments.stream()
                .max((a1, a2) -> Long.compare(a1.getId() != null ? a1.getId() : 0L, a2.getId() != null ? a2.getId() : 0L))
                .orElse(activeAssignments.get(0));
    }

    private boolean isUserAdmin(String email) {
        User user = userRepository.findByEmail(email);
        return user != null && user.getRole() != null && "Administrador".equalsIgnoreCase(user.getRole().getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> obtenerMisPlanificaciones(String studentEmail) {
        return lessonPlanRepository.findByPracticeAssignment_Student_Email(studentEmail);
    }

    @Override
    @Transactional
    public LessonPlan crearPlanificacion(
            String studentEmail,
            Long assignmentId,
            String schoolGrade,
            String mathAxis,
            String objective,
            String specificObjectives,
            String classActivities,
            MultipartFile file) {

        PracticeAssignment assignment = resolveAssignment(studentEmail, assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("No tienes una asignación de práctica activa.");
        }

        LessonPlan plan = new LessonPlan();
        plan.setPracticeAssignment(assignment);
        plan.setStatus(PlanStatus.BORRADOR);
        plan.setSchoolGrade(schoolGrade);
        plan.setMathAxis(MathAxis.valueOf(mathAxis));
        plan.setBaseLearningObjective(objective);
        plan.setObjectives(specificObjectives);
        plan.setContentJson(classActivities);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty() && file.getSize() > 0) {
            if (!"planificacion_sin_archivo.txt".equals(file.getOriginalFilename())) {
                String storedFileName = fileStorageService.storeFile(file);
                PortfolioDocument doc = new PortfolioDocument();
                doc.setPracticeAssignment(assignment);
                doc.setType(DocumentType.PLANIFICACION);
                doc.setFileName(file.getOriginalFilename());
                doc.setStoredFileName(storedFileName);
                doc.setContentType(file.getContentType());
                doc.setSizeBytes(file.getSize());
                doc.setUploadDate(LocalDateTime.now());

                portfolioDocumentRepository.save(doc);
                doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
                portfolioDocumentRepository.save(doc);

                plan.setDocument(doc);
            }
        }

        return lessonPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public void eliminarPlanificacion(Long id, String userEmail) {
        LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Planificación no encontrada"));

        boolean isAdmin = isUserAdmin(userEmail);
        if (!isAdmin && !plan.isOwnedBy(userEmail)) {
            throw new AccessDeniedException("No tiene permisos para eliminar esta planificación");
        }

        if (plan.getDocument() != null) {
            fileStorageService.deleteFile(plan.getDocument().getStoredFileName());
        }
        lessonPlanRepository.delete(plan);
    }
}
```

---

#### D. Service 3: `AuthService` & `AuthServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/AuthService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;

import java.util.Map;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    Map<String, String> forgotPassword(String email);
    Map<String, String> resetPassword(String tokenStr, String newPassword);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/AuthServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.dto.UserResponse;
import com.ubb.practicas.model.PasswordResetToken;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.PasswordResetTokenRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.security.JwtUtil;
import com.ubb.practicas.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository tokenRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User usuario = userRepository.findByEmail(request.getEmail());

        if (usuario == null) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new DisabledException("Cuenta desactivada");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        String roleName = usuario.getRole() != null ? usuario.getRole().getName() : "SinRol";
        String token = jwtUtil.generateToken(usuario.getEmail(), roleName);
        UserResponse userResponse = UserResponse.fromUser(usuario);

        return new AuthResponse(token, userResponse);
    }

    @Override
    @Transactional
    public Map<String, String> forgotPassword(String email) {
        if (email != null && !email.trim().isEmpty()) {
            User user = userRepository.findByEmail(email.trim());
            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(token);
                resetToken.setUser(user);
                resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
                tokenRepository.save(resetToken);

                // TR-02: Registrar en logs del servidor en desarrollo; NUNCA exponer el enlace en la respuesta HTTP
                log.info("Solicitud de recuperación para {}. Token de desarrollo: http://localhost:5173/reset-password?token={}", email, token);
            }
        }

        // BK-12 & TR-02: Retorno uniforme para prevenir enumeración de cuentas y account takeover
        return Map.of(
                "message", "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."
        );
    }

    @Override
    @Transactional
    public Map<String, String> resetPassword(String tokenStr, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenStr);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("El enlace de recuperación es inválido o no existe.");
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El enlace ha expirado. Solicita uno nuevo.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(token);

        return Map.of("message", "Contraseña restablecida exitosamente.");
    }
}
```

---

#### E. Service 4: `SubjectService` & `SubjectServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/SubjectService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.model.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> listarAsignaturas();
    Subject crearAsignatura(Subject subject);
    void eliminarAsignatura(Long id);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/SubjectServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Subject> listarAsignaturas() {
        return subjectRepository.findAll();
    }

    @Override
    @Transactional
    public Subject crearAsignatura(Subject subject) {
        subject.setId(null);

        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la asignatura es obligatorio");
        }
        if (subject.getName().trim().length() > 255) {
            throw new IllegalArgumentException("El nombre de la asignatura no puede superar los 255 caracteres");
        }
        if (subject.getCycle() == null) {
            throw new IllegalArgumentException("El ciclo de la asignatura es obligatorio");
        }
        subject.setName(subject.getName().trim());

        if (subject.getCode() != null) {
            String trimmedCode = subject.getCode().trim();
            if (trimmedCode.length() > 50) {
                throw new IllegalArgumentException("El código de la asignatura no puede superar los 50 caracteres");
            }
            subject.setCode(trimmedCode.isEmpty() ? null : trimmedCode);
        }

        if (subject.getProfessor() != null) {
            if (subject.getProfessor().getId() == null) {
                subject.setProfessor(null);
            } else if (!userRepository.existsById(subject.getProfessor().getId())) {
                throw new IllegalArgumentException("El profesor especificado no existe");
            }
        }

        try {
            return subjectRepository.save(subject);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Datos de asignatura inválidos o referencia no encontrada", e);
        }
    }

    @Override
    @Transactional
    public void eliminarAsignatura(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new NoSuchElementException("Asignatura no encontrada");
        }
        if (practiceAssignmentRepository != null && practiceAssignmentRepository.existsBySubjectId(id)) {
            throw new IllegalStateException("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica.");
        }
        try {
            subjectRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar la asignatura porque tiene dependencias en el sistema.", e);
        }
    }
}
```

---

#### F. Service 5: `PracticeAssignmentService` & `PracticeAssignmentServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/PracticeAssignmentService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.model.PracticeAssignment;

import java.util.List;

public interface PracticeAssignmentService {
    List<PracticeAssignment> listarAsignaciones();
    List<PracticeAssignment> listarPorColaborador(Long collaboratorId);
    List<PracticeAssignment> listarPorTutor(Long tutorId);
    PracticeAssignment crearAsignacion(PracticeAssignment assignment);
    void eliminarAsignacion(Long id);
    PracticeAssignment resolveActiveAssignment(String studentEmail, Long explicitAssignmentId);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/PracticeAssignmentServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.PracticeCenterRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PracticeAssignmentServiceImpl implements PracticeAssignmentService {

    private final PracticeAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final PracticeCenterRepository centerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> listarAsignaciones() {
        return assignmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> listarPorColaborador(Long collaboratorId) {
        return assignmentRepository.findByCollaboratorId(collaboratorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeAssignment> listarPorTutor(Long tutorId) {
        return assignmentRepository.findByTutorId(tutorId);
    }

    @Override
    @Transactional
    public PracticeAssignment crearAsignacion(PracticeAssignment assignment) {
        if (assignment.getStudent() == null || assignment.getStudent().getId() == null) {
            throw new IllegalArgumentException("El estudiante es obligatorio");
        }
        if (assignment.getSubject() == null || assignment.getSubject().getId() == null) {
            throw new IllegalArgumentException("La asignatura es obligatoria");
        }

        if (!userRepository.existsById(assignment.getStudent().getId())) {
            throw new IllegalArgumentException("El estudiante no existe en la base de datos");
        }
        if (!subjectRepository.existsById(assignment.getSubject().getId())) {
            throw new IllegalArgumentException("La asignatura no existe en la base de datos");
        }
        if (assignment.getCenter() != null && assignment.getCenter().getId() != null
                && !centerRepository.existsById(assignment.getCenter().getId())) {
            throw new IllegalArgumentException("El centro de práctica no existe");
        }

        assignment.setCreatedAt(LocalDateTime.now());
        if (assignment.getStatus() == null) {
            assignment.setStatus(AssignmentStatus.ACTIVA);
        }

        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void eliminarAsignacion(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new NoSuchElementException("Asignación no encontrada");
        }
        assignmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeAssignment resolveActiveAssignment(String email, Long explicitAssignmentId) {
        if (explicitAssignmentId != null) {
            Optional<PracticeAssignment> opt = assignmentRepository.findById(explicitAssignmentId);
            if (opt.isPresent()) {
                PracticeAssignment a = opt.get();
                if (a.isStudent(email) && a.getStatus() == AssignmentStatus.ACTIVA) {
                    return a;
                }
            }
            return null;
        }

        List<PracticeAssignment> activeAssignments = assignmentRepository.findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA);
        if (activeAssignments.isEmpty()) {
            return null;
        }
        return activeAssignments.stream()
                .max((a1, a2) -> Long.compare(a1.getId() != null ? a1.getId() : 0L, a2.getId() != null ? a2.getId() : 0L))
                .orElse(activeAssignments.get(0));
    }
}
```

---

#### G. Service 6: `PracticeCenterService` & `PracticeCenterServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/PracticeCenterService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.model.PracticeCenter;

import java.util.List;
import java.util.Optional;

public interface PracticeCenterService {
    List<PracticeCenter> listarCentros();
    Optional<PracticeCenter> obtenerPorId(Long id);
    PracticeCenter crearCentro(PracticeCenter center);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/PracticeCenterServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.repository.PracticeCenterRepository;
import com.ubb.practicas.service.PracticeCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PracticeCenterServiceImpl implements PracticeCenterService {

    private final PracticeCenterRepository centerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeCenter> listarCentros() {
        return centerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PracticeCenter> obtenerPorId(Long id) {
        return centerRepository.findById(id);
    }

    @Override
    @Transactional
    public PracticeCenter crearCentro(PracticeCenter center) {
        return centerRepository.save(center);
    }
}
```

---

#### H. Service 7: `RoleService` & `RoleServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/RoleService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    List<Role> obtenerRoles();
    Optional<Role> buscarPorNombre(String name);
    Optional<Role> buscarPorId(Long id);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/RoleServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Role> obtenerRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> buscarPorNombre(String name) {
        return Optional.ofNullable(roleRepository.findByName(name));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> buscarPorId(Long id) {
        return roleRepository.findById(id);
    }
}
```

---

#### I. Service 8: `UserService` (Interface) & `UserServiceImpl`

**File:** `src/main/java/com/ubb/practicas/service/UserService.java`
```java
package com.ubb.practicas.service;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> obtenerTodosUsuarios();
    List<User> listarUsuariosPorRol(String roleName);
    User obtenerPorEmail(String email);
    Optional<User> obtenerPorId(Long id);
    User crearUsuario(User nuevoUsuario) throws Exception;
    User actualizarUsuario(Long usuarioId, User datosActualizados) throws Exception;
    User actualizarRolUsuario(Long usuarioId, String roleName) throws Exception;
    User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception;
    User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request) throws Exception;
    void procesarArchivoMasivo(MultipartFile archivo) throws Exception;
    boolean isUserAdmin(String email);
}
```

**File:** `src/main/java/com/ubb/practicas/service/impl/UserServiceImpl.java`
```java
package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<User> obtenerTodosUsuarios() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listarUsuariosPorRol(String roleName) {
        return userRepository.findByRoleName(roleName);
    }

    @Override
    @Transactional(readOnly = true)
    public User obtenerPorEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> obtenerPorId(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdmin(String email) {
        User user = userRepository.findByEmail(email);
        return user != null && user.getRole() != null && "Administrador".equalsIgnoreCase(user.getRole().getName());
    }

    @Override
    @Transactional
    public User crearUsuario(User nuevoUsuario) throws Exception {
        if (userRepository.findByEmail(nuevoUsuario.getEmail()) != null) {
            throw new Exception("El email ya está en uso");
        }

        if (nuevoUsuario.getRole() != null && nuevoUsuario.getRole().getName() != null) {
            Role role = roleRepository.findByName(nuevoUsuario.getRole().getName());
            if (role == null) {
                throw new Exception("Rol no encontrado: " + nuevoUsuario.getRole().getName());
            }
            nuevoUsuario.setRole(role);
        }

        if (nuevoUsuario.getPasswordHash() != null) {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode(nuevoUsuario.getPasswordHash()));
        } else {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode("temporal123"));
        }

        nuevoUsuario.setCreatedAt(LocalDateTime.now());
        nuevoUsuario.setActive(true);

        return userRepository.save(nuevoUsuario);
    }

    @Override
    @Transactional
    public User actualizarUsuario(Long usuarioId, User datosActualizados) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        if (datosActualizados.getFullName() != null) {
            user.setFullName(datosActualizados.getFullName());
        }
        if (datosActualizados.getEmail() != null && !user.getEmail().equals(datosActualizados.getEmail())) {
            if (userRepository.findByEmail(datosActualizados.getEmail()) != null) {
                throw new Exception("El email ya está en uso");
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

    @Override
    @Transactional
    public User actualizarRolUsuario(Long usuarioId, String roleName) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new Exception("Rol no encontrado: " + roleName);
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        user.setActive(estado);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request) throws Exception {
        User user = userRepository.findByEmail(emailActual);
        if (user == null) {
            throw new Exception("Usuario no encontrado");
        }
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()) != null) {
                throw new Exception("El email ya está en uso por otro usuario");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
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

    private void procesarExcel(MultipartFile archivo) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                Cell cellName = row.getCell(0);
                Cell cellEmail = row.getCell(1);
                Cell cellPass = row.getCell(2);
                Cell cellRole = row.getCell(3);

                if (cellName == null || cellEmail == null || cellRole == null) continue;

                String fullName = cellName.getStringCellValue().trim();
                String email = cellEmail.getStringCellValue().trim();
                String password = cellPass != null ? cellPass.getStringCellValue().trim() : "temporal123";
                String roleName = cellRole.getStringCellValue().trim();

                guardarUsuarioBulk(fullName, email, password, roleName);
            }
        }
    }

    private void procesarCSV(MultipartFile archivo) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                String[] datos = linea.split(",");
                if (datos.length < 4) continue;

                String fullName = datos[0].trim();
                String email = datos[1].trim();
                String password = datos[2].trim();
                String roleName = datos[3].trim();

                guardarUsuarioBulk(fullName, email, password, roleName);
            }
        }
    }

    private void guardarUsuarioBulk(String fullName, String email, String password, String roleName) throws Exception {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new Exception("Rol no encontrado en archivo: " + roleName);
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            user = new User();
            user.setCreatedAt(LocalDateTime.now());
            user.setActive(true);
        }
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);

        userRepository.save(user);
    }
}
```

---

#### J. Refactored Controllers

##### 1. `PortfolioController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.dto.DownloadedDocument;
import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.service.LessonPlanService;
import com.ubb.practicas.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/portafolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final LessonPlanService lessonPlanService;

    @GetMapping("/evidencias")
    public ResponseEntity<?> obtenerMisEvidencias(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<PortfolioDocument> documentos = portfolioService.obtenerMisEvidencias(principal.getName());
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/planificaciones")
    public ResponseEntity<?> obtenerMisPlanificaciones(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<LessonPlan> planificaciones = lessonPlanService.obtenerMisPlanificaciones(principal.getName());
        return ResponseEntity.ok(planificaciones);
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            DownloadedDocument doc = portfolioService.descargarDocumento(id, principal.getName());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(doc.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.fileName() + "\"")
                    .body(doc.resource());
        } catch (AccessDeniedException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/evidencias/upload")
    public ResponseEntity<?> subirEvidencia(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            PortfolioDocument doc = portfolioService.subirEvidencia(principal.getName(), assignmentId, file);
            return ResponseEntity.ok(doc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al subir archivo: " + e.getMessage());
        }
    }

    @PostMapping("/planificaciones/upload")
    public ResponseEntity<?> crearPlanificacion(
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "classActivities", required = false) String classActivities) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            LessonPlan plan = lessonPlanService.crearPlanificacion(
                    principal.getName(), assignmentId, schoolGrade, mathAxis, objective, specificObjectives, classActivities, file);
            return ResponseEntity.ok(plan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/planificaciones/{id}")
    public ResponseEntity<?> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            lessonPlanService.eliminarPlanificacion(id, principal.getName());
            return ResponseEntity.ok("Planificación eliminada");
        } catch (AccessDeniedException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            portfolioService.eliminarDocumento(id, principal.getName());
            return ResponseEntity.ok("Documento eliminado");
        } catch (AccessDeniedException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar documento: " + e.getMessage());
        }
    }
}
```

##### 2. `AuthController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String tokenStr = request.get("token");
        String newPassword = request.get("newPassword");
        try {
            return ResponseEntity.ok(authService.resetPassword(tokenStr, newPassword));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
```

##### 3. `SubjectController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/asignaturas")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public List<Subject> listarAsignaturas() {
        return subjectService.listarAsignaturas();
    }

    @PostMapping
    public ResponseEntity<?> crearAsignatura(@RequestBody Subject subject) {
        try {
            return ResponseEntity.ok(subjectService.crearAsignatura(subject));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAsignatura(@PathVariable Long id) {
        try {
            subjectService.eliminarAsignatura(id);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
```

##### 4. `PracticeAssignmentController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.service.PracticeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class PracticeAssignmentController {

    private final PracticeAssignmentService practiceAssignmentService;

    @GetMapping
    public List<PracticeAssignment> listarAsignaciones() {
        return practiceAssignmentService.listarAsignaciones();
    }

    @GetMapping("/colaborador/{id}")
    public ResponseEntity<List<PracticeAssignment>> listarPorColaborador(@PathVariable Long id) {
        return ResponseEntity.ok(practiceAssignmentService.listarPorColaborador(id));
    }

    @GetMapping("/tutor/{id}")
    public ResponseEntity<List<PracticeAssignment>> listarPorTutor(@PathVariable Long id) {
        return ResponseEntity.ok(practiceAssignmentService.listarPorTutor(id));
    }

    @PostMapping
    public ResponseEntity<?> crearAsignacion(@RequestBody PracticeAssignment assignment) {
        try {
            return ResponseEntity.ok(practiceAssignmentService.crearAsignacion(assignment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarAsignacion(@PathVariable Long id) {
        try {
            practiceAssignmentService.eliminarAsignacion(id);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

##### 5. `PracticeCenterController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.service.PracticeCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/centros")
@RequiredArgsConstructor
public class PracticeCenterController {

    private final PracticeCenterService practiceCenterService;

    @GetMapping
    public List<PracticeCenter> listarCentros() {
        return practiceCenterService.listarCentros();
    }
}
```

##### 6. `RoleController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<Role> obtenerRoles() {
        return roleService.obtenerRoles();
    }
}
```

##### 7. `UserController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.model.User;
import com.ubb.practicas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> obtenerTodosUsuarios() {
        return userService.obtenerTodosUsuarios();
    }

    @GetMapping("/rol/{roleName}")
    public List<User> listarUsuariosPorRol(@PathVariable String roleName) {
        return userService.listarUsuariosPorRol(roleName);
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
    public ResponseEntity<?> actualizarRol(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String roleName = payload.get("roleName");
            if (roleName == null || roleName.isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del rol es requerido");
            }
            User updatedUser = userService.actualizarRolUsuario(id, roleName);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody User datosActualizados) {
        try {
            User updatedUser = userService.actualizarUsuario(id, datosActualizados);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean estado = payload.get("active");
            if (estado == null) {
                return ResponseEntity.badRequest().body("El campo 'active' es requerido");
            }
            User updatedUser = userService.cambiarEstadoUsuario(id, estado);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerMiPerfil(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }
        User user = userService.obtenerPorEmail(principal.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarMiPerfil(
            Principal principal,
            @RequestBody UserProfileUpdateRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }
        try {
            User updatedUser = userService.actualizarMiPerfil(principal.getName(), request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
```

##### 8. `ProfessorController.java`
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.TeachingRole;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profesor")
@RequiredArgsConstructor
public class ProfessorController {

    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final UserRepository userRepository;

    @GetMapping("/{professorId}/estudiantes")
    public ResponseEntity<?> obtenerEstudiantesPorProfesor(@PathVariable Long professorId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User targetProfessor = userRepository.findById(professorId).orElse(null);
        if (targetProfessor == null || targetProfessor.getRole() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        User caller = userRepository.findByEmail(principal.getName());
        if (caller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = caller.getRole() != null && "Administrador".equalsIgnoreCase(caller.getRole().getName());
        boolean isSelf = caller.getId() != null && caller.getId().equals(professorId);

        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado: no tiene permisos para consultar los estudiantes de este docente");
        }

        Optional<TeachingRole> teachingRole = TeachingRole.fromRoleName(targetProfessor.getRole().getName());
        if (teachingRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El usuario no tiene un rol docente válido");
        }

        List<PracticeAssignment> assignments = teachingRole.get().findAssignments(practiceAssignmentRepository, professorId);
        return ResponseEntity.ok(assignments);
    }
}
```

---

## 5. Verification Method

1. **Verify Complete Elimination of Field `@Autowired`:**
   Run in project root:
   ```bash
   git grep "@Autowired" src/main/java/com/ubb/practicas/controller/
   git grep "@Autowired" src/main/java/com/ubb/practicas/service/
   ```
   **Pass Condition:** Zero matching lines returned in `controller/` and `service/`.
2. **Compilation and Tests:**
   Run:
   ```bash
   ./mvnw test -Dtest=AuthControllerTest,PortfolioControllerTest,ProfessorControllerTest,SubjectControllerTest
   ```
   **Pass Condition:** All 4 controller test suites pass with 0 failures and 0 errors, validating that:
   - Status codes (200, 400, 401, 403, 404, 409) match precisely.
   - Headers (`Content-Disposition: attachment; filename=...`) match.
   - Security guards (`isAccessibleBy`, `isOwnedBy`, `isUserAdmin`, `TeachingRole`, active status) operate seamlessly inside services.
3. **Architecture & Packaging Verification:**
   Verify files exist in:
   - `src/main/java/com/ubb/practicas/service/` (Interfaces: `AuthService`, `PortfolioService`, `LessonPlanService`, `SubjectService`, `PracticeAssignmentService`, `PracticeCenterService`, `RoleService`, `UserService`)
   - `src/main/java/com/ubb/practicas/service/impl/` (Implementations: `AuthServiceImpl`, `PortfolioServiceImpl`, `LessonPlanServiceImpl`, `SubjectServiceImpl`, `PracticeAssignmentServiceImpl`, `PracticeCenterServiceImpl`, `RoleServiceImpl`, `UserServiceImpl`)
   - `src/main/java/com/ubb/practicas/dto/DownloadedDocument.java`
