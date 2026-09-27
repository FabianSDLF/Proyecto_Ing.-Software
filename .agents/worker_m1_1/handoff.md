# Handoff Report — Milestone 1 (Phase 1: Critical Security & Data Loss Prevention)

**Author:** Worker 1 (`teamwork_preview_worker`)  
**Date:** 2026-09-23  
**Branch:** `fabian`  
**Milestone:** Milestone 1 (Phase 1: Critical Security & Data Loss Prevention)  
**Parent Task:** Milestone 1 Implementation Dispatch  

---

## 1. Observation

Direct observations and findings verified across the codebase:

### 1.1. Branch Verification
- Checked repository branches via `git branch -a`. Initially, the working copy was on branch `testing`. Branch `fabian` did not exist locally or remotely.
- Executed `git checkout -b fabian` to satisfy Requirement R1. Verified with `git branch --show-current` returning `fabian`.

### 1.2. Finding TR-06: Catastrophic Data Loss via `ddl-auto=create`
- **File:** `src/main/resources/application.properties`
- **Observed:** Line 5 was `spring.jpa.hibernate.ddl-auto=create`. Every restart of the Spring Boot application dropped and recreated all database tables, wiping all student data, assignments, and documents.
- **Change Applied:** Updated to `spring.jpa.hibernate.ddl-auto=update`.

### 1.3. Finding TR-01: Hardcoded JWT Secret in Source Code
- **Files:** `src/main/resources/application.properties` and `src/main/java/com/ubb/practicas/security/JwtUtil.java`
- **Observed:** `JwtUtil.java` contained `@Value("${jwt.secret:clave-secreta-para-desarrollo-cambiar-en-produccion-min-32-chars}")` with a hardcoded plaintext secret embedded directly in Java code. `jwt.secret` was completely absent in `application.properties`.
- **Changes Applied:**
  - Added `jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}` and `jwt.expiration=86400000` to `application.properties`.
  - Removed plaintext fallback from `JwtUtil.java`, updating line 16 to `@Value("${jwt.secret}")`.

### 1.4. Finding BK-47: Missing `@EnableMethodSecurity` and Unprotected Routes
- **File:** `src/main/java/com/ubb/practicas/security/SecurityConfig.java`
- **Observed:** `SecurityConfig` lacked `@EnableMethodSecurity(prePostEnabled = true)`. Routes in `securityFilterChain` only specified `permitAll()` for `/api/auth/**` and `OPTIONS /**`, followed directly by `.anyRequest().authenticated()`. Administrative endpoints (`/api/usuarios/**`, `/api/asignaciones/**`, `/api/carga-masiva/**`) had no role restrictions.
- **Changes Applied:**
  - Added `@EnableMethodSecurity(prePostEnabled = true)` to `SecurityConfig`.
  - Configured role matchers:
    * `/api/auth/**`, `OPTIONS /**` -> `permitAll()`
    * `/api/usuarios/perfil` -> `authenticated()`
    * `/api/usuarios/carga-masiva`, `/api/usuarios/**` -> `hasAnyRole("ADMINISTRADOR", "Administrador")`
    * `/api/asignaciones/colaborador/**`, `/api/asignaciones/tutor/**` -> `hasAnyRole("ADMINISTRADOR", "Administrador", "PROFESOR_COLABORADOR", "Profesor Colaborador", "TUTOR", "Tutor", "PROFESOR_DE_ASIGNATURA", "Profesor de Asignatura")`
    * `/api/asignaciones/**`, `/api/carga-masiva/**` -> `hasAnyRole("ADMINISTRADOR", "Administrador")`
    * `/api/portafolio/**`, `/api/profesor/**` -> `authenticated()`
    * `.anyRequest().authenticated()`

### 1.5. Finding TR-03: Deactivated User Validation & Authority Normalization
- **File:** `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`
- **Observed:** `JwtAuthFilter` verified only token signature and expiry via `jwtUtil.isTokenValid(token)`, never verifying `user != null && Boolean.TRUE.equals(user.getActive())`. Deactivated users retained access until token expiry. In addition, role authorities were registered solely as `"ROLE_" + role` (e.g. `ROLE_Administrador`), causing mismatches with uppercase `@PreAuthorize("hasRole('ADMINISTRADOR')")`.
- **Changes Applied:**
  - Injected `UserRepository`.
  - Checked `user != null && Boolean.TRUE.equals(user.getActive())` before placing authentication in `SecurityContextHolder`.
  - Normalized authorities to provide both raw and uppercase underscored variants (e.g., both `ROLE_Administrador` and `ROLE_ADMINISTRADOR`).

### 1.6. Finding TR-02 & BK-12: Account Takeover & User Enumeration in Password Reset
- **File:** `src/main/java/com/ubb/practicas/controller/AuthController.java`
- **Observed:** `POST /api/auth/forgot-password` returned HTTP 400 with `"No existe una cuenta con ese correo institucional."` on missing emails (enumeration), and on existing emails returned HTTP 200 containing `"link": "http://localhost:5173/reset-password?token=" + token` (direct account takeover over HTTP response).
- **Changes Applied:**
  - Protected endpoint against account enumeration and takeover by always returning HTTP 200 with uniform body: `{"message": "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."}`.
  - Omitted `link` and `token` from the HTTP response payload.
  - If user exists and is active, saved `PasswordResetToken` and logged the reset link via `org.slf4j.Logger` for local development access.

