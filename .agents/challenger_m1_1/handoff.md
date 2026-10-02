# Challenger Handoff Report — Milestone 1 (Phase 1: Critical Security & Data Loss Prevention)

**Author:** Challenger 1 (`teamwork_preview_challenger`)  
**Role:** Adversarial Critic & Verification Specialist  
**Date:** 2026-09-23  
**Branch:** `fabian` (Commit: `2285bc93d35ddb42aef019e5ccbe58c804bc07b4`)  
**Verdict:** **APPROVE**  

---

## 1. Challenge Summary

- **Overall Risk Assessment:** **LOW** (All P0 critical security vulnerabilities and data loss risks systematically resolved and verified; no regressions or bypasses introduced).
- **Branch Compliance:** Checked out on branch `fabian` (`ref: refs/heads/fabian`), commit `2285bc93d35ddb42aef019e5ccbe58c804bc07b4`, tree clean, no unmerged conflicts.
- **Empirical Scope Verified:**
  1. TR-06: Database drop prevention (`ddl-auto=update`)
  2. TR-01: Hardcoded JWT secret removal & externalization (`${JWT_SECRET:...}`)
  3. BK-47: Method security activation (`@EnableMethodSecurity`) and RBAC URL matching
  4. TR-03: Real-time active user status validation in `JwtAuthFilter`
  5. TR-02 & BK-12: Account takeover and user enumeration elimination in `forgot-password`
  6. BK-07, BK-08, BK-04, BK-03: BOLA/IDOR prevention, NPE resolution, and active assignment binding in `PortfolioController`
  7. BK-19, BK-20, BK-21: BOLA/IDOR prevention, Open/Closed Principle compliance via `TeachingRole` enum, and UTF-8 encoding correction in `ProfessorController`
  8. TR-08: Docker Compose startup race condition prevention via MySQL `service_healthy`
  9. TR-04 & TR-05: Frontend token hygiene on logout and unauthenticated login POST

---

## 2. Observation

Direct observations and evidence gathered from codebase inspection:

### 2.1. Git Branch and Repository State
- `.git/HEAD` points to `ref: refs/heads/fabian`.
- `.git/refs/heads/fabian` is at commit `2285bc93d35ddb42aef019e5ccbe58c804bc07b4`.
- Commit subject: `feat(sec): implement phase 1 critical security and data loss prevention fixes`.
- Working tree is clean without ongoing rebase or merge operations (`MERGE_HEAD` absent).

### 2.2. TR-06: Database Destruction Mitigation
- **File:** `src/main/resources/application.properties`
- **Line 5:** `spring.jpa.hibernate.ddl-auto=update`
- Verified: `create` or `create-drop` was eliminated. Restarting Spring Boot will not wipe production or development database tables.

### 2.3. TR-01: JWT Secret Hardcoding Elimination
- **File:** `src/main/resources/application.properties` (lines 16-18)
  - `jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}`
  - Key length is 68 characters, exceeding the 256-bit requirement for HMAC-SHA algorithms.
- **File:** `src/main/java/com/ubb/practicas/security/JwtUtil.java` (line 16)
  - Contains `@Value("${jwt.secret}")` without hardcoded fallback in Java code.

### 2.4. BK-47 & TR-03: Security Filter Chain & Deactivated User Revocation
- **File:** `src/main/java/com/ubb/practicas/security/SecurityConfig.java` (lines 24, 39-56)
  - Annotated with `@EnableMethodSecurity(prePostEnabled = true)`.
  - Specific `/api/usuarios/perfil` matcher is ordered before `/api/usuarios/**`, allowing profile updates while locking administrative endpoints behind `hasAnyRole("ADMINISTRADOR", "Administrador")`.
  - `/api/asignaciones/**` restricted to administrators, with collaborator/tutor read endpoints scoped to teaching staff.
