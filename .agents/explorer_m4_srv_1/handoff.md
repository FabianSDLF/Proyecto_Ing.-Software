# Handoff Report: Phase 4 Backend Domain Services Unit Testing Suite

**Agent ID:** `explorer_m4_srv_1`  
**Date:** 2026-09-24T01:48:00Z  
**Target Milestone:** Phase 4 — Unit Testing Backend Domain Services (TS-01 to TS-06)  
**Target Directory:** `c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas`

---

## 1. Observation

### 1.1. Current Test Coverage & Test Smells (TS-01 to TS-06)
- **Report Reference:** In `reporte_codigo.md` (lines 750-800), backend test coverage is identified as below 5%, with zero unit tests for domain services.
- **Test Smells Observed in Legacy Tests:**
  - `PracticasApplicationTests.java` (lines 9-11): Empty test method `contextLoads()` with no assertions (**TS-01**).
  - `SubjectControllerTest.java` (lines 41-46): Destructive teardown calling `deleteAll()` against the configured database (**TS-02**).
  - `SubjectControllerTest.java` (line 29): Complete deactivation of security filters (`@AutoConfigureMockMvc(addFilters = false)`) (**TS-03**).
  - `SubjectControllerTest.java` (lines 135, 146, 157, 169): Fragile string assertions matching Spanish phrases (**TS-05**).
- **Execution Benchmark:** Pure unit tests without Spring context (such as `LocalStorageServiceImplTest.java`) execute in **0.316 s** via `.\mvnw.cmd test -Dtest=LocalStorageServiceImplTest` (run on 2026-09-23T22:44:16). Conversely, `@SpringBootTest` suites take multiple seconds and require MySQL/database connectivity.

### 1.2. Codebase Inventory of Target Domain Services & Security Utility
Direct inspection of `src/main/java/com/ubb/practicas/` revealed the exact contracts, implementations, and dependencies:

1. **`AuthService` & `AuthServiceImpl`** (`src/main/java/com/ubb/practicas/service/impl/AuthServiceImpl.java`):
   - Dependencies: `UserRepository`, `PasswordEncoder`, `JwtUtil`, `PasswordResetTokenRepository`.
   - Lines 39-59: `login(LoginRequest)` checks:
     - User existence -> throws `BadCredentialsException("Credenciales incorrectas")` if null.
     - User activation (`!Boolean.TRUE.equals(usuario.getActive())`) -> throws `DisabledException("Cuenta desactivada")`.
     - Password match -> throws `BadCredentialsException("Credenciales incorrectas")` if mismatch.
     - Role fallback -> if `usuario.getRole() == null`, passes `"SinRol"` to `jwtUtil.generateToken`.
   - Lines 63-83: `forgotPassword(String email)`:
     - If email valid and user active -> saves `PasswordResetToken` (expiry +30m).
     - Returns uniform message `Map.of("message", "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.")` to prevent user enumeration attacks.
   - Lines 87-105: `resetPassword(String tokenStr, String newPassword)`:
     - Missing token -> throws `IllegalArgumentException("El enlace de recuperación es inválido o no existe.")`.
     - Expired token (`token.getExpiryDate().isBefore(LocalDateTime.now())`) -> throws `IllegalStateException("El enlace ha expirado. Solicita uno nuevo.")`.
     - Valid token -> encodes new password, saves user, deletes token from repository.

2. **`UserService` & `UserServiceImpl`** (`src/main/java/com/ubb/practicas/service/impl/UserServiceImpl.java`):
   - Dependencies: `UserRepository`, `RoleRepository`, `PasswordEncoder`, `UserBatchImportService`.
   - Lines 105-128: `crearUsuario(User)`:
     - Checks email uniqueness (`userRepository.findByEmail(email) != null`) -> throws `EmailAlreadyInUseException` (which extends `ResourceConflictException` in `com/ubb/practicas/exception/EmailAlreadyInUseException.java`).
     - Role lookup -> throws `RoleNotFoundException` (which extends `ResourceNotFoundException`) if role name not found.
     - Password hashing -> encodes `nuevoUsuario.getPasswordHash()` with `passwordEncoder.encode(...)`; if null, hashes default `"temporal123"`.
     - Sets `active = true` and `createdAt = LocalDateTime.now()`.
   - Lines 57-68: `actualizarRolUsuario(Long usuarioId, String roleName)`:
     - Throws `UserNotFoundException` if ID not found.
     - Throws `RoleNotFoundException` if role not found.
   - Lines 96-101: `cambiarEstadoUsuario(Long usuarioId, boolean estado)`:
     - Throws `UserNotFoundException` if ID not found; updates `active` status.