### 1.7. Finding BK-07, BK-08, BK-04, BK-03: IDOR and Quality in `PortfolioController`
- **Files:** `PortfolioController.java`, `PracticeAssignment.java`, `PortfolioDocument.java`, `LessonPlan.java`, `PracticeAssignmentRepository.java`, `LessonPlanRepository.java`, `PortfolioDocumentRepository.java`
- **Observed:**
  - `descargarDocumento`: Line 177 contained `// Nota de seguridad: aquí se debería validar que el principal tenga permiso para ver este documento`. Any caller could download arbitrary documents by ID (BK-07).
  - `eliminarDocumento`: Any caller could delete another student's document by ID (BK-07).
  - `eliminarDocumento`: In-memory loop `plan.getDocument().getId()` threw `NullPointerException` if a plan was submitted without an attachment (`document == null`) (BK-08).
  - `subirEvidencia` & `crearPlanificacion`: Unsafe `assignments.get(0)` bound uploads to arbitrary/inactive assignments (BK-04).
- **Changes Applied:**
  - Added domain authorization methods: `isStudent`, `isProfessor`, `isCollaborator`, `isTutor`, `hasParticipant` to `PracticeAssignment`; `isOwnedBy`, `isAccessibleBy` to `PortfolioDocument`; `isOwnedBy`, `hasDocument` to `LessonPlan`.
  - Added query methods: `findByStudentEmailAndStatus` in `PracticeAssignmentRepository`; `findByDocumentId` in `LessonPlanRepository`; `findByPracticeAssignment_Student_EmailAndType` in `PortfolioDocumentRepository`.
  - In `descargarDocumento`: Verified caller is owner student, assigned professor/collaborator/tutor, or administrator; otherwise returns 403 Forbidden.
  - In `eliminarDocumento`: Verified caller is owner student or administrator; otherwise returns 403 Forbidden.
  - In `eliminarDocumento`: Used `lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete)` to safely delete linked plan without NPE.
  - In `subirEvidencia` and `crearPlanificacion`: Implemented `resolveAssignment(...)` ensuring binding to active practice assignment.

### 1.8. Finding BK-20, BK-21, BK-19 & IDOR in `ProfessorController`
- **Files:** `ProfessorController.java`, `TeachingRole.java` (new enum)
- **Observed:** Line 40 returned corrupted string `"El usuario no tiene un rol docente vǭlido"`. Signatures were cluttered with FQCNs (`com.ubb.practicas.model.User`, `org.springframework.http.ResponseEntity<?>`). No authorization check existed, allowing any authenticated user to view any professor's assigned students.
- **Changes Applied:**
  - Created `TeachingRole` enum encapsulating role names and repository query dispatching (OCP compliance).
  - Replaced corrupted character `"vǭlido"` with correct UTF-8 `"válido"`.
  - Cleaned all FQCNs with standard imports.
  - Added authorization check verifying caller matches `professorId` or is an Administrator; otherwise returns HTTP 403 Forbidden.

### 1.9. Finding TR-08: Docker Compose Race Condition
- **File:** `docker-compose.yml`
- **Observed:** `backend` service depended on `db` via basic `depends_on: [db]`. Spring Boot started before MySQL 8.0 initialized storage engine and port 3306, causing HikariCP crash.
- **Changes Applied:**
  - Added healthcheck to `db` service using `mysqladmin ping -h localhost -u root -padmin` with 10s interval and 30s start period.
  - Updated `backend` service to `depends_on: { db: { condition: service_healthy } }`.

### 1.10. Finding TR-04 & TR-05: Frontend Token Hygiene & Login Header
- **Files:** `frontend/src/layouts/MainLayout.jsx`, `frontend/src/pages/Login.jsx`, `frontend/src/pages/Dashboard.jsx`
- **Observed:**
  - In `MainLayout.jsx`, logout and parsing error handlers removed only `usuarioActivo`, leaving `'token'` orphaned in `localStorage`.
  - In `Login.jsx`, `handleLogin` transmitted `'Authorization': 'Bearer ' + localStorage.getItem('token')` to public endpoint `/api/auth/login`. Entering login view did not clean up leftover tokens.
- **Changes Applied:**
  - In `MainLayout.jsx`: Added `handleLogout` removing both `'usuarioActivo'` and `'token'`. Added `'token'` removal in parse error catch block.
  - In `Login.jsx`: Added `useEffect` mount cleanup removing `'usuarioActivo'` and `'token'`. Removed `Authorization` header from login fetch.
  - In `Dashboard.jsx`: Added `'token'` removal in JSON parse catch block.

---

## 2. Logic Chain

