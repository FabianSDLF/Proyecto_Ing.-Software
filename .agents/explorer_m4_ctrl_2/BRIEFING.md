# BRIEFING — 2026-09-24T01:52:00Z

## Mission
Investigate and design isolated integration tests for REST controllers and resolve test smells TS-01 to TS-05 for Phase 4.

## 🔒 My Identity
- Archetype: teamwork_preview_explorer
- Roles: explorer
- Working directory: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m4_ctrl_2
- Original parent: 403313ad-9e49-4fa6-96a7-fe91e61771bb
- Milestone: M4 - Tests & Code Quality (Controllers & Test Smells)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Design isolated integration tests for REST controllers and fix test smells (TS-01 through TS-05)
- All proposed code changes must be presented as exact designs/diffs/proposals in handoff.md, no direct code editing in project source

## Current Parent
- Conversation ID: 403313ad-9e49-4fa6-96a7-fe91e61771bb
- Updated: 2026-09-24T01:39:50Z

## Investigation State
- **Explored paths**:
  - `reporte_codigo.md` (TS-01, TS-02, TS-03, TS-04, TS-05)
  - `pom.xml`, dependency tree (`mvnw dependency:tree`)
  - `PracticasApplicationTests.java`, `SubjectControllerTest.java`, `AuthControllerTest.java`
  - `SubjectController.java`, `AuthController.java`, `UserController.java`
  - `SubjectServiceImpl.java`, `AuthServiceImpl.java`, `UserServiceImpl.java`
  - `SecurityConfig.java`, `GlobalExceptionHandler.java`, `ApiErrorResponse.java`
  - `DataSeeder.java`
- **Key findings**:
  - `pom.xml` lacks `h2` and `spring-security-test` dependencies.
  - `src/test/resources/` directory is missing; tests default to production MySQL properties.
  - TS-01: `PracticasApplicationTests.java` is empty; designed full context load verification asserting non-null core beans.
  - TS-02: `SubjectControllerTest.java` uses destructive `deleteAll()`; designed `@Transactional` rollback isolation.
  - TS-03: Security filters disabled (`addFilters = false`); designed RBAC tests using `@WithMockUser` and unauthenticated calls.
  - TS-04: Concurrency test used flaky `CountDownLatch` and 5s/10s timers; replaced with deterministic transactional tests.
  - TS-05: Plaintext Spanish substring assertions replaced with strict JSONPath assertions on `ApiErrorResponse` (`$.status`, `$.error`, `$.message`, `$.path`).
  - `UserControllerTest.java` was missing entirely; designed full test suite covering RBAC, CRUD, profile, and error handling.
  - `AuthControllerTest.java` lacked login and reset-password tests; designed full test suite with anti-enumeration verification.
- **Unexplored areas**: None for REST controller testing scope.

## Key Decisions Made
- Recommending `@Profile("!test")` on `DataSeeder.java` to prevent test contamination.
- Full code implementations and diffs delivered in `handoff.md`.

## Artifact Index
- DISPATCH.md — Recorded dispatch instructions
- BRIEFING.md — Working memory index
- progress.md — Liveness heartbeat
- handoff.md — Final investigation and test design report
