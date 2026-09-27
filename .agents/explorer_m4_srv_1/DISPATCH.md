## 2026-09-24T01:39:50Z

You are explorer_m4_srv_1, a teamwork_preview_explorer.
Your working directory is:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m4_srv_1

CRITICAL MANDATORY INSTRUCTION:
You MUST read ORIGINAL_REQUEST.md before starting work:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/ORIGINAL_REQUEST.md

Reference files:
- PROJECT.md: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/PROJECT.md
- Diagnostic Report: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/reporte_codigo.md (focus on Phase 4, TS-01 to TS-06)

Objective:
Investigate and design unit test suites for all backend domain services for Phase 4:
1. `AuthService`: login success, invalid password (BadCredentialsException), disabled account (DisabledException), password reset request & token confirmation.
2. `UserService`: user creation with password hashing, email duplication (ResourceConflictException), role assignment, user activation/deactivation.
3. `PortfolioService`: upload evidence, file type validation, download document authorization (IDOR protection), document deletion.
4. `LessonPlanService`: plan creation with/without file, ownership validation, delete plan.
5. `SubjectService` and `PracticeAssignmentService`: subject registration, student assignment creation, professor query filtering.
6. `JwtUtil`: token generation, claims extraction, expiration, and invalid/tampered signature rejection.
Ensure pure Mockito unit tests without loading heavy Spring context, running fast (< 1s per suite).

Deliverable:
Write report to:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m4_srv_1/handoff.md
And notify parent orchestrator via send_message.
