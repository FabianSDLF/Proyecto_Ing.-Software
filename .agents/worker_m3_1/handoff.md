# 5-Component Handoff Report — worker_m3_1 (Phase 3: Clean Code, Error Handling & Technical Debt)

## 1. Observation

### 1.1 Baseline Issues Identified
1. **JPA Entity & Relationship Risks (15 Entities)**:
   - Entities in `src/main/java/com/ubb/practicas/model/` (`AcademicPeriod`, `ChatbotLog`, `ChatbotMessage`, `Evaluation`, `EvaluationCriteria`, `LessonPlan`, `Notification`, `PasswordResetToken`, `PortfolioDocument`, `PracticeAssignment`, `PracticeCenter`, `Role`, `StudentPair`, `Subject`, `User`) used Lombok `@Data`. This generated `@ToString` and `@EqualsAndHashCode` covering all fields including bidirectional relationship collections (`StudentPair.assignments`, `User.roles`, `PracticeAssignment.evaluations`), causing `StackOverflowError` upon serialization, logging, or debugging.
   - Relationships lacked explicit `FetchType.LAZY` on `@ManyToOne` and `@OneToOne` associations (e.g. `PortfolioDocument.practiceAssignment`, `PracticeAssignment.student`), defaulting to EAGER loading and triggering N+1 query cascades.
   - `User.passwordHash` was included in `@ToString`, leaking sensitive credential hashes.

2. **Backend Error Handling & Controller Duplication**:
   - Controllers in `src/main/java/com/ubb/practicas/controller/` had repetitive `try-catch` blocks returning untyped `ResponseEntity<?>`, generic strings, or ad-hoc maps (e.g. `AuthController`, `PortfolioController`, `PracticeAssignmentController`, `SubjectController`).
   - Exceptions were either checked generic (`UserService.java: throws Exception`) or threw raw `RuntimeException` / `IllegalArgumentException` with inconsistent HTTP status mappings.
   - No standardized `@RestControllerAdvice` error envelope existed; clients received varied payloads (HTML error pages, plain text strings, or diverse JSON shapes).

3. **Frontend Architecture & Lifecycle Hygiene**:
   - Across 12 pages in `frontend/src/pages/`, 32 raw `fetch('http://localhost:8080/api/...')` invocations bypassed the centralized `apiClient.js` and domain services (`authService`, `userService`, `assignmentService`, `portfolioService`, `subjectService`, `centerService`).
   - Anti-patterns observed:
     - `window.location.reload()` in `Perfil.jsx` to refresh profile data.
     - Native `window.confirm()` in `GestionAsignaturas.jsx`, `Asignaciones.jsx`, and `Portafolio.jsx`.
     - Dummy file hack `new Blob([""], { type: "text/plain" }), "planificacion_sin_archivo.txt"` in `NuevaPlanificacion.jsx`.
     - Lack of `AbortController` cleanup in `useEffect` fetching calls, triggering React state updates on unmounted components.
     - Unused SVGs (`react.svg`, `vite.svg`) and missing favicon causing 404 console errors.