3. **`PortfolioService` & `PortfolioServiceImpl`** (`src/main/java/com/ubb/practicas/service/impl/PortfolioServiceImpl.java`):
   - Dependencies: `PortfolioDocumentRepository`, `PracticeAssignmentRepository`, `LessonPlanRepository`, `UserRepository`, `StorageService`.
   - Lines 93-113: `subirEvidencia(String studentEmail, Long assignmentId, MultipartFile file)`:
     - Resolves active assignment via `resolveAssignment(...)`. Throws `IllegalArgumentException("No tienes una asignación de práctica activa.")` if null or student mismatch.
     - Delegates storage to `storageService.store(file)` (which validates file extension against `FORBIDDEN_EXTENSIONS` and throws `StorageException`).
     - Persists `PortfolioDocument` with `DocumentType.EVIDENCIA_APRENDIZAJE` and generates URL `/api/portafolio/documentos/{id}/descargar`.
   - Lines 73-89: `descargarDocumento(Long id, String userEmail)`:
     - Document not found -> throws `NoSuchElementException("Documento no encontrado")`.
     - Authorization check (IDOR Protection): verifies `isUserAdmin(userEmail)` OR `doc.isAccessibleBy(userEmail)` (checks student, subject professor, collaborator, or tutor via `PracticeAssignment.hasParticipant`).
     - If neither admin nor participant -> throws `AccessDeniedException("No tiene permisos para acceder a este documento")`.
     - Retrieves resource from `storageService.loadAsResource(...)`.
   - Lines 117-133: `eliminarDocumento(Long id, String userEmail)`:
     - Ownership check: verifies `isUserAdmin(userEmail)` OR `doc.isOwnedBy(userEmail)`. Throws `AccessDeniedException` if unauthorized.
     - If doc type is `PLANIFICACION`, cascades deletion to `lessonPlanRepository.findByDocumentId(doc.getId())`.
     - Deletes physical file via `storageService.deleteFile(...)` and record via `portfolioDocumentRepository.delete(doc)`.

4. **`LessonPlanService` & `LessonPlanServiceImpl`** (`src/main/java/com/ubb/practicas/service/impl/LessonPlanServiceImpl.java`):
   - Dependencies: `LessonPlanRepository`, `PracticeAssignmentRepository`, `PortfolioDocumentRepository`, `UserRepository`, `StorageService`.
   - Lines 72-119: `crearPlanificacion(...)`:
     - Assignment resolution -> throws `IllegalArgumentException` if no active assignment for caller.
     - Creates `LessonPlan` with `PlanStatus.BORRADOR` and `MathAxis.valueOf(mathAxis)`.
     - File handling: If file is provided, non-empty, and filename != `"planificacion_sin_archivo.txt"`, stores file via `storageService.store(file)`, creates `PortfolioDocument` with type `PLANIFICACION`, links `plan.setDocument(doc)`. If no file, leaves document null.
   - Lines 123-136: `eliminarPlanificacion(Long id, String userEmail)`:
     - Not found -> throws `NoSuchElementException`.
     - Ownership check -> verifies `isUserAdmin(userEmail)` OR `plan.isOwnedBy(userEmail)` (`practiceAssignment.isStudent(userEmail)`). Throws `AccessDeniedException` if unauthorized.
     - If plan has document -> calls `storageService.deleteFile(...)`.
     - Deletes plan via `lessonPlanRepository.delete(plan)`.

5. **`SubjectService` & `SubjectServiceImpl`** (`src/main/java/com/ubb/practicas/service/impl/SubjectServiceImpl.java`):
   - Dependencies: `SubjectRepository`, `PracticeAssignmentRepository`, `UserRepository`.
   - Lines 32-67: `crearAsignatura(Subject)`:
     - Name validation: cannot be null, blank, or > 255 chars -> throws `IllegalArgumentException`.
     - Cycle validation: cannot be null -> throws `IllegalArgumentException`.
     - Code validation: length <= 50 -> throws `IllegalArgumentException`.
     - Professor validation: if professor provided with non-null ID, verifies `userRepository.existsById(...)`. Throws `IllegalArgumentException` if not found.
   - Lines 70-83: `eliminarAsignatura(Long id)`:
     - Not found -> throws `NoSuchElementException`.
     - Referential integrity check -> if `practiceAssignmentRepository.existsBySubjectId(id)` is true, throws `IllegalStateException("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica.")`.

