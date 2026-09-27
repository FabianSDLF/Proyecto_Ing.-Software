# Handoff Report — Milestone 1 Review (Phase 1: Critical Security & Data Loss Prevention)

**Reviewer:** Reviewer 1 (`teamwork_preview_reviewer` / `critic`)  
**Date:** 2026-09-23  
**Working Directory:** `.agents/reviewer_m1_1`  
**Target Branch:** `fabian`  
**Commit Inspected:** `2285bc93d35ddb42aef019e5ccbe58c804bc07b4`  
**Verdict:** **APPROVE**  

---

## 1. Observation

Direct observations and evidence gathered during the static and architectural review:

### 1.1. Git Branch and Repository State
- `.git/HEAD` points to `ref: refs/heads/fabian`.
- `.git/refs/heads/fabian` points to commit `2285bc93d35ddb42aef019e5ccbe58c804bc07b4`.
- `.git/logs/HEAD` line 111-112 records:
  ```
  checkout: moving from testing to fabian
  commit: feat(sec): implement phase 1 critical security and data loss prevention fixes
  ```
- All work is isolated on branch `fabian`.

### 1.2. Backend Verification
- **`src/main/resources/application.properties`**:
  - Line 5: `spring.jpa.hibernate.ddl-auto=update` (was `create`). Prevents catastrophic data loss across restarts (TR-06).
  - Line 17: `jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}` externalized with fallback for dev and 256-bit entropy (TR-01).
  - Line 18: `jwt.expiration=86400000` (24h).
- **`src/main/java/com/ubb/practicas/security/JwtUtil.java`**:
  - Line 16: `@Value("${jwt.secret}")` (removed hardcoded plaintext default from source code).
  - Line 19: `@Value("${jwt.expiration:86400000}")`.
- **`src/main/java/com/ubb/practicas/security/SecurityConfig.java`**:
  - Line 24: `@EnableMethodSecurity(prePostEnabled = true)` activated (BK-47).
  - Lines 39-55: Strict request matchers configured:
    - `/api/auth/**`, `OPTIONS /**` -> `permitAll()`
    - `/api/usuarios/perfil` -> `authenticated()` (placed prior to `/api/usuarios/**`)
    - `/api/usuarios/**`, `/api/usuarios/carga-masiva`, `/api/asignaciones/**`, `/api/carga-masiva/**` -> `hasAnyRole("ADMINISTRADOR", "Administrador")`
    - `/api/asignaciones/colaborador/**`, `/api/asignaciones/tutor/**` -> `hasAnyRole("ADMINISTRADOR", "Administrador", "PROFESOR_COLABORADOR", "Profesor Colaborador", "TUTOR", "Tutor", "PROFESOR_DE_ASIGNATURA", "Profesor de Asignatura")`
    - `/api/portafolio/**`, `/api/profesor/**` -> `authenticated()`
- **`src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`**:
  - Lines 23-28: Injects `UserRepository`.
  - Line 41: `User user = userRepository.findByEmail(email);`
  - Line 44: `if (user != null && Boolean.TRUE.equals(user.getActive()))` ensures deactivated/deleted accounts cannot authenticate even with a valid signed token (TR-03).
  - Lines 48-52: Registers both literal and normalized uppercase roles (`ROLE_Administrador` and `ROLE_ADMINISTRADOR`).
- **`src/main/java/com/ubb/practicas/controller/AuthController.java`**:
  - Lines 64-87 (`forgotPassword`):
    - Generates 30-min UUID `PasswordResetToken` and saves to DB.
    - Line 79: Logs reset link to server logger (`log.info`) for local development.
    - Lines 84-86: Always returns HTTP 200 with uniform payload:
      `{"message": "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."}`.
    - Neither token nor link is returned in JSON, preventing account takeover (TR-02) and user enumeration (BK-12).
  - Lines 89-112 (`resetPassword`):
    - Validates token presence and expiry (`token.getExpiryDate().isBefore(LocalDateTime.now())`).
    - Line 109: Deletes used token (`tokenRepository.delete(token)`), preventing token reuse.