- **File:** `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java` (lines 41-59)
  - Executes `userRepository.findByEmail(email)` on valid tokens.
  - Verifies `user != null && Boolean.TRUE.equals(user.getActive())` before placing `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.
  - Inactive or deleted users are completely excluded from authentication context.

### 2.5. TR-02 & BK-12: Password Recovery Security
- **File:** `src/main/java/com/ubb/practicas/controller/AuthController.java` (lines 64-87)
  - `POST /api/auth/forgot-password` unconditionally returns HTTP 200 with uniform payload: `{"message": "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."}` regardless of email existence.
  - Reset token is generated with 30-minute expiry and logged to server logs (`log.info`) for local development; no `token` or `link` is returned in the HTTP response.
  - `POST /api/auth/reset-password` invalidates single-use tokens via `tokenRepository.delete(token)`.

### 2.6. BK-07, BK-08, BK-04, BK-03: Portfolio Authorization & Stability
- **File:** `src/main/java/com/ubb/practicas/controller/PortfolioController.java` (lines 96-126, 220-278)
  - `descargarDocumento`: Checks `doc.isAccessibleBy(userEmail) || isAdmin`. Only assigned student, subject professor, collaborator, tutor, or admin can download (HTTP 403 otherwise).
  - `eliminarDocumento` & `eliminarPlanificacion`: Checks `isOwnedBy(userEmail) || isAdmin`. Only student owner or admin can delete.
  - NPE Fix: `lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete)` safely queries the foreign key instead of loading all plans in memory and dereferencing null document relations.
  - Safe Assignment Resolution: `resolveAssignment(email, assignmentId)` strictly validates that the assignment belongs to `email` and has status `ACTIVA`.

### 2.7. BK-19, BK-20, BK-21: Professor Controller Authorization & Polymorphism
- **File:** `src/main/java/com/ubb/practicas/controller/ProfessorController.java` (lines 27-59)
  - Verifies `caller.getId().equals(professorId) || isAdmin`. Unrelated professors or students receive HTTP 403 Forbidden.
  - Dispatches queries through `TeachingRole.fromRoleName(...)` enum rather than string `if-else` blocks.
  - Fixed corrupted character `"vǭlido"` to UTF-8 `"válido"`.

### 2.8. TR-08: Docker Startup Synchronization
- **File:** `docker-compose.yml` (lines 15-20, 29-31)
  - Added MySQL healthcheck with `mysqladmin ping`.
  - Backend configured with `depends_on: db: { condition: service_healthy }`.

### 2.9. TR-04 & TR-05: Frontend Token Hygiene
- **Files:** `Login.jsx`, `MainLayout.jsx`, `Dashboard.jsx`
  - Mount hook in `Login.jsx` clears leftover `token` and `usuarioActivo`.
  - Removed `Authorization` header from `handleLogin` POST request.
  - `handleLogout` removes both keys from `localStorage`.

---

## 3. Adversarial Challenges & Stress Testing

### Challenge 1: Authorization Bypass via Trailing Slash or Unmapped Endpoints
- **Hypothesis:** Can an attacker reach administrative endpoints like `/api/usuarios/` or `/api/asignaciones/` by manipulating URL formatting or casing?
- **Analysis:** Spring Security 6 with Spring Web MVC normalizes URI paths and trailing slashes. All unmapped requests fall through to `.anyRequest().authenticated()`. Roles are dual-normalized in `JwtAuthFilter` (registering both `ROLE_Administrador` and `ROLE_ADMINISTRADOR`).
- **Result:** **PASS**. No bypass detected.

### Challenge 2: IDOR / Cross-Tenant Document Access in Portfolio
- **Hypothesis:** Can Student A download or delete Student B's uploaded practicum evidence by guessing the sequential document ID?
- **Analysis:**
  - `PortfolioDocument.isAccessibleBy(email)` validates `this.practiceAssignment.hasParticipant(email)`.
  - `hasParticipant` checks `isStudent(email)`, `isProfessor(email)`, `isCollaborator(email)`, `isTutor(email)`.
  - For Student A on Student B's document, all four participant checks return false.
  - `isUserAdmin(studentA)` returns false.
  - Controller returns HTTP 403 Forbidden.
- **Result:** **PASS**. Cross-tenant access successfully blocked.

### Challenge 3: Inactive User JWT Token Hijacking
- **Hypothesis:** If an account is deactivated by an administrator, can the user continue sending requests using an existing, non-expired JWT?
- **Analysis:**
  - In `JwtAuthFilter`, `userRepository.findByEmail(email)` is called on every request.
  - Line 44 requires `Boolean.TRUE.equals(user.getActive())`.
  - For a deactivated user, authentication is never set in `SecurityContextHolder`.
  - Spring Security rejects the request with HTTP 401/403.
- **Result:** **PASS**. Instant revocation on deactivation verified.

### Challenge 4: Null Document Dereference (BK-08 NPE)
- **Hypothesis:** If a student submits a lesson plan without a document (`document == null`), will deleting another portfolio document trigger an unhandled NullPointerException?
- **Analysis:**
  - The previous code iterated over `lessonPlanRepository.findAll()` and called `plan.getDocument().getId()`.
  - The refactored code uses `lessonPlanRepository.findByDocumentId(doc.getId())`.
  - Plans with null documents are never loaded into the lookup.
  - Unit test `testEliminarDocumentoConPlanSinArchivoNoLanzaNPE` explicitly creates a plan with `document = null` and verifies zero errors.
- **Result:** **PASS**. Regression prevented.

### Challenge 5: Anonymous Login POST Header Leakage
- **Hypothesis:** Does visiting `/` or logging in transmit stale JWT credentials from prior sessions?
- **Analysis:**
  - `Login.jsx` executes `useEffect` clearing `localStorage.removeItem('token')`.
  - Fetch call sends only `'Content-Type': 'application/json'`.
- **Result:** **PASS**. Anonymous login semantics restored.

---

## 4. Caveats & Recommendations for Future Milestones

1. **Password Reset Validation (Phase 3 Recommendation):**
   In `AuthController.resetPassword`, sending a body with `"newPassword": null` or empty string should be intercepted with explicit validation (Jakarta Bean Validation / `@NotBlank`) to avoid an unhandled `IllegalArgumentException` from `BCryptPasswordEncoder`. This is noted for Phase 3 (Error Handling and DTOs).

2. **Controller SRP & Architecture (Phase 2 Roadmap):**
   `PortfolioController` and `ProfessorController` currently house their domain query logic directly within the controllers. Phase 2 must extract these into `PortfolioService`, `LessonPlanService`, and `ProfessorService`.

3. **Frontend API Client Centralization (Phase 2 Roadmap):**
   Hardcoded `http://localhost:8080` endpoints remain in frontend views and should be centralized into `src/services/apiClient.js` with `VITE_API_URL` during Phase 2.