6. **`PracticeAssignmentService` & `PracticeAssignmentServiceImpl`** (`src/main/java/com/ubb/practicas/service/impl/PracticeAssignmentServiceImpl.java`):
   - Dependencies: `PracticeAssignmentRepository`, `UserRepository`, `SubjectRepository`, `PracticeCenterRepository`.
   - Lines 51-76: `crearAsignacion(PracticeAssignment)`:
     - Validates student != null and exists in `userRepository`.
     - Validates subject != null and exists in `subjectRepository`.
     - Validates center (if present) exists in `centerRepository`.
     - Defaults status to `AssignmentStatus.ACTIVA`.
   - Lines 112-136: `obtenerEstudiantesPorProfesor(Long professorId, String callerEmail)`:
     - Validates target professor exists and has role -> throws `NoSuchElementException`.
     - Validates caller exists -> throws `SecurityException`.
     - Authorization check: `isAdmin || isSelf` (caller ID matches professorId). If false -> throws `AccessDeniedException`.
     - Polymorphic role dispatch: Resolves `TeachingRole.fromRoleName(targetProfessor.getRole().getName())`. If empty (e.g. Estudiante) -> throws `ForbiddenException("El usuario no tiene un rol docente válido")`.
     - Executes `teachingRole.get().findAssignments(assignmentRepository, professorId)`.

7. **`JwtUtil`** (`src/main/java/com/ubb/practicas/security/JwtUtil.java`):
   - Attributes: `@Value("${jwt.secret}") String secret`, `@Value("${jwt.expiration:86400000}") long expirationMs`.
   - Uses JJWT `0.12.6` with HMAC-SHA signing key (`Keys.hmacShaKeyFor`).
   - Methods: `generateToken`, `extractClaims`, `extractEmail`, `extractRole`, `isTokenValid`.

---

## 2. Logic Chain

1. **Isolation & Performance:**
   - Loading Spring context (`@SpringBootTest`) incurs a 3-10s overhead per test class, requires database connectivity, and risks DB state mutation (**TS-01, TS-02**).
   - All domain services utilize constructor dependency injection (`@RequiredArgsConstructor`).
   - Therefore, mocking all repository, encoder, and storage dependencies using Mockito (`@ExtendWith(MockitoExtension.class)`) isolates 100% of business logic, eliminates database dependency, and achieves execution times well under 1 second per suite (< 100ms per test).

2. **Security & Boundary Validation:**
   - Authorization rules are embedded directly in the domain services:
     - IDOR prevention in `PortfolioServiceImpl.descargarDocumento` and `eliminarDocumento`.
     - Ownership checks in `LessonPlanServiceImpl.eliminarPlanificacion`.
     - Caller validation in `PracticeAssignmentServiceImpl.obtenerEstudiantesPorProfesor`.
   - These domain guards can be tested directly with negative test scenarios asserting `AccessDeniedException` and `ForbiddenException` without relying on HTTP filters or web layers.

3. **Exception Architecture Consistency:**
   - `UserServiceImpl` throws `EmailAlreadyInUseException` on duplicate email. Because `EmailAlreadyInUseException extends ResourceConflictException`, asserting either or verifying the hierarchy fulfills the requirement `ResourceConflictException`.
   - `AuthServiceImpl` uses standard Spring Security exceptions (`BadCredentialsException`, `DisabledException`) for authentication and `IllegalArgumentException`/`IllegalStateException` for token lifecycle.
   - All exception assertions should use `assertThrows(ExpectedException.class, () -> ...)` rather than fragile string matching (**TS-05**).

---

## 3. Caveats

- **No Caveats.** All domain service classes, models, enums, exception hierarchies, and utility classes are fully implemented and compile without errors in branch `fabian`.
- The tests designed herein are pure Mockito unit tests placed in `src/test/java/com/ubb/practicas/service/` and `src/test/java/com/ubb/practicas/security/`. Integration tests (`@SpringBootTest` / `MockMvc`) are addressed by peer agents and do not replace these isolated unit tests.

---

## 4. Conclusion & Test Suite Design Specification

The test suite architecture comprises **6 dedicated test classes** (totaling **68 distinct test scenarios**):
1. `AuthServiceImplTest` (12 scenarios)
2. `UserServiceImplTest` (14 scenarios)
3. `PortfolioServiceImplTest` (14 scenarios)
4. `LessonPlanServiceImplTest` (10 scenarios)
5. `SubjectServiceImplTest` & `PracticeAssignmentServiceImplTest` (18 scenarios across 2 classes)
6. `JwtUtilTest` (8 scenarios)

