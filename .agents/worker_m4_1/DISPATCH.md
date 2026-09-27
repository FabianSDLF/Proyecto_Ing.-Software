## 2026-09-23T22:50:00Z
You are worker_m4_1, a teamwork_preview_worker.
Your assigned working directory is:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/worker_m4_1

CRITICAL MANDATORY INSTRUCTION:
You MUST read ORIGINAL_REQUEST.md before starting work:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/ORIGINAL_REQUEST.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Key Reference Blueprint Files from Phase 4 Explorers:
- Service Unit Tests Blueprint: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m4_srv_1/handoff.md
- Controller Integration & Test Smell Blueprint: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m4_ctrl_2/handoff.md
- Frontend Automated Testing Blueprint: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m4_fe_3/handoff.md
- PROJECT.md: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/PROJECT.md
- Diagnostic Report: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/reporte_codigo.md

Your Mission for Phase 4 (Quality Assurance & Automated Tests):
1. **Branch Verification**:
   - Ensure the git repository is on branch `fabian`. All changes must be on branch `fabian`.

2. **Backend Test Dependencies & Isolation (TS-01, TS-02)**:
   - Check `pom.xml`: ensure `h2` and `spring-security-test` are present in `<dependencies>` under `<scope>test</scope>`.
   - Create `src/test/resources/application-test.properties` configured with H2 in-memory DB (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL`) and `ddl-auto=update` or `create-drop`.
   - Fix `PracticasApplicationTests.java` (TS-01): annotate with `@ActiveProfiles("test")`, verify context loading with explicit bean assertions (`assertNotNull`).
   - Fix `SubjectControllerTest.java` (TS-02, TS-03, TS-04, TS-05):
     - Remove destructive `@BeforeEach`/`@AfterEach` `deleteAll()` that purged real DB. Annotate class with `@Transactional` and `@ActiveProfiles("test")`.
     - Enable security filters (remove `addFilters = false`).
     - Use `@WithMockUser(roles = "ADMINISTRADOR")`, `@WithMockUser(roles = "ESTUDIANTE")`, and unauthenticated requests to verify role-based security.
     - Replace latch concurrency with deterministic transactional tests.
     - Replace raw Spanish string assertions with JSONPath assertions on `ApiErrorResponse` (`$.status`, `$.error`, `$.message`).

3. **Backend Service Unit Tests (per explorer_m4_srv_1 handoff)**:
   - Implement JUnit 5 + Mockito unit tests in `src/test/java/com/ubb/practicas/service/`:
     - `AuthServiceTest.java`
     - `UserServiceTest.java`
     - `PortfolioServiceTest.java`
     - `LessonPlanServiceTest.java`
     - `SubjectServiceTest.java`
     - `PracticeAssignmentServiceTest.java`
   - Implement `JwtUtilTest.java` in `src/test/java/com/ubb/practicas/security/`.

4. **Backend Controller Integration Tests (per explorer_m4_ctrl_2 handoff)**:
   - Implement `AuthControllerTest.java` and `UserControllerTest.java` in `src/test/java/com/ubb/practicas/controller/` with `MockMvc`, `@ActiveProfiles("test")`, and `@WithMockUser`.

5. **Frontend Automated Testing & Playwright Config (TS-06, per explorer_m4_fe_3 handoff)**:
   - Configure frontend test environment in `frontend/package.json` and `frontend/vite.config.js` (or lightweight runner script that executes node/vitest tests for components and utilities).
   - Implement frontend tests in `frontend/src/tests/`:
     - `apiClient.test.js`
     - `Button.test.jsx`
     - `Modal.test.jsx`
     - `ConfirmDialog.test.jsx`
     - `AuthContext.test.jsx`
   - Create `frontend/playwright.config.js` and `frontend/e2e/auth.spec.js` for E2E specification.
   - Verify `"test"` script in `frontend/package.json` passes without errors.

6. **Build & Test Verification**:
   - Backend: run `./mvnw test` — verify ALL unit and integration tests pass with 0 failures and 0 errors. Run `./mvnw clean compile` to verify clean compilation.
   - Frontend: run `npm test` and `npm run build` in `frontend/` — verify tests pass and build succeeds cleanly.

7. **Git Commit**:
   - Stage and commit all changes cleanly to branch `fabian` with descriptive message (e.g. `feat(test): implement Phase 4 test isolation, service unit tests, controller security tests, and frontend test suite`).
   - Verify `git status` shows clean working tree.

8. **Deliverable**:
   - Write comprehensive report to:
     `c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/worker_m4_1/handoff.md`
   - Notify parent orchestrator via `send_message`.