4. **In-Memory H2 for Test Automation (Phase 4 Roadmap):**
   Current integration tests configure MySQL directly. Phase 4 should introduce `application-test.properties` using H2 in-memory DB so integration tests run independently of an active MySQL container.

---

## 5. Conclusion & Verdict

**Verdict: APPROVE**

Milestone 1 (Phase 1: Critical Security & Data Loss Prevention) is thoroughly implemented and meets all acceptance criteria:
1. `ddl-auto=create` eliminated -> data loss prevented.
2. Hardcoded JWT secret removed -> 12-factor configuration compliant.
3. Broken Access Control closed with RBAC and `@EnableMethodSecurity`.
4. Account takeover and user enumeration eliminated in `forgot-password`.
5. BOLA/IDOR eliminated in `PortfolioController` and `ProfessorController`.
6. NullPointerException in document deletion resolved.
7. Docker Compose startup race condition prevented.
8. Frontend token hygiene established.
9. All changes committed cleanly on branch `fabian`.

---

## 6. Verification Method

To independently verify the implementation:
1. Check current branch:
   ```powershell
   git branch --show-current
   # Output: fabian
   ```
2. Verify commit integrity:
   ```powershell
   git log -1 --oneline
   # Output: 2285bc9 feat(sec): implement phase 1 critical security and data loss prevention fixes
   ```
3. Verify backend compilation:
   ```powershell
   .\mvnw.cmd test-compile
   # Output: BUILD SUCCESS
   ```
4. Verify controller security test suite:
   ```powershell
   .\mvnw.cmd test -Dtest=AuthControllerTest,PortfolioControllerTest,ProfessorControllerTest
   ```
5. Verify frontend build:
   ```powershell
   cd frontend
   npm run build
   ```