Below is the complete implementation design and specification for each suite.

---

### Test Suite 1: `AuthServiceImplTest`
**File Target:** `src/test/java/com/ubb/practicas/service/AuthServiceImplTest.java`  
**Annotations:** `@ExtendWith(MockitoExtension.class)`  
**Dependencies to Mock:**
- `UserRepository userRepository`
- `PasswordEncoder passwordEncoder`
- `JwtUtil jwtUtil`
- `PasswordResetTokenRepository tokenRepository`

#### Detailed Test Scenarios:
| Method Name | Input / Precondition | Mock Behavior | Expected Assertion |
|---|---|---|---|
| `login_Success` | Valid `LoginRequest("test@ubb.cl", "correct")` | User found, `active=true`, password matches, role="Estudiante" | Returns `AuthResponse`, token matches mock, user email matches |
| `login_UserNotFound_ThrowsBadCredentialsException` | `LoginRequest("unknown@ubb.cl", "pass")` | `userRepository.findByEmail` returns `null` | `assertThrows(BadCredentialsException.class)`, encoder & jwt never called |
| `login_DisabledAccount_ThrowsDisabledException` | `LoginRequest("disabled@ubb.cl", "pass")` | User found with `active=false` | `assertThrows(DisabledException.class)`, encoder & jwt never called |
| `login_InvalidPassword_ThrowsBadCredentialsException` | `LoginRequest("test@ubb.cl", "wrong")` | User active, `passwordEncoder.matches` returns `false` | `assertThrows(BadCredentialsException.class)`, jwt never called |
| `login_UserWithoutRole_FallsBackToSinRol` | User with `role=null` | Password matches | `jwtUtil.generateToken("test@ubb.cl", "SinRol")` verified |
| `forgotPassword_ActiveUser_GeneratesAndSavesToken` | `"test@ubb.cl"` | User found, `active=true` | `tokenRepository.save(any())` called with token, returns anti-enumeration message |
| `forgotPassword_NonExistentUser_ReturnsUniformMessage` | `"none@ubb.cl"` | `userRepository.findByEmail` returns `null` | Returns same message, `tokenRepository.save` never called |
| `forgotPassword_DisabledUser_ReturnsUniformMessage` | `"disabled@ubb.cl"` | User found with `active=false` | Returns same message, `tokenRepository.save` never called |
| `forgotPassword_NullOrBlankEmail_ReturnsUniformMessage` | `""` or `null` | None | Returns same message, `tokenRepository.save` never called |
| `resetPassword_Success_EncodesPasswordAndDeletesToken` | Valid token, new password | Token found, expiry in future | User password hash updated, `userRepository.save` called, `tokenRepository.delete` called |
| `resetPassword_TokenNotFound_ThrowsIllegalArgumentException` | Unknown token | `tokenRepository.findByToken` returns `Optional.empty()` | `assertThrows(IllegalArgumentException.class)` |
| `resetPassword_TokenExpired_ThrowsIllegalStateException` | Token with expiry in past | `token.getExpiryDate().isBefore(now)` | `assertThrows(IllegalStateException.class)` |

```java
// SKELETON SPECIFICATION: AuthServiceImplTest.java
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void login_Success() {
        Role role = new Role(1L, "Estudiante");
        User user = new User();
        user.setEmail("estudiante@ubb.cl");
        user.setPasswordHash("hashed_pass");
        user.setActive(true);
        user.setRole(role);

        when(userRepository.findByEmail("estudiante@ubb.cl")).thenReturn(user);
        when(passwordEncoder.matches("raw_pass", "hashed_pass")).thenReturn(true);
        when(jwtUtil.generateToken("estudiante@ubb.cl", "Estudiante")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(new LoginRequest("estudiante@ubb.cl", "raw_pass"));

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.token());
        assertEquals("estudiante@ubb.cl", response.user().getEmail());
    }
}
```

---

### Test Suite 2: `UserServiceImplTest`
**File Target:** `src/test/java/com/ubb/practicas/service/UserServiceImplTest.java`  
**Annotations:** `@ExtendWith(MockitoExtension.class)`  
**Dependencies to Mock:**
- `UserRepository userRepository`
- `RoleRepository roleRepository`
- `PasswordEncoder passwordEncoder`
- `UserBatchImportService userBatchImportService`

