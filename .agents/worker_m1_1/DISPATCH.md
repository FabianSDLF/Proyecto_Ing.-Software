## 2026-09-23T19:07:15Z
You are Worker 1 for Milestone 1 (Phase 1: Critical Security & Data Loss Prevention).

## 🔒 Your Identity & Working Directory
- Identity: teamwork_preview_worker
- Working directory: c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\worker_m1_1
- Parent Conversation ID: c3f760d9-58dc-4c34-b179-bc154eee62cf

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Mandatory Files to Read
- ORIGINAL_REQUEST.md: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\ORIGINAL_REQUEST.md`
- Explorer 1 Handoff (Backend Security): `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_sec_1\handoff.md`
- Explorer 2 Handoff (Controllers & IDOR): `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_ctrl_2\handoff.md`
- Explorer 3 Handoff (Infra & Frontend Auth): `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_infra_fe_3\handoff.md`

## Concrete Tasks to Implement

### Step 1: Branch Requirement (R1)
Before modifying any files, verify and switch to branch `fabian`:
In `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas`:
Run `git checkout -b fabian` (or `git checkout fabian` if it exists). Verify current branch is `fabian`.

### Step 2: Implement Backend Security & Config (from Explorer 1 handoff)
1. `src/main/resources/application.properties`:
   - Change `spring.jpa.hibernate.ddl-auto=create` to `update` (TR-06).
   - Add `jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}` and `jwt.expiration=86400000` (TR-01).
2. `src/main/java/com/ubb/practicas/security/SecurityConfig.java`:
   - Add `@EnableMethodSecurity(prePostEnabled = true)` (BK-47).
   - Configure role-based access rules:
     * `/api/auth/**` and `OPTIONS /**` -> `permitAll()`
     * `/api/usuarios/perfil` -> `authenticated()`
     * `/api/usuarios/**`, `/api/usuarios/carga-masiva` -> `hasAnyRole("ADMINISTRADOR", "Administrador")`
     * `/api/asignaciones/colaborador/**`, `/api/asignaciones/tutor/**` -> `hasAnyRole("ADMINISTRADOR", "Administrador", "PROFESOR_COLABORADOR", "Profesor Colaborador", "TUTOR", "Tutor", "PROFESOR_DE_ASIGNATURA", "Profesor de Asignatura")`
     * `/api/asignaciones/**` (admin mutations) -> `hasAnyRole("ADMINISTRADOR", "Administrador")`
     * `/api/portafolio/**`, `/api/profesor/**`, and others -> `authenticated()`
     * `.anyRequest().authenticated()`
3. `src/main/java/com/ubb/practicas/security/JwtUtil.java`:
   - Remove hardcoded default secret fallback from `@Value("${jwt.secret}")` (TR-01).
4. `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`:
   - Inject `UserRepository` to verify `user != null && Boolean.TRUE.equals(user.getActive())` before setting authentication (TR-03).
   - Normalize authorities to support both title case and uppercase (e.g., `ROLE_Administrador` and `ROLE_ADMINISTRADOR`).
5. `src/main/java/com/ubb/practicas/controller/AuthController.java`:
   - In `forgotPassword`: do NOT return `link` or `token` in the HTTP response JSON (TR-02 & BK-12).
   - Return a uniform generic message: `"Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."` (HTTP 200 OK) regardless of user existence.
   - If user exists, save `PasswordResetToken` and log the reset link to server log for development access.

### Step 3: Implement Controllers IDOR & Quality Fixes (from Explorer 2 handoff)
1. `src/main/java/com/ubb/practicas/controller/PortfolioController.java`:
   - `descargarDocumento`: check that authenticated caller (`principal.getName()`) is the student owner, or assigned professor/collaborator/tutor, or an administrator. If unauthorized, return HTTP 403 Forbidden (BK-07).
   - `eliminarDocumento`: check that authenticated caller is the student owner. If unauthorized, return HTTP 403 (BK-07).
   - `eliminarDocumento`: guard against `NullPointerException` on `plan.getDocument()` before checking ID (BK-08).
   - `subirEvidencia` and `crearPlanificacion`: validate active assignment properly, avoiding blind unsafe assumption (BK-04).
2. `src/main/java/com/ubb/practicas/controller/ProfessorController.java`:
   - Fix corrupted character: change `"vǭlido"` to `"válido"` (BK-20).
   - Add proper imports and clean up FQCN (BK-21).
   - Ensure caller is authorized: only the professor themselves or an administrator can view the assigned students.

### Step 4: Implement Infrastructure & Frontend Auth Hygiene (from Explorer 3 handoff)
1. `docker-compose.yml`:
   - Add `healthcheck` to `db` service (`mysqladmin ping -h localhost -u root -padmin`).
   - Add `depends_on: { db: { condition: service_healthy } }` to `backend` service (TR-08).
2. `frontend/src/layouts/MainLayout.jsx`:
   - In `handleLogout`: remove both `localStorage.removeItem('usuarioActivo')` and `localStorage.removeItem('token')` (TR-04).
   - Also remove both in JSON parsing catch block on mount.
3. `frontend/src/pages/Login.jsx`:
   - In `useEffect` on mount, remove leftover `token` and `usuarioActivo` from `localStorage` (TR-04).
   - In `handleLogin`: remove `'Authorization': 'Bearer ' + ...` header from POST `/api/auth/login` fetch request (TR-05).

### Step 5: Verification
1. Run backend build: In repository root, run `.\mvnw.cmd compile`. Ensure BUILD SUCCESS with 0 errors.
2. Run frontend build: In `frontend/`, run `npm.cmd run build`. Ensure build succeeds without fatal errors.
3. Run `npm.cmd run lint` in `frontend/` to ensure no linting regressions.

### Step 6: Git Commit
1. Verify `git status` shows all modified files on branch `fabian`.
2. Commit all changes: `git add .` and `git commit -m "feat(sec): implement phase 1 critical security and data loss prevention fixes"`.
3. Verify working tree is clean on branch `fabian`.

### Step 7: Handoff
Write your full implementation and verification report in `handoff.md` in your working directory and notify the parent via `send_message`.