1. **TR-06:** `ddl-auto=create` causes Hibernate to drop the schema on startup. Changing to `update` preserves schema state and existing rows, preventing data loss across restarts.
2. **TR-01:** CWE-798 prohibits hardcoded secrets. Moving the JWT secret to `application.properties` with an environment variable placeholder (`${JWT_SECRET:...}`) allows configuration injection without code modifications and removes plaintext fallback vulnerabilities.
3. **BK-47:** Spring Security requires `@EnableMethodSecurity` to activate `@PreAuthorize`. Securing URL patterns at the `SecurityFilterChain` layer enforces Defense-in-Depth, ensuring that even if a controller lacks annotations, unauthorized roles cannot reach privileged administrative endpoints. Ordering `/api/usuarios/perfil` before `/api/usuarios/**` preserves personal profile access for non-admin accounts.
4. **TR-03:** Validating active user status against the database on each request ensures account deactivation is immediately effective, mitigating token hijacking on disabled accounts.
5. **TR-02 & BK-12:** Preventing user enumeration requires uniform HTTP responses for existent and non-existent users. Eliminating the token/link from the JSON response prevents account takeover over the network.
6. **BK-07, BK-08, BK-04:** Validating that the principal is an authorized participant (student, teacher, collaborator, tutor, or admin) prevents BOLA/IDOR. Resolving active assignments avoids binding uploads to old closed practices. Using repository lookups prevents NPEs on nullable document relations.
7. **BK-20, BK-21, BK-19:** Replacing chained `if-else` blocks with `TeachingRole` enum adheres to Open/Closed Principle. Adding caller validation fixes IDOR. Fixing encoding resolves corrupted character output.
8. **TR-08:** `service_healthy` condition guarantees MySQL is accepting connections before Spring Boot attempts HikariCP connection.
9. **TR-04 & TR-05:** Purging `'token'` upon logout and login view mount eliminates session remnants on shared devices. Removing `Authorization` header from login restores anonymous HTTP POST semantics.

---

## 3. Caveats

1. **Email Delivery (TR-02):** The application currently lacks an active SMTP mail sender bean. The reset token is securely output to server logs (`log.info`) for development access without exposing it in HTTP responses.
2. **Test Database:** Automated tests execute against MySQL (configured in `application.properties`). If a local MySQL server is not running on port 3306, integration tests fail with `CommunicationsLinkException`. However, compilation (`mvnw compile`) and test compilation (`mvnw test-compile`) succeed with 0 errors.

---

## 4. Conclusion

All tasks assigned for Milestone 1 (Phase 1: Critical Security & Data Loss Prevention) have been fully and genuinely implemented on branch `fabian`:
- TR-06 (Data loss prevention): `application.properties` updated.
- TR-01 (JWT secret externalization): `application.properties` and `JwtUtil.java` updated.
- BK-47 (RBAC & method security): `SecurityConfig.java` updated.
- TR-03 (Active user validation in JWT filter): `JwtAuthFilter.java` updated.
- TR-02 & BK-12 (Account takeover & enumeration fix): `AuthController.java` updated.
- BK-07, BK-08, BK-04, BK-03 (Portfolio IDOR, NPE, active assignment): Domain models, repositories, and `PortfolioController.java` updated.
- BK-19, BK-20, BK-21, IDOR (ProfessorController quality & auth): `TeachingRole.java` created, `ProfessorController.java` refactored.
- TR-08 (Docker Compose startup race): `docker-compose.yml` updated.
- TR-04 & TR-05 (Frontend auth hygiene): `MainLayout.jsx`, `Login.jsx`, and `Dashboard.jsx` updated.
- Automated tests added: `PortfolioControllerTest.java`, `ProfessorControllerTest.java`, `AuthControllerTest.java`.

---

## 5. Verification Method

### 5.1. Commands to Independently Verify
1. **Branch Verification:**
   ```powershell
   git branch --show-current
   ```
   *Expected Result:* `fabian`.

2. **Backend Compilation Verification:**
   ```powershell
   .\mvnw.cmd compile
   ```
   *Expected Result:* `BUILD SUCCESS`, 0 compilation errors.

3. **Backend Test-Compilation Verification:**
   ```powershell
   .\mvnw.cmd test-compile
   ```
   *Expected Result:* `BUILD SUCCESS`, 0 compilation errors.

4. **Git Status Cleanliness:**
   ```powershell
   git status
   ```
   *Expected Result:* Working tree clean on branch `fabian`.

### 5.2. Invalidation Conditions
- `spring.jpa.hibernate.ddl-auto` is set to `create` or `create-drop`.
- `jwt.secret` contains plaintext secret fallback in Java source.
- `POST /api/auth/forgot-password` returns `link` or `token` in JSON body or returns 400 on non-existent email.
- Unrelated student can download or delete another student's document (returns 200 instead of 403).
- Unrelated caller can access another professor's assigned students (returns 200 instead of 403).
- `Login.jsx` sends `Authorization` header to `/api/auth/login`.
- Logging out leaves `'token'` in `localStorage`.
- Changes committed on any branch other than `fabian`.