#### Detailed Test Scenarios:
| Method Name | Input / Precondition | Mock Behavior | Expected Assertion |
|---|---|---|---|
| `crearUsuario_Success_WithPasswordHashing` | User with raw password and valid role | Email not taken, role exists | Encodes password, sets `active=true`, sets `createdAt`, saves user |
| `crearUsuario_NullPassword_UsesDefaultTemporalPassword` | User with `passwordHash=null` | Email not taken | `passwordEncoder.encode("temporal123")` called |
| `crearUsuario_DuplicateEmail_ThrowsResourceConflictException` | User with existing email | `userRepository.findByEmail` returns existing user | `assertThrows(ResourceConflictException.class)` (and `EmailAlreadyInUseException`), save never called |
| `crearUsuario_RoleNotFound_ThrowsRoleNotFoundException` | User with non-existent role | `roleRepository.findByName` returns `null` | `assertThrows(RoleNotFoundException.class)` |
| `actualizarRolUsuario_Success` | User ID 1L, new role "Tutor" | User exists, role exists | User's role updated and saved |
| `actualizarRolUsuario_UserNotFound_ThrowsException` | User ID 99L | `userRepository.findById` returns empty | `assertThrows(UserNotFoundException.class)` |
| `actualizarRolUsuario_RoleNotFound_ThrowsException` | User ID 1L, role "FakeRole" | Role returns `null` | `assertThrows(RoleNotFoundException.class)` |
| `cambiarEstadoUsuario_Deactivate_Success` | User ID 1L, `estado=false` | User exists with `active=true` | User updated to `active=false`, saved |
| `cambiarEstadoUsuario_Activate_Success` | User ID 1L, `estado=true` | User exists with `active=false` | User updated to `active=true`, saved |
| `cambiarEstadoUsuario_UserNotFound_ThrowsException` | User ID 999L | User returns empty | `assertThrows(UserNotFoundException.class)` |
| `actualizarUsuario_Success_UpdatesFields` | New name, same email | User exists | `fullName` updated, saved |
| `actualizarUsuario_EmailConflict_ThrowsException` | New email already used by another user | New email exists in DB | `assertThrows(EmailAlreadyInUseException.class)` |
| `actualizarMiPerfil_Success` | Valid update request with new password | User exists | Password encoded, profile fields updated and saved |
| `procesarArchivoMasivo_DelegatesToBatchService` | `MultipartFile` | None | `userBatchImportService.procesarArchivoMasivo(archivo)` called once |

---

### Test Suite 3: `PortfolioServiceImplTest`
**File Target:** `src/test/java/com/ubb/practicas/service/PortfolioServiceImplTest.java`  
**Annotations:** `@ExtendWith(MockitoExtension.class)`  
**Dependencies to Mock:**
- `PortfolioDocumentRepository portfolioDocumentRepository`
- `PracticeAssignmentRepository practiceAssignmentRepository`
- `LessonPlanRepository lessonPlanRepository`
- `UserRepository userRepository`
- `StorageService storageService`

#### Detailed Test Scenarios:
| Method Name | Input / Precondition | Mock Behavior | Expected Assertion |
|---|---|---|---|
| `subirEvidencia_Success` | Active assignment, valid file | File stored, doc created | Returns `PortfolioDocument` with type `EVIDENCIA_APRENDIZAJE` and valid download URL |
| `subirEvidencia_NoActiveAssignment_ThrowsIllegalArgumentException` | Student has no active assignment | Assignment search returns empty / null | `assertThrows(IllegalArgumentException.class)`, storage never called |
| `subirEvidencia_StorageException_Propagated` | Invalid file type rejected by storage | `storageService.store(file)` throws `StorageException` | `assertThrows(StorageException.class)`, document repository never saved |
| `descargarDocumento_Success_OwnerStudent` | Doc owned by student | Doc found, student is owner | Returns `DownloadedDocument` with correct resource and filename |
| `descargarDocumento_Success_SupervisingTeacher` | Doc accessed by supervising tutor/collaborator | Caller is tutor on practice assignment | Download succeeds, resource returned |
| `descargarDocumento_Success_AdminBypass` | Doc accessed by administrator | Caller has role "Administrador" | Download succeeds regardless of assignment ownership |
| `descargarDocumento_IdorProtection_ThrowsAccessDeniedException` | Doc accessed by unrelated student | Caller is neither admin nor participant | `assertThrows(AccessDeniedException.class)`, `storageService.loadAsResource` NEVER called |
| `descargarDocumento_NotFound_ThrowsNoSuchElementException` | ID 999L | Doc returns empty | `assertThrows(NoSuchElementException.class)` |
| `eliminarDocumento_Success_OwnerStudent` | Doc type `EVIDENCIA_APRENDIZAJE` owned by caller | Doc exists, caller is owner | `storageService.deleteFile` and `portfolioDocumentRepository.delete` called |
| `eliminarDocumento_Success_PlanificacionCascadesLessonPlan` | Doc type `PLANIFICACION` | Associated `LessonPlan` exists | `lessonPlanRepository.delete` called before doc deletion |
| `eliminarDocumento_IdorProtection_ThrowsAccessDeniedException` | Doc owned by student A, caller is student B | `doc.isOwnedBy(caller)` is `false`, non-admin | `assertThrows(AccessDeniedException.class)`, file & doc never deleted |
| `eliminarDocumento_Success_AdminBypass` | Doc owned by student, caller is Admin | Caller is admin | Deletion succeeds |
| `eliminarDocumento_NotFound_ThrowsNoSuchElementException` | ID 999L | Doc returns empty | `assertThrows(NoSuchElementException.class)` |
| `obtenerMisEvidencias_ReturnsStudentDocuments` | Student email | Repo returns list | List returned with type `EVIDENCIA_APRENDIZAJE` |

