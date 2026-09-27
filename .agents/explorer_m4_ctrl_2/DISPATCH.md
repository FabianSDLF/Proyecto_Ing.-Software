## 2026-09-24T01:39:50Z

Investigate and design isolated integration tests for REST controllers and fix test smells for Phase 4:
1. Test Environment Isolation (TS-01, TS-02): Check `src/test/resources/application-test.properties` (or `application.properties`) configuring H2 in-memory database (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL`) with `@ActiveProfiles("test")`. Fix `PracticasApplicationTests.java`: replace empty `contextLoads()` with explicit context assertions (`assertNotNull(...)` on beans). Fix `SubjectControllerTest`: eliminate `deleteAll()` teardown that touches real DB; use `@Transactional` rollback.
2. Security Verification & `@WithMockUser` (TS-03): Enable security filters in `SubjectControllerTest` (remove `addFilters = false`). Use `@WithMockUser(roles = "ADMINISTRADOR")`, `@WithMockUser(roles = "ESTUDIANTE")`, and unauthenticated requests to prove role-based access control works.
3. Concurrency & Flaky Test Fix (TS-04): Replace non-deterministic `CountDownLatch` and fixed timers with deterministic transactional tests.
4. Structured JSON Assertions (TS-05): Replace Spanish plaintext string assertions (`containsString(...)`) with HTTP status checks and JSONPath assertions against `ApiErrorResponse` (`$.status`, `$.error`, `$.message`).
5. Controller Test Coverage: Design tests for `AuthControllerTest` and `UserControllerTest` with `MockMvc`.