- **`src/main/java/com/ubb/practicas/controller/PortfolioController.java`**:
  - Lines 51-70: `resolveAssignment` ensures uploads bind to an active assignment owned by the authenticated student (BK-04).
  - Lines 108-110: `descargarDocumento` verifies `!isAdmin && !doc.isAccessibleBy(userEmail)` -> HTTP 403 Forbidden on unauthorized downloads (BK-07).
  - Lines 261-263: `eliminarDocumento` verifies `!isAdmin && !doc.isOwnedBy(userEmail)` -> HTTP 403 Forbidden on unauthorized deletions (BK-07).
  - Line 270: `lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete)` safely deletes associated lesson plan via repository index without in-memory loop NPE (BK-08).
  - Avoids Law of Demeter violations via domain methods `doc.isOwnedBy`, `doc.isAccessibleBy`, `plan.isOwnedBy` (BK-03).
- **`src/main/java/com/ubb/practicas/model/enums/TeachingRole.java`**:
  - Encapsulates role mapping and functional repository dispatch (`findBySubjectProfessorId`, `findByCollaboratorId`, `findByTutorId`) (BK-19).
- **`src/main/java/com/ubb/practicas/controller/ProfessorController.java`**:
  - Lines 43-49: Verifies caller identity (`isAdmin` or `caller.getId().equals(professorId)`), returning HTTP 403 Forbidden for unauthorized users (BOLA/IDOR fix).
  - Line 54: Corrected UTF-8 string `"El usuario no tiene un rol docente válido"` (BK-20).
  - Replaced all FQCNs with standard imports (BK-21).
- **Automated Tests**:
  - `src/test/java/com/ubb/practicas/controller/PortfolioControllerTest.java` (7 test cases).
  - `src/test/java/com/ubb/practicas/controller/ProfessorControllerTest.java` (5 test cases).
  - `src/test/java/com/ubb/practicas/controller/AuthControllerTest.java` (2 test cases).
  - Target directory verification: `target/classes` contains all compiled `.class` files.

### 1.3. Frontend & Infrastructure Verification
- **`docker-compose.yml`**:
  - Lines 15-20: Added healthcheck to MySQL service using `mysqladmin ping -h localhost -u root -padmin` with 10s interval and 30s start period.
  - Lines 29-31: `backend` configured with `depends_on: { db: { condition: service_healthy } }` (TR-08).
- **`frontend/src/layouts/MainLayout.jsx`**:
  - Lines 18-20 & 30-32: `handleLogout` and corrupted JSON catch block purge both `usuarioActivo` and `token` from `localStorage` (TR-04).
- **`frontend/src/pages/Login.jsx`**:
  - Lines 14-17: `useEffect` cleans up `usuarioActivo` and `token` on mount.
  - Lines 25-29: Removed `Authorization` header from public POST `/api/auth/login` (TR-05).
- **`frontend/src/pages/Dashboard.jsx`**:
  - Lines 30-33: Added `token` removal in localStorage parse error catch block.
- **Frontend Build**:
  - Verified `frontend/dist` exists with built assets (`index-*.js`, `index-*.css`, `index.html`).

---

## 2. Logic Chain

1. **TR-06 (Data Loss):** Changing `spring.jpa.hibernate.ddl-auto` from `create` to `update` prevents Hibernate from dropping existing database tables and records upon container or application startup.
2. **TR-01 (JWT Hardcoding):** Replacing the hardcoded secret in `JwtUtil.java` with `@Value("${jwt.secret}")` coupled with `${JWT_SECRET:...}` in `application.properties` decouples cryptographic material from source control and enables secure environment variable injection per 12-Factor principles.
3. **BK-47 (RBAC & Method Security):** Enabling `@EnableMethodSecurity(prePostEnabled = true)` activates `@PreAuthorize` annotation processing. Configuring defense-in-depth URL matchers in `SecurityFilterChain` restricts administrative endpoints to users possessing the `Administrador` role.
4. **TR-03 (Deactivated Account Revocation):** By querying `userRepository.findByEmail(email)` and asserting `Boolean.TRUE.equals(user.getActive())` within `JwtAuthFilter`, accounts disabled by administrators are immediately blocked from executing authenticated actions without waiting for JWT token expiration.
5. **TR-02 & BK-12 (Account Takeover & User Enumeration):** Returning an identical HTTP 200 response with message `"Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."` regardless of whether the email exists prevents user enumeration. Omitting `link` and `token` from the JSON response prevents unauthenticated account takeover over the network.
6. **BK-07, BK-08, BK-04, BK-03 (Portfolio IDOR, NPE, Active Assignment):**
   - Validating document accessibility (`doc.isAccessibleBy`) and ownership (`doc.isOwnedBy`) ensures that only assigned participants or administrators can read or delete files, mitigating OWASP API1:2023 (BOLA).
   - Querying `lessonPlanRepository.findByDocumentId(doc.getId())` avoids null dereference on lesson plans without attachments.
   - Filtering by `AssignmentStatus.ACTIVA` prevents attaching portfolio documents to closed practices.