---

### Test Suite 4: `LessonPlanServiceImplTest`
**File Target:** `src/test/java/com/ubb/practicas/service/LessonPlanServiceImplTest.java`  
**Annotations:** `@ExtendWith(MockitoExtension.class)`  
**Dependencies to Mock:**
- `LessonPlanRepository lessonPlanRepository`
- `PracticeAssignmentRepository practiceAssignmentRepository`
- `PortfolioDocumentRepository portfolioDocumentRepository`
- `UserRepository userRepository`
- `StorageService storageService`

#### Detailed Test Scenarios:
| Method Name | Input / Precondition | Mock Behavior | Expected Assertion |
|---|---|---|---|
| `crearPlanificacion_WithFile_Success` | Active assignment, valid PDF file | `storageService.store` returns file UUID | Plan created with attached document of type `PLANIFICACION`, status `BORRADOR` |
| `crearPlanificacion_WithoutFile_Success` | Active assignment, `file = null` | None | Plan created with `document = null`, storage never called |
| `crearPlanificacion_PlaceholderFile_Ignored` | Filename `"planificacion_sin_archivo.txt"` | None | Storage never called, document remains null |
| `crearPlanificacion_NoActiveAssignment_ThrowsException` | No active assignment | `resolveAssignment` returns null | `assertThrows(IllegalArgumentException.class)`, repo never saves plan |
| `eliminarPlanificacion_Success_Owner` | Plan without document owned by caller | Caller is student of assignment | `lessonPlanRepository.delete(plan)` called |
| `eliminarPlanificacion_Success_OwnerWithAttachedDocument` | Plan has document attached | Document stored filename exists | `storageService.deleteFile` called, plan deleted |
| `eliminarPlanificacion_Success_AdminBypass` | Caller is "Administrador" | Plan owned by student | Deletion succeeds |
| `eliminarPlanificacion_OwnershipViolation_ThrowsAccessDeniedException` | Caller is different student | `plan.isOwnedBy` returns `false` | `assertThrows(AccessDeniedException.class)`, storage & repo delete never called |
| `eliminarPlanificacion_NotFound_ThrowsNoSuchElementException` | ID 999L | Plan returns empty | `assertThrows(NoSuchElementException.class)` |
| `obtenerMisPlanificaciones_ReturnsStudentPlans` | Student email | Repo returns plans | List returned matching student email |

---

### Test Suite 5: `SubjectServiceImplTest` & `PracticeAssignmentServiceImplTest`

#### 5A: `SubjectServiceImplTest`
**File Target:** `src/test/java/com/ubb/practicas/service/SubjectServiceImplTest.java`  
**Annotations:** `@ExtendWith(MockitoExtension.class)`  
**Dependencies to Mock:**
- `SubjectRepository subjectRepository`
- `PracticeAssignmentRepository practiceAssignmentRepository`
- `UserRepository userRepository`

