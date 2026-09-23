# BRIEFING — 2026-09-23T19:06:00Z

## Mission
Investigate and produce line-by-line remediation evidence and concrete code change proposals for Phase 1 security findings (TR-06, BK-47, TR-01, TR-02/BK-12, TR-03).

## 🔒 My Identity
- Archetype: teamwork_preview_explorer
- Roles: investigation, synthesis
- Working directory: c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_sec_1
- Original parent: c3f760d9-58dc-4c34-b179-bc154eee62cf
- Milestone: Milestone 1 (Phase 1: Critical Security & Data Loss Prevention)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Work only within assigned scope: TR-06, BK-47, TR-01, TR-02/BK-12, TR-03
- Write only to my assigned folder: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_sec_1`

## Current Parent
- Conversation ID: c3f760d9-58dc-4c34-b179-bc154eee62cf
- Updated: 2026-09-23T19:06:00Z

## Investigation State
- **Explored paths**:
  - `src/main/resources/application.properties`
  - `src/main/java/com/ubb/practicas/security/SecurityConfig.java`
  - `src/main/java/com/ubb/practicas/security/JwtUtil.java`
  - `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`
  - `src/main/java/com/ubb/practicas/controller/AuthController.java`
  - `src/main/java/com/ubb/practicas/controller/UserController.java`
  - `src/main/java/com/ubb/practicas/controller/PracticeAssignmentController.java`
  - `frontend/src/pages/ForgotPassword.jsx`
  - `frontend/src/pages/MisEstudiantes.jsx`
  - `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java`
- **Key findings**:
  - TR-06: Line 5 in `application.properties` drops all tables on boot; change to `update`.
  - BK-47: `SecurityConfig.java` has no method security and no role authorization. Must protect `/api/usuarios/**`, `/api/asignaciones/**`, `/api/carga-masiva/**`, while permitting `/api/usuarios/perfil` for any authenticated user and allowing teachers read access on their assignment routes.
  - TR-01: Plaintext secret in `JwtUtil.java`; move to `application.properties` with `${JWT_SECRET:...}` default and inject cleanly.
  - TR-02 & BK-12: `AuthController.java` exposes token and reset link in JSON response, and enumerates users on 400. Fix by returning uniform 200 OK without token/link, logging token server-side for dev.
  - TR-03: `JwtAuthFilter.java` fails to check DB user status. Inject `UserRepository`, verify `user.getActive() == true`, and normalize granted authorities (`ROLE_Administrador` and `ROLE_ADMINISTRADOR`).
- **Unexplored areas**: None within the Phase 1 security scope.

## Key Decisions Made
- Proposed exact before/after code blocks and unified diffs for the Worker to apply.
- Ensured `/api/usuarios/perfil` route ordering is preserved ahead of `/api/usuarios/**` to avoid locking students out of profile management.

## Artifact Index
- `DISPATCH.md` — Incoming dispatch log
- `progress.md` — Agent heartbeat and progress tracking
- `BRIEFING.md` — Persistent situational memory
- `handoff.md` — Final 5-component handoff report