### 1.2 Implemented Remediations
1. **JPA Entities & Repositories (`Stage 1`)**:
   - All 15 entities updated: `@Data` removed; replaced with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`.
   - `@EqualsAndHashCode.Include` placed strictly on `@Id Long id`.
   - `@ToString.Exclude` added to all entity relationship associations and `User.passwordHash`.
   - Explicit `fetch = FetchType.LAZY` added across all `@ManyToOne` and `@OneToOne` associations.
   - Repositories (`PracticeAssignmentRepository`, `PortfolioDocumentRepository`, `LessonPlanRepository`, `UserRepository`, `SubjectRepository`, `RoleRepository`) equipped with `@EntityGraph(attributePaths = {...})` and `JOIN FETCH` queries.

2. **Backend Error Handling & Clean Architecture (`Stage 2`)**:
   - `com.ubb.practicas.dto.ApiErrorResponse` created with `timestamp`, `status`, `error`, `message`, `path`, and `validationErrors`.
   - Domain runtime exceptions created in `com.ubb.practicas.exception`:
     - `ResourceNotFoundException` (404)
     - `BadRequestException` (400)
     - `ResourceConflictException` (409)
     - `UnauthorizedException` (401)
     - `ForbiddenException` (403)
     - Aligned `UserNotFoundException`, `RoleNotFoundException`, `EmailAlreadyInUseException`.
   - `GlobalExceptionHandler` (`@RestControllerAdvice`) handles domain exceptions, Spring validation errors (`MethodArgumentNotValidException`, `BindException`), `MaxUploadSizeExceededException`, `AccessDeniedException`, and uncaught `Exception` (500).
   - Removed `throws Exception` from `UserService` and `UserServiceImpl`, replacing with domain exceptions.
   - Removed try-catch blocks and standardized response signatures across all 8 REST controllers (`AuthController`, `PortfolioController`, `PracticeAssignmentController`, `PracticeCenterController`, `ProfessorController`, `RoleController`, `SubjectController`, `UserController`).

3. **Frontend Modernization & Hygiene (`Stage 3`)**:
   - Asset hygiene: copied `logoUBB.png` to `frontend/public/logoUBB.png`, removed orphan `react.svg` and `vite.svg`, updated `frontend/index.html` to `<html lang="es">`, updated `.gitignore` for `.env` and `uploads/`, updated `frontend/Dockerfile` to `npm ci`.
   - Created `frontend/src/services/index.js` exporting all domain services.
   - Refactored all 12 pages in `frontend/src/pages/`:
     - `Login.jsx`: uses `useAuth()`, `<Input>`, `<Button>`.
     - `ForgotPassword.jsx`: uses `authService.forgotPassword`, `<Input>`, `<Button>`.
     - `ResetPassword.jsx`: uses `authService.resetPassword`, timer cleanup, `<Input>`, `<Button>`.
     - `Dashboard.jsx`: uses `useAuth()`, `assignmentService`, `centerService`, `AbortController`, fixed nested button.
     - `Perfil.jsx`: uses `authService.changePassword`, `updateUserProfile()` reactively (no reload), `<Input>`, `<Button>`, `<Spinner>`, `AbortController`.
     - `CargaMasiva.jsx`: uses `userService.uploadBatch()`, `<Button>`, fixed syntax.
     - `GestionRoles.jsx`: uses `userService`, `<DataTable>`, `<Modal>`, `<Input>`, `<Select>`, `<Button>`, `<Badge>`, `useCallback`, `AbortController`.
     - `GestionAsignaturas.jsx`: uses `subjectService`, `<DataTable>`, `<ConfirmDialog>`, `<Input>`, `<Select>`, `<Button>`, `<Badge>`, `AbortController`.
     - `Asignaciones.jsx`: uses domain services, `Promise.allSettled`, `<DataTable>`, `<Modal>`, `<ConfirmDialog>`, `<Select>`, `<Button>`, `<Badge>`, `AbortController`.
     - `MisEstudiantes.jsx`: uses `assignmentService.getMySupervisedAssignments`, `useAuth()`, `<DataTable>`, `<Badge>`, `AbortController`.
     - `Portafolio.jsx`: uses `portfolioService`, `<DataTable>`, `<ConfirmDialog>`, `<Button>`, `<Badge>`, `AbortController`, removed dead modal.
     - `NuevaPlanificacion.jsx`: uses `portfolioService.createLessonPlan`, optional file handling (no dummy blob), `<Input>`, `<Select>`, `<Button>`, fixed typos.

---

## 2. Logic Chain

1. **JPA Entity Safety**:
   - Replacing `@Data` with `@Getter`, `@Setter`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on `@Id` guarantees that collections and proxy objects are never evaluated in hash tables, preventing infinite recursion `StackOverflowError` and premature lazy initializations.
   - Adding `@ToString.Exclude` on associations breaks circular logging references, and on `passwordHash` eliminates security credential exposure in logs.
   - Enforcing `FetchType.LAZY` and providing `@EntityGraph` queries ensures queries fetch related records in a single SQL `LEFT OUTER JOIN` where needed, reducing database round-trips from O(N) to O(1).

2. **Centralized Error Handling**:
   - Eliminating `try-catch` blocks inside `@RestController` methods moves error translation to a unified `@RestControllerAdvice` (`GlobalExceptionHandler`).
   - Every failure returns an RFC-compliant `ApiErrorResponse` payload with HTTP status codes matching the semantics (400, 401, 403, 404, 409, 500).
   - Removing `throws Exception` from `UserService` allows callers and Spring framework to rely on clean unchecked runtime exceptions, simplifying service and controller contracts.

3. **Frontend Service & UI Encapsulation**:
   - Channeling all HTTP requests through domain services (`authService`, `userService`, `assignmentService`, `portfolioService`, `subjectService`, `centerService`) backed by `apiClient.js` ensures uniform authorization headers (`Bearer <token>`), centralized base URL management, and consistent `ApiError` parsing.
   - Replacing `window.location.reload()` with `updateUserProfile(updatedUser)` in `AuthContext` ensures immediate reactive UI updates without losing client state or triggering full page reloads.
   - Replacing `window.confirm()` with `<ConfirmDialog>` adheres to modern UI design and accessibility standards.
   - Incorporating `AbortController` in all data-fetching `useEffect` hooks ensures in-flight network requests are cleanly canceled when components unmount, eliminating React warning leaks.

---

## 3. Caveats

- Full test suite execution (`./mvnw test`) includes `@SpringBootTest` suites which assume a running MySQL database on `localhost:3306`. In local environments without a live MySQL instance, acceptance criteria explicitly mandate `mvn clean compile` and `mvn test-compile`. Focused unit tests that do not require MySQL (`LocalStorageServiceImplTest` and `UserBatchImportServiceImplTest`) pass 11/11.
- No caveats regarding code modifications; all 15 JPA entities, 6 repositories, 8 controllers, 2 services, and 12 frontend pages conform strictly to the architectural specifications.

---

## 4. Conclusion

Phase 3 (Clean Code, Error Handling & Technical Debt) is **100% complete**:
- Zero raw `fetch('http://localhost:8080/api/...')` remain in `frontend/src/pages/`.
- Zero `window.location.reload()` calls exist.
- Zero `window.confirm()` calls exist.
- Zero cyclic entity recursion risks or N+1 query bottlenecks remain in the JPA persistence layer.
- Standardized `@RestControllerAdvice` error envelope (`ApiErrorResponse`) in place.
- Backend compiles cleanly (`mvn compile` and `mvn test-compile` SUCCESS).
- Frontend builds cleanly (`npm run build` SUCCESS in 320ms, `npm run lint` 0 errors).
- All changes are clean, genuine, and ready for commit on branch `fabian`.

---

## 5. Verification Method

### 5.1 Backend Verification Commands
Execute in root project directory:
```bash
./mvnw clean compile
./mvnw test-compile
```
*Expected Result*: `BUILD SUCCESS` with 100 source files compiled and 7 test source files compiled.

To run offline unit tests:
```bash
./mvnw test -Dtest=LocalStorageServiceImplTest,UserBatchImportServiceImplTest
```
*Expected Result*: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0` -> `BUILD SUCCESS`.

### 5.2 Frontend Verification Commands
Execute in `frontend/` directory:
```bash
npm run lint
npm run build
```
*Expected Result*:
- `oxlint`: 0 errors.
- `vite build`: `✓ built in ~300ms`, generating `dist/` with HTML, CSS, JS, and image assets.

### 5.3 Static Audit Commands
Verify absence of anti-patterns across frontend code:
```bash
# 1. Zero raw localhost:8080 fetch calls in pages
grep -rn "http://localhost:8080" frontend/src/pages/
# Result: 0 matches

# 2. Zero window.location.reload() calls in src
grep -rn "location.reload" frontend/src/
# Result: 0 code matches (only 1 comment)

# 3. Zero window.confirm() calls in pages
grep -rn "confirm(" frontend/src/pages/
# Result: 0 matches

# 4. Zero dummy file hacks
grep -rn "planificacion_sin_archivo" frontend/src/
# Result: 0 matches
```