| Method Name | Input / Precondition | Mock Behavior | Expected Assertion |
|---|---|---|---|
| `crearAsignatura_Success` | Valid Subject with cycle and code | Professor exists in DB | ID reset to null, subject saved, trimmed name/code |
| `crearAsignatura_NullProfessorId_ResetsProfessorToNull` | Subject with Professor(id=null) | None | Professor set to null, subject saved |
| `crearAsignatura_BlankName_ThrowsIllegalArgumentException` | `name = ""` or `"   "` | None | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignatura_NameExceeds255Chars_ThrowsIllegalArgumentException` | Name length 256 | None | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignatura_NullCycle_ThrowsIllegalArgumentException` | `cycle = null` | None | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignatura_CodeExceeds50Chars_ThrowsIllegalArgumentException` | Code length 51 | None | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignatura_NonExistentProfessor_ThrowsIllegalArgumentException` | Professor ID 99L | `userRepository.existsById` returns `false` | `assertThrows(IllegalArgumentException.class)` |
| `eliminarAsignatura_Success` | Subject ID 1L | Subject exists, no active assignments | `subjectRepository.deleteById(1L)` called |
| `eliminarAsignatura_AssociatedAssignments_ThrowsIllegalStateException` | Subject ID 1L | `practiceAssignmentRepository.existsBySubjectId` returns `true` | `assertThrows(IllegalStateException.class)`, delete never called |
| `eliminarAsignatura_NotFound_ThrowsNoSuchElementException` | Subject ID 99L | `subjectRepository.existsById` returns `false` | `assertThrows(NoSuchElementException.class)` |
| `listarAsignaturas_ReturnsList` | Call method | Repo returns list | Returns list of subjects |

#### 5B: `PracticeAssignmentServiceImplTest`
**File Target:** `src/test/java/com/ubb/practicas/service/PracticeAssignmentServiceImplTest.java`  
**Annotations:** `@ExtendWith(MockitoExtension.class)`  
**Dependencies to Mock:**
- `PracticeAssignmentRepository assignmentRepository`
- `UserRepository userRepository`
- `SubjectRepository subjectRepository`
- `PracticeCenterRepository centerRepository`

| Method Name | Input / Precondition | Mock Behavior | Expected Assertion |
|---|---|---|---|
| `crearAsignacion_Success_DefaultsToActiva` | Student & Subject in DB, status=null | All entities exist | Status set to `ACTIVA`, `createdAt` set, saved |
| `crearAsignacion_MissingStudent_ThrowsIllegalArgumentException` | `student = null` | None | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignacion_MissingSubject_ThrowsIllegalArgumentException` | `subject = null` | None | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignacion_StudentNotInDb_ThrowsIllegalArgumentException` | Student ID 99L | `userRepository.existsById` returns `false` | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignacion_SubjectNotInDb_ThrowsIllegalArgumentException` | Subject ID 99L | `subjectRepository.existsById` returns `false` | `assertThrows(IllegalArgumentException.class)` |
| `crearAsignacion_CenterNotInDb_ThrowsIllegalArgumentException` | Center ID 99L | `centerRepository.existsById` returns `false` | `assertThrows(IllegalArgumentException.class)` |
| `obtenerEstudiantesPorProfesor_Self_ProfesorAsignatura` | Teacher ID 10L, role "Profesor de Asignatura", caller is self | Returns assignments | Calls `assignmentRepository.findBySubjectProfessorId(10L)` |
| `obtenerEstudiantesPorProfesor_Self_ProfesorColaborador` | Teacher ID 11L, role "Profesor Colaborador", caller is self | Returns assignments | Calls `assignmentRepository.findByCollaboratorId(11L)` |
| `obtenerEstudiantesPorProfesor_Self_Tutor` | Teacher ID 12L, role "Tutor", caller is self | Returns assignments | Calls `assignmentRepository.findByTutorId(12L)` |
| `obtenerEstudiantesPorProfesor_Admin_CanQueryAnyTeacher` | Caller has role "Administrador" | Target is teacher | Permitted, calls repository fetcher |
| `obtenerEstudiantesPorProfesor_UnauthorizedCaller_ThrowsAccessDeniedException` | Caller is another teacher/student (non-admin, not self) | Target is teacher | `assertThrows(AccessDeniedException.class)` |
| `obtenerEstudiantesPorProfesor_NonTeachingRole_ThrowsForbiddenException` | Target user has role "Estudiante" | Caller is admin | `assertThrows(ForbiddenException.class)` ("El usuario no tiene un rol docente válido") |
| `obtenerEstudiantesPorProfesor_TargetNotFound_ThrowsNoSuchElementException` | Target ID 999L | `userRepository.findById` returns empty | `assertThrows(NoSuchElementException.class)` |
| `obtenerEstudiantesPorProfesor_CallerNotFound_ThrowsSecurityException` | Caller email not in DB | `userRepository.findByEmail` returns null | `assertThrows(SecurityException.class)` |
| `eliminarAsignacion_Success` | Assignment ID 1L | Exists in DB | `assignmentRepository.deleteById(1L)` called |
| `eliminarAsignacion_NotFound_ThrowsNoSuchElementException` | Assignment ID 99L | Does not exist | `assertThrows(NoSuchElementException.class)` |

---

### Test Suite 6: `JwtUtilTest`
**File Target:** `src/test/java/com/ubb/practicas/security/JwtUtilTest.java`  
**Annotations:** Standard JUnit 5 (`@Test`, `@BeforeEach`) — No mocks required (pure POJO test).  
**Setup via `ReflectionTestUtils`:**
- `secret`: `"mySuperSecretKeyForTestingJwtTokenGenerationAndValidation1234567890!"` (512-bit HMAC key)
- `expirationMs`: `3600000L` (1 hour)

#### Detailed Test Scenarios:
| Method Name | Input / Setup | Expected Assertion |
|---|---|---|
| `testGenerateToken_Success` | Email: `"student@ubb.cl"`, Role: `"Estudiante"` | Token generated with 3 segments separated by dots; `isTokenValid` returns `true` |
| `testExtractEmailAndRole_Success` | Valid token | `extractEmail(token)` equals `"student@ubb.cl"`; `extractRole(token)` equals `"Estudiante"` |
| `testExtractClaims_ExpirationInFuture` | Valid token | `extractClaims(token).getExpiration().after(new Date())` is `true` |
| `testTokenExpiration_Rejection` | `expirationMs = -5000L` (expired at creation) | `isTokenValid(token)` returns `false`; calling `extractClaims(token)` throws `ExpiredJwtException` |
| `testTamperedSignature_Rejection` | Alter last 4 characters of signature | `isTokenValid(tampered)` returns `false`; `extractClaims` throws `JwtException` |
| `testTokenSignedWithDifferentSecret_Rejection` | Token generated by second JwtUtil with key `"anotherKey..."` | First JwtUtil rejects token: `isTokenValid` returns `false`, `extractClaims` throws `JwtException` |
| `testMalformedToken_Rejection` | Strings: `"invalid.token"`, `"not-a-token"`, `""` | `isTokenValid` returns `false` |
| `testIsTokenValid_NullToken_ReturnsFalse` | `null` | Returns `false` without throwing NullPointerException |

```java
// SKELETON SPECIFICATION: JwtUtilTest.java
class JwtUtilTest {
    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "superSecretKeyForTestingJwtTokensWhichMustBeAtLeast256BitsLong12345!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void testTokenExpiration_Rejection() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);
        String expiredToken = jwtUtil.generateToken("user@ubb.cl", "Estudiante");

