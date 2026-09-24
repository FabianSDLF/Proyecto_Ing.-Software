# 5-Component Handoff & Code Review Report — reviewer_m3_1

**Target**: Phase 3 Backend Refactoring (Commit `1c773f8` on branch `fabian`)  
**Role**: Reviewer & Adversarial Critic  
**Review Verdict**: **APPROVE**  
**Date**: 2026-09-23T22:38:40Z  

---

## 1. Observation

### 1.1 Independent Build and Compilation Verification
- Command: `./mvnw clean compile`  
  - Result: `BUILD SUCCESS` (5.405 s). Compiled 100 source files to `target/classes`. Zero compilation errors.
- Command: `./mvnw test-compile`  
  - Result: `BUILD SUCCESS` (4.443 s). Compiled 7 test source files to `target/test-classes`.
- Command: `./mvnw test "-Dtest=LocalStorageServiceImplTest,UserBatchImportServiceImplTest,GlobalExceptionHandlerAdversarialTest,JpaEntityAdversarialTest"`  
  - Result: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0` -> `BUILD SUCCESS` (4.501 s).

### 1.2 Inspection of Global Error Handling Layer
- **`com.ubb.practicas.dto.ApiErrorResponse`**:
  - Implements RFC 7807 compliant fields: `timestamp` (`LocalDateTime`), `status` (`int`), `error` (`String`), `message` (`String`), `path` (`String`), and optional `validationErrors` (`Map<String, String>`).
  - Equipped with `@JsonInclude(JsonInclude.Include.NON_NULL)` and `@JsonInclude(JsonInclude.Include.NON_EMPTY)`.
- **`com.ubb.practicas.exception.GlobalExceptionHandler`** (`@RestControllerAdvice`):
  - HTTP 404 (Not Found): maps `ResourceNotFoundException`, `UserNotFoundException`, `RoleNotFoundException`, `StorageFileNotFoundException`, `NoSuchElementException`.
  - HTTP 400 (Bad Request): maps `BadRequestException`, `IllegalArgumentException`, `FileProcessingException`, `UnsupportedFileFormatException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `MaxUploadSizeExceededException`.
  - HTTP 400 (Validation): maps `MethodArgumentNotValidException` with structured `validationErrors` map extracted from `FieldError` objects.
  - HTTP 401 (Unauthorized): maps `UnauthorizedException`, `BadCredentialsException`, `SecurityException`.
  - HTTP 403 (Forbidden): maps `AccessDeniedException`, `ForbiddenException`, `DisabledException`.
  - HTTP 409 (Conflict): maps `ResourceConflictException`, `EmailAlreadyInUseException`, `IllegalStateException`, `DataIntegrityViolationException`.
  - HTTP 500 (Internal Server Error): maps `StorageException` and uncaught `Exception.class`.
- **Information Leakage Checks**:
  - `DataIntegrityViolationException` root message is explicitly sanitized at lines 181–183:
    ```java
    if (ex instanceof DataIntegrityViolationException) {
        message = "Conflicto de integridad de datos en la base de datos.";
    }
    ```
    This prevents raw SQL schema details, table names, or database constraints from reaching API consumers.
  - Generic `Exception.class` handler at lines 209–218 masks error details with a fixed message: `"Ha ocurrido un error interno inesperado en el servidor."`. The actual exception is logged via `log.error(...)` on the server and no stack traces are emitted in the response body.
  - Custom domain exceptions (`ResourceNotFoundException`, `BadRequestException`, `ResourceConflictException`, `UnauthorizedException`, `ForbiddenException`) cleanly extend standard Java `RuntimeException`.