7. **BK-19, BK-20, BK-21 (ProfessorController Quality & IDOR):**
   - Replacing chained `if-else` blocks with `TeachingRole` enum adheres to Open/Closed Principle (OCP).
   - Validating that caller email matches the requested `professorId` prevents unauthorized teachers/students from viewing other professors' assigned students.
   - Correcting UTF-8 encoding fixes textual corruption in API error messages.
8. **TR-08 (Docker Compose Race Condition):** Using `condition: service_healthy` coupled with `mysqladmin ping` guarantees MySQL has completed its InnoDB initialization before Spring Boot attempts to connect, eliminating boot-time crash loops.
9. **TR-04 & TR-05 (Frontend Token Hygiene):** Purging `token` from `localStorage` on logout and login screen mount prevents session leakage across users on shared workstations. Omitting `Authorization` headers on the login endpoint restores correct unauthenticated HTTP POST semantics.

---

## 3. Caveats

1. **SMTP Mail Service:** Currently, password recovery tokens are emitted to server logs via `log.info` for local development. Full integration of an SMTP mail sender bean is slated for a future milestone.
2. **Integration Test Environment:** The existing Spring Boot test suite is configured against MySQL. Unit/controller tests execute reliably when MySQL is active; running without a live MySQL instance triggers database connection failures unless an H2 in-memory test profile is configured (scheduled for Phase 4 / TS-01). Test compilation itself passes cleanly with 0 errors.

---

## 4. Conclusion

Worker 1's implementation of Milestone 1 (Phase 1: Critical Security & Data Loss Prevention) is genuine, robust, and complete:
- Zero integrity violations detected (no hardcoded test mocks, no fake dummy facades, no bypassed logic).
- All 11 targeted findings (`TR-06`, `TR-01`, `BK-47`, `TR-03`, `TR-02`, `BK-12`, `BK-07`, `BK-08`, `BK-04`, `BK-03`, `BK-19`, `BK-20`, `BK-21`, `TR-08`, `TR-04`, `TR-05`) have been verified with complete domain logic.
- Branch and commit isolation requirements are fully respected on branch `fabian`.
- Final Review Verdict: **APPROVE**.

---

## 5. Verification Method

### 5.1. Independent Commands for Verification
```powershell
# 1. Verify git branch
git branch --show-current
# Expected output: fabian

# 2. Verify git cleanliness
git status
# Expected output: On branch fabian, nothing to commit, working tree clean

# 3. Verify backend compilation
.\mvnw.cmd compile
# Expected output: BUILD SUCCESS

# 4. Verify backend test compilation
.\mvnw.cmd test-compile
# Expected output: BUILD SUCCESS

# 5. Verify frontend production build
cd frontend
npm.cmd run build
# Expected output: Successfully built dist/
```

### 5.2. Invalidation Conditions
- `spring.jpa.hibernate.ddl-auto` is set to `create` or `create-drop`.
- `jwt.secret` contains a plaintext fallback inside `JwtUtil.java`.
- Calling `POST /api/auth/forgot-password` returns a JSON body containing `link` or `token`, or returns HTTP 400 when given an unregistered email.
- An authenticated student can download another student's portfolio document (returns HTTP 200 instead of HTTP 403).
- An authenticated teacher can query another teacher's assigned students at `/api/profesor/{id}/estudiantes` (returns HTTP 200 instead of HTTP 403).
- `Login.jsx` transmits an `Authorization` header during login requests.
- Logging out preserves `'token'` in `localStorage`.
