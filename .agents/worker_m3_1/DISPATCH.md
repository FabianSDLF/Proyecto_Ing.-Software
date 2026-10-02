## 2026-09-23T22:01:05Z
You are worker_m3_1, a teamwork_preview_worker.
Your assigned working directory is:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/worker_m3_1

CRITICAL MANDATORY INSTRUCTION:
You MUST read ORIGINAL_REQUEST.md before starting work:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/ORIGINAL_REQUEST.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Key Reference Blueprint Files from Phase 3 Explorers:
- Error Handling Blueprint: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_err_1/handoff.md
- JPA & Domain Blueprint: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_jpa_2/handoff.md
- Frontend & Hygiene Blueprint: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_fe_3/handoff.md
- PROJECT.md: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/PROJECT.md
- Diagnostic Report: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/reporte_codigo.md

Your Mission for Phase 3 (Clean Code, Error Handling & Technical Debt):
1. **Branch Verification**:
   - Ensure the git repository is on branch `fabian`. If not, checkout `fabian`. All changes must be made on branch `fabian`.

2. **Backend Error Handling & Clean Code (per explorer_m3_err_1 handoff)**:
   - Create `ApiErrorResponse` DTO in `com.ubb.practicas.dto` (fields: `timestamp`, `status`, `error`, `message`, `path`, optional `validationErrors`).
   - Create custom domain runtime exceptions in `com.ubb.practicas.exception`:
     - `ResourceNotFoundException` (404)
     - `BadRequestException` (400)
     - `ResourceConflictException` (409)
     - `UnauthorizedException` (401)
     - `ForbiddenException` (403)
     - `StorageException` (500)
   - Create `GlobalExceptionHandler` with `@RestControllerAdvice` in `com.ubb.practicas.exception`:
     - Handles custom exceptions above with appropriate HTTP statuses.
     - Handles `MethodArgumentNotValidException` (400 with detailed field validation map).
     - Handles Spring Security exceptions (`AccessDeniedException` -> 403, `BadCredentialsException`/`DisabledException` -> 401/403).
     - Handles generic `Exception.class` -> 500 with sanitized message.
   - Refactor all 8 controllers (`AuthController`, `PortfolioController`, `PracticeAssignmentController`, `PracticeCenterController`, `ProfessorController`, `RoleController`, `SubjectController`, `UserController`):
     - Remove redundant, repetitive `try-catch (Exception e)` blocks and let exceptions propagate naturally to `GlobalExceptionHandler`.
     - Standardize return types to DTOs or clean entities, eliminating raw plaintext error strings.
     - Sane `UserService` and `AuthService` method signatures (remove `throws Exception`).

3. **Backend JPA Entities, Lombok & N+1 Optimization (per explorer_m3_jpa_2 handoff)**:
   - Sane all 15 JPA entities in `src/main/java/com/ubb/practicas/model/`:
     - Replace `@Data` with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on `@Id`.
     - Add `@ToString.Exclude` to all relationship fields and on `User.passwordHash` to prevent infinite recursion, `StackOverflowError`, and credential leaks.
     - Change all 16 `@ManyToOne` and `@OneToOne` associations across all entities to explicit `FetchType.LAZY`.
   - Optimize Repositories in `src/main/java/com/ubb/practicas/repository/`:
     - Use `@EntityGraph` (or `JOIN FETCH`) in `PracticeAssignmentRepository`, `PortfolioDocumentRepository`, `LessonPlanRepository`, `UserRepository`, and `SubjectRepository` where associated entities need to be fetched in a single query, preventing N+1 queries.
     - Sane return types in `UserRepository` and `RoleRepository` to return `Optional<T>`.

4. **Frontend Architecture & Hygiene (per explorer_m3_fe_3 handoff)**:
   - Migrate all 12 views in `frontend/src/pages/` to consume the domain services from `src/services/` (`authService`, `portfolioService`, `assignmentService`, `subjectService`, `userService`, `centerService`) and `apiClient.js`:
     - Eliminate all 32 occurrences of raw `fetch('http://localhost:8080/api/...')` with hardcoded localhost and manual Bearer headers.
   - Adopt reusable UI components from `src/components/ui/` (`Button`, `Modal`, `Input`, `DataTable`, `Badge`, `Spinner`, `ConfirmDialog`).
   - Fix `Perfil.jsx`: remove `window.location.reload()` antipattern; use `updateUserProfile()` from `useAuth()`.
   - Fix lifecycle hygiene: add `AbortController` in `useEffect` for data-fetching pages (`Dashboard`, `MisEstudiantes`, `GestionRoles`, `GestionAsignaturas`, `Portafolio`, `Asignaciones`).
   - Clean dead code: remove unreachable modal JSX in `Portafolio.jsx`, remove dummy simulated file hack in `NuevaPlanificacion.jsx`, clean orphan Vite template assets (`react.svg`), add missing favicon in `frontend/public/` to avoid 404s, update `.gitignore` for `.env` and `uploads/`.

5. **Compilation & Verification**:
   - Backend: run `mvn clean compile` or `./mvnw compile` to verify 100% successful Java compilation with 0 errors.
   - Frontend: run `npm run build` in `frontend/` to verify 100% clean Vite build with 0 errors.
   - Tests: run `mvn test-compile` to ensure test classes compile.

6. **Git Commit**:
   - Cleanly stage and commit all changes to branch `fabian` with a descriptive message (e.g. `feat(clean-code): implement Phase 3 error handling, JPA Lombok sanitization, N+1 optimization, and frontend hygiene`).
   - Verify `git status` shows clean working tree.

7. **Deliverable**:
   - Write comprehensive report to:
     `c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/worker_m3_1/handoff.md`
   - Notify parent orchestrator via `send_message`.