### 1.3 Inspection of REST Controllers (8 Controllers)
All 8 controllers in `src/main/java/com/ubb/practicas/controller/` were inspected:
1. `AuthController.java` (38 lines): 0 try-catch blocks. Returns typed `ResponseEntity<AuthResponse>` and `ResponseEntity<Map<String, String>>`.
2. `PortfolioController.java` (107 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<PortfolioDocument>>`, `ResponseEntity<List<LessonPlan>>`, `ResponseEntity<Resource>`, `ResponseEntity<PortfolioDocument>`, `ResponseEntity<LessonPlan>`, and `ResponseEntity<Map<String, String>>`. Checks `if (principal == null) throw new UnauthorizedException("Sesión no autorizada");`.
3. `PracticeAssignmentController.java` (46 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<PracticeAssignment>>`, `ResponseEntity<PracticeAssignment>`, `ResponseEntity<Map<String, String>>`.
4. `PracticeCenterController.java` (23 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<PracticeCenter>>`.
5. `ProfessorController.java` (30 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<PracticeAssignment>>`. Throws `UnauthorizedException` when principal is null.
6. `RoleController.java` (25 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<Role>>`.
7. `SubjectController.java` (36 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<Subject>>`, `ResponseEntity<Subject>`, `ResponseEntity<Map<String, String>>`.
8. `UserController.java` (94 lines): 0 try-catch blocks. Returns typed `ResponseEntity<List<User>>`, `ResponseEntity<User>`, `ResponseEntity<Map<String, String>>`. Input validation triggers `BadRequestException` when parameters are missing.
- **Static Analysis Search**:
  - `grep -rn "catch" src/main/java/com/ubb/practicas/controller/`: **0 matches**.
  - `grep -rn "ResponseEntity<?>" src/main/java/com/ubb/practicas/controller/`: **0 matches**.

### 1.4 Inspection of JPA Entities (15 Entities)
All 15 entities in `src/main/java/com/ubb/practicas/model/` were verified individually:
- `AcademicPeriod`: Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`, `@EqualsAndHashCode.Include` on `@Id Long id`. No associations.
- `ChatbotLog`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `lessonPlan`.
- `ChatbotMessage`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `chatbotLog`.
- `Evaluation`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `practiceAssignment` and `evaluator`.
- `EvaluationCriteria`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `evaluation`.
- `LessonPlan`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `practiceAssignment` and `parentPlan`; `@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)` and `@ToString.Exclude` on `document`.
- `Notification`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `targetUser`.
- `PasswordResetToken`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `user`.
- `PortfolioDocument`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `practiceAssignment`.
- `PracticeAssignment`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `student`, `subject`, `center`, `collaborator`, `tutor`, `period`.
- `PracticeCenter`: Lombok annotations identical; `@EqualsAndHashCode.Include` on `@Id Long id`. No associations.
- `Role`: Lombok annotations identical; `@EqualsAndHashCode.Include` on `@Id Long id`. No associations.
- `StudentPair`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `assignment1`, `assignment2`, `period`.
- `Subject`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `professor`.
- `User`: Lombok annotations identical; `@ManyToOne(fetch = FetchType.LAZY)` and `@ToString.Exclude` on `role`. Crucially: `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` and `@ToString.Exclude` on `passwordHash`.

### 1.5 Inspection of Repositories & N+1 Query Elimination
- `PracticeAssignmentRepository`:
  - `findAll()`, `findById()` decorated with `@EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "center", "collaborator", "tutor", "period"})`.
  - `findBySubjectProfessorId()`, `findByCollaboratorId()`, `findByTutorId()` decorated with `@EntityGraph(attributePaths = {"student", "student.role", "subject", "center", "collaborator", "tutor", "period"})`.
  - `findByStudentEmail()` and `findByStudentEmailAndStatus()` decorated with `@EntityGraph(attributePaths = {"student", "subject", "center", "collaborator", "tutor", "period"})`.
- `PortfolioDocumentRepository`:
  - `findById()` decorated with `@EntityGraph(attributePaths = {"practiceAssignment", "practiceAssignment.student", "practiceAssignment.collaborator", "practiceAssignment.tutor", "practiceAssignment.subject", "practiceAssignment.subject.professor"})`.
  - `findByPracticeAssignment_Student_Email` and `findByPracticeAssignment_Student_EmailAndType` use explicit `JOIN FETCH` queries.
- `LessonPlanRepository`:
  - `findByPracticeAssignment_Student_Email` uses `LEFT JOIN FETCH lp.document JOIN FETCH lp.practiceAssignment pa JOIN FETCH pa.student s`.
  - `findById` and `findByDocumentId` use `@EntityGraph` for `practiceAssignment`, `student`, and `document`.
- `UserRepository`:
  - `findAll()`, `findById()`, `findByEmail()`, `findOptionalByEmail()`, and `findByRoleName()` use `@EntityGraph(attributePaths = {"role"})`.
- `SubjectRepository`:
  - `findAll()` and `findById()` use `@EntityGraph(attributePaths = {"professor", "professor.role"})`.

---

## 2. Logic Chain

1. **Safety from Cyclic Recursion**:
   - Observations 1.4 show that Lombok `@Data` was completely eliminated from all 15 JPA entities.
   - `@ToString.Exclude` on all relational associations breaks circular reference graphs (e.g. `LessonPlan.parentPlan`, `PracticeAssignment.student`).
   - `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on `@Id` prevents Hibernate proxy evaluation and collection iteration during hashing, eliminating `StackOverflowError` in `HashSet` and `HashMap`.