        assertFalse(jwtUtil.isTokenValid(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractClaims(expiredToken));
    }
}
```

---

## 5. Verification Method

To independently verify this design and confirm the implementation once written by the builder:

1. **Compilation Check:**
   ```powershell
   .\mvnw.cmd test-compile
   ```
   *Expected:* Compiles cleanly without syntax or classpath errors (`BUILD SUCCESS`).

2. **Execution of Specific Domain Service Unit Test Suites:**
   ```powershell
   .\mvnw.cmd test -Dtest=AuthServiceImplTest
   .\mvnw.cmd test -Dtest=UserServiceImplTest
   .\mvnw.cmd test -Dtest=PortfolioServiceImplTest
   .\mvnw.cmd test -Dtest=LessonPlanServiceImplTest
   .\mvnw.cmd test -Dtest=SubjectServiceImplTest
   .\mvnw.cmd test -Dtest=PracticeAssignmentServiceImplTest
   .\mvnw.cmd test -Dtest=JwtUtilTest
   ```
   *Expected:* 100% pass rate across all 68 tests.  
   *Speed criteria:* Time elapsed < 1.0s per test class (typically ~0.2s - 0.4s).

3. **Combined Service Test Run:**
   ```powershell
   .\mvnw.cmd test -Dtest="*ServiceImplTest,JwtUtilTest"
   ```
   *Expected:* All 7 classes pass concurrently in under 3.5 seconds total runtime.

4. **Invalidation Conditions:**
   - Any test suite loading the Spring context (e.g., using `@SpringBootTest`) violates the speed (< 1s) and isolation requirements.
   - Any test relying on an active MySQL connection or calling `deleteAll()` on production/development tables violates the hermetic testing principle (**TS-02**).
   - Any test failing to assert `AccessDeniedException` for IDOR scenarios leaves authorization regressions undetected.