2. **Clean REST Architecture & Unified Error Translation**:
   - Observations 1.2 and 1.3 show that 100% of redundant `try-catch` blocks and `ResponseEntity<?>` returns were excised from all 8 controllers.
   - Any runtime failure naturally bubbles up to `GlobalExceptionHandler`, returning an RFC 7807 compliant `ApiErrorResponse` payload with correct HTTP semantics (400, 401, 403, 404, 409, 500).
   - Sensitive details such as SQL constraint errors (`DataIntegrityViolationException`) and internal stack traces (`Exception.class`) are masked with neutral user-facing messages.

3. **Performance & N+1 Prevention**:
   - Observations 1.4 and 1.5 establish that default association fetching is configured to `FetchType.LAZY` across all `@ManyToOne` and `@OneToOne` relations.
   - For all read operations exposed to the frontend, repositories declare explicit `@EntityGraph` and `JOIN FETCH` directives. This guarantees that relational graphs are fetched in single SQL `LEFT OUTER JOIN` statements rather than cascading O(N) secondary queries.

4. **Credential Security**:
   - In `User.java`, `passwordHash` is protected at two layers:
     - `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` prevents password hashes from being serialized into outgoing JSON responses.
     - `@ToString.Exclude` prevents password hashes from being printed in logs or debug traces.

5. **Adversarial Integrity**:
   - The test suite includes genuine adversarial tests (`JpaEntityAdversarialTest`, `GlobalExceptionHandlerAdversarialTest`) that directly test edge cases (deep recursive parent-plan chains of 1000 items, null exception messages, parameter validation mappings, set deduplication).
   - No mock facades or hardcoded test bypasses exist.

---

## 3. Caveats

- **Caveat 1 — Transient Entity Set Deduplication**: Because `@EqualsAndHashCode` relies exclusively on `@Id`, two unsaved (transient) instances of an entity where `id == null` will evaluate to equal and share the same hash code (`0`). This is standard idiomatic JPA design, but callers should persist entities before adding multiple distinct instances to a `Set`.
- **Caveat 2 — Live MySQL Integration Tests**: Tests requiring a live MySQL database instance (e.g. `@SpringBootTest` full application contexts) were not executed in this offline sandbox. However, all domain logic, mock MVC controller slices, entity behavior, file storage, and batch import unit tests pass 23/23 without database dependencies.

---

## 4. Conclusion & Verdict

### Review Verdict: **APPROVE**

The Phase 3 backend changes on branch `fabian` (commit `1c773f8`) meet all requirements and architectural constraints:
- All 15 JPA entities are clean, sanitized, and immune to recursion and N+1 cascading.
- All 8 REST controllers are free of redundant try-catch blocks and wildcards.
- Centralized `GlobalExceptionHandler` guarantees RFC-compliant error structures and prevents information leakage.
- Independent build (`./mvnw clean compile`) and test compilation (`./mvnw test-compile`) pass with 0 errors.
- 23 unit and adversarial test cases execute and pass with 100% success.
- Integrity check: **PASS** (zero integrity violations, no dummy facades, no hardcoded cheating).

---

## 5. Verification Method

To independently verify this evaluation:

1. **Verify Backend Build**:
   ```powershell
   ./mvnw clean compile
   ./mvnw test-compile
   ```
   *Expected*: `BUILD SUCCESS` with 100 main source files and 7 test source files.

2. **Verify Offline Unit and Adversarial Test Suite**:
   ```powershell
   ./mvnw test "-Dtest=LocalStorageServiceImplTest,UserBatchImportServiceImplTest,GlobalExceptionHandlerAdversarialTest,JpaEntityAdversarialTest"
   ```
   *Expected*: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0` -> `BUILD SUCCESS`.

3. **Verify Zero Try-Catch and Zero Wildcard Returns in Controllers**:
   ```powershell
   git grep "catch" src/main/java/com/ubb/practicas/controller/
   git grep "ResponseEntity<?>" src/main/java/com/ubb/practicas/controller/
   ```
   *Expected*: 0 matches.

4. **Verify Lombok Sanitization across Entities**:
   ```powershell
   git grep "@Data" src/main/java/com/ubb/practicas/model/
   ```
   *Expected*: 0 matches.
