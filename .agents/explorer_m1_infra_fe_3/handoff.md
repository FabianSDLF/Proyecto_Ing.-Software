# Handoff Report: Milestone 1 — Infra & Frontend Auth Hygiene (Explorer 3)

- **Date**: 2026-09-23T19:05:00Z
- **Author**: Explorer 3 (`explorer_m1_infra_fe_3`)
- **Status**: Complete (Hard Handoff)
- **Target Role**: Milestone 1 Implementation Worker / Sentinel / Orchestrator

---

## 1. Observation

### 1.1 Git Branch and Status
Execution of `git status` and `git branch -a` in `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas`:
- Current branch: `testing`
- Local branches found:
  - `main`
  - `* testing`
- Remote branches found:
  - `remotes/origin/HEAD -> origin/main`
  - `remotes/origin/main`
  - `remotes/origin/testing`
- Branch `fabian` does **not** currently exist either locally or remotely.
- Status:
  - Modified untracked items are exclusively inside `.agents/` (`ORIGINAL_REQUEST.md`, `sentinel/BRIEFING.md`). Working directory source code is 100% clean.

### 1.2 TR-08: Docker Compose Race Condition
File: `docker-compose.yml` (49 lines total)
- Lines 5–16:
  ```yaml
    # Base de datos MySQL
    db:
      image: mysql:8.0
      container_name: practicas_db
      environment:
        MYSQL_ROOT_PASSWORD: admin
        MYSQL_DATABASE: sistema_practicas
      ports:
        - "3306:3306"
      volumes:
        - db_data:/var/lib/mysql
      restart: unless-stopped
  ```
  The `db` service has no `healthcheck` defined.
- Lines 18–25:
  ```yaml
    # Backend Spring Boot
    backend:
      build: .
      container_name: practicas_backend
      ports:
        - "8080:8080"
      depends_on:
        - db
  ```
  The `backend` service uses simple process-level dependency `depends_on: [ - db ]`, which does not check whether MySQL is actually accepting TCP connections or ready for SQL queries.

### 1.3 TR-04: Token Orphan in `localStorage`
File: `frontend/src/layouts/MainLayout.jsx`
- Line 18 (in `useEffect` catch block):
  ```javascript
              } catch (error) {
                  console.error("Error parsing user from localStorage:", error);
                  localStorage.removeItem('usuarioActivo');
                  navigate('/');
              }
  ```
- Lines 131–138 (Logout button):
  ```jsx
                              {/* Botón de Logout sutil al lado */}
                              <button 
                                  onClick={() => { localStorage.removeItem('usuarioActivo'); navigate('/'); }}
                                  className="ml-2 p-2 text-gray-400 hover:text-red-400 hover:bg-gray-800 hover:bg-opacity-40 rounded-lg transition-colors"
                                  title="Cerrar Sesión"
                              >
                                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                              </button>
  ```
- In both lines 18 and 132, only `localStorage.removeItem('usuarioActivo')` is executed. The JWT key `'token'` stored by `Login.jsx:31` (`localStorage.setItem('token', authData.token)`) is never removed upon logout or JSON parsing error.

File: `frontend/src/pages/Login.jsx`
- Lines 29–33:
  ```javascript
                  // (Opcional pero recomendado) Guardamos el usuario en la memoria del navegador
                  // para que las otras pantallas sepan quién inició sesión.
                  localStorage.setItem('token', authData.token);
                  localStorage.setItem('usuarioActivo', JSON.stringify(authData.user));
  ```
  `Login.jsx` stores both `'token'` and `'usuarioActivo'`, but contains no lifecycle cleanup (`useEffect`) upon mounting to ensure that entering the login page clears pre-existing or orphaned tokens.

File: `frontend/src/pages/Dashboard.jsx`
- Line 31 (Defensive observation):
  ```javascript
              } catch (error) {
                  console.error("Error parsing user from localStorage:", error);
                  localStorage.removeItem('usuarioActivo');
              }
  ```
  Parsing error in `Dashboard.jsx` also leaves `'token'` behind.

### 1.4 TR-05: Incoherent Authorization Header in Login Request
File: `frontend/src/pages/Login.jsx`
- Lines 18–23:
  ```javascript
          try {
              const respuesta = await fetch('http://localhost:8080/api/auth/login', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                  body: JSON.stringify({ email: email, password: password })
              });
  ```
  Line 21 injects `'Authorization': 'Bearer ' + localStorage.getItem('token')` into the POST request to `/api/auth/login`. If no token exists, it evaluates to `"Bearer null"`. If a stale orphaned token exists from a previous user (due to TR-04), it sends that obsolete token to the public authentication endpoint.

### 1.5 Verification Tooling Baseline
- Running `.\mvnw.cmd compile` in root directory: Exits with code 0 (`BUILD SUCCESS` in 2.2s).
- Running `npm.cmd run build` in `frontend/`: Exits with code 0 (`vite v8.3.0 building client environment for production... built in 656ms`).
- Running `npm.cmd run lint` in `frontend/`: Exits with code 0 (30 warnings, 0 errors).
- Note on Windows PowerShell: Direct execution of `npm` or `mvn` fails due to PowerShell script execution policy (`npm.ps1` restricted) or missing global PATH; explicitly using `npm.cmd` and `.\mvnw.cmd` succeeds deterministically.

---

## 2. Logic Chain

1. **Git Branch Creation:**
   - Observation 1.1 reveals the repository is on branch `testing`, and branch `fabian` does not exist.
   - Per requirement R1 in `ORIGINAL_REQUEST.md`, all implementation work must occur on branch `fabian`.
   - Therefore, the Worker must execute `git checkout -b fabian` prior to any code edits.

2. **Docker Compose Startup Race Condition (TR-08):**
   - Observation 1.2 shows that `backend` depends on `db` via `depends_on: [db]`.
   - Docker Compose starts `db` and immediately proceeds to spin up `backend`.
   - MySQL 8.0 requires 15–30 seconds to initialize its storage engine, perform InnoDB recovery/checks, and bind port 3306.
   - Spring Boot connects immediately at boot time through HikariCP. Failure to reach MySQL immediately causes `CommunicationsLinkException` and process termination.
   - Defining a `healthcheck` on `db` using `mysqladmin ping -h localhost -u root -padmin` with `start_period: 30s` tests daemon readiness. Setting `depends_on: { db: { condition: service_healthy } }` on `backend` guarantees that Spring Boot will only boot once MySQL is confirmed ready to receive connections.

3. **CWE-613 Client-side Token Hygiene (TR-04):**
   - Observation 1.3 shows `Login.jsx` creates two keys: `'token'` and `'usuarioActivo'`.
   - When logging out via `MainLayout.jsx:132`, only `'usuarioActivo'` is purged.
   - This leaves the active JWT valid in `localStorage.getItem('token')` for the remainder of its expiration window (up to 24 hours), enabling session reuse or token extraction on shared workstations.
   - Furthermore, navigating back to `/` leaves the old token in browser storage.
   - Resolving this requires:
     a) Removing both `'usuarioActivo'` and `'token'` in `MainLayout.jsx` (in `handleLogout` and parsing catch).
     b) Adding a clean mount `useEffect` in `Login.jsx` that removes both keys whenever the login view is rendered.
     c) Updating `Dashboard.jsx:31` catch block defensively.

4. **HTTP Auth Protocol Semantics (TR-05):**
   - Observation 1.4 confirms line 21 of `Login.jsx` attaches `'Authorization': 'Bearer ' + localStorage.getItem('token')` to `/api/auth/login`.
   - Public credential authentication endpoints must never require or supply authorization headers.
   - Because of TR-04, this header sent the previous session's token (or `"Bearer null"`). If backend filters inspect this header prior to authentication or if the token expired, it can cause 401/403 rejections or security filter anomalies.
   - Removing `'Authorization'` from `Login.jsx:21` headers restores correct anonymous HTTP POST semantics.

---

## 3. Caveats

1. **Docker Engine Execution**: While the `docker-compose.yml` healthcheck syntax is standard Docker Compose v3.8, actual execution of `docker compose up` depends on the host machine having Docker Desktop running with available resources. The configuration syntax itself can be verified syntactically.
2. **Phase 1 Scope Isolation**: In Phase 2, a complete `AuthContext` and unified `apiClient.js` will be introduced to eliminate direct `fetch` calls and raw `localStorage` interactions throughout all 12 frontend pages. Phase 1 purposefully addresses the immediate security hygiene issues without overhauling the entire client architecture prematurely.
3. **Password in Healthcheck**: The healthcheck command specifies `-padmin` directly matching the `MYSQL_ROOT_PASSWORD: admin` environment variable defined in the same file. In Phase 1 / Phase 2 credential externalization (TR-07), this can be parameterized if `.env` is introduced.

---

## 4. Conclusion & Concrete Implementation Plan

The Worker should perform the following exact sequence of changes:

### Step 0: Branch Setup
Execute in repo root:
```powershell
if (git branch --list fabian) {
    git checkout fabian
} else {
    git checkout -b fabian
}
```

---

### Step 1: Fix TR-08 in `docker-compose.yml`
Target file: `docker-compose.yml`

#### Proposed Unified Diff:
```diff
--- a/docker-compose.yml
+++ b/docker-compose.yml
@@ -14,14 +14,21 @@
     volumes:
       - db_data:/var/lib/mysql
+    healthcheck:
+      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-padmin"]
+      interval: 10s
+      timeout: 5s
+      retries: 5
+      start_period: 30s
     restart: unless-stopped
 
   # Backend Spring Boot
   backend:
     build: .
     container_name: practicas_backend
     ports:
       - "8080:8080"
     depends_on:
-      - db
+      db:
+        condition: service_healthy
     environment:
```

#### Full Target `docker-compose.yml` snippet (services `db` and `backend`):
```yaml
services:
  # Base de datos MySQL
  db:
    image: mysql:8.0
    container_name: practicas_db
    environment:
      MYSQL_ROOT_PASSWORD: admin
      MYSQL_DATABASE: sistema_practicas
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-padmin"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    restart: unless-stopped

  # Backend Spring Boot
  backend:
    build: .
    container_name: practicas_backend
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    environment:
      # Sobrescribimos las variables de entorno de application.properties
      # para que apunte al contenedor "db" en vez de "localhost"
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/sistema_practicas?allowPublicKeyRetrieval=true&useSSL=false
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=admin
    volumes:
      # Volumen para que los archivos subidos persistan si el contenedor se reinicia
      - backend_uploads:/app/uploads
    restart: unless-stopped
```

---

### Step 2: Fix TR-04 in `frontend/src/layouts/MainLayout.jsx`
Target file: `frontend/src/layouts/MainLayout.jsx`

#### Key changes:
1. In `useEffect` parsing error (line ~18), remove both `'usuarioActivo'` and `'token'`.
2. Introduce a dedicated `handleLogout` function:
   ```javascript
   const handleLogout = () => {
       localStorage.removeItem('usuarioActivo');
       localStorage.removeItem('token');
       navigate('/');
   };
   ```
3. Use `handleLogout` in the logout button (line ~132).

#### Proposed Unified Diff:
```diff
--- a/frontend/src/layouts/MainLayout.jsx
+++ b/frontend/src/layouts/MainLayout.jsx
@@ -16,6 +16,7 @@ export default function MainLayout() {
             } catch (error) {
                 console.error("Error parsing user from localStorage:", error);
                 localStorage.removeItem('usuarioActivo');
+                localStorage.removeItem('token');
                 navigate('/');
             }
         } else {
@@ -25,6 +26,12 @@ export default function MainLayout() {
 
     const userRole = user?.role?.name || null;
 
+    const handleLogout = () => {
+        localStorage.removeItem('usuarioActivo');
+        localStorage.removeItem('token');
+        navigate('/');
+    };
+
     // Función para obtener las iniciales (Ej: Fabián Sepúlveda -> FS)
     const getInitials = (name) => {
         if (!name) return 'U';
@@ -129,7 +136,7 @@ export default function MainLayout() {
 
                             {/* Botón de Logout sutil al lado */}
                             <button 
-                                onClick={() => { localStorage.removeItem('usuarioActivo'); navigate('/'); }}
+                                onClick={handleLogout}
                                 className="ml-2 p-2 text-gray-400 hover:text-red-400 hover:bg-gray-800 hover:bg-opacity-40 rounded-lg transition-colors"
                                 title="Cerrar Sesión"
                             >
```

---

### Step 3: Fix TR-04 and TR-05 in `frontend/src/pages/Login.jsx`
Target file: `frontend/src/pages/Login.jsx`

#### Key changes:
1. Import `useEffect` from `'react'`.
2. Add mount cleanup in `Login` component to purge residual session tokens upon accessing the login page.
3. Remove `'Authorization': 'Bearer ' + localStorage.getItem('token')` from the login fetch request headers.

#### Proposed Unified Diff:
```diff
--- a/frontend/src/pages/Login.jsx
+++ b/frontend/src/pages/Login.jsx
@@ -1,4 +1,4 @@
-import { useState } from 'react';
+import { useState, useEffect } from 'react';
 import { useNavigate, Link } from 'react-router-dom';
 import loginImage from '../assets/loginImage.jpeg';
 
@@ -10,6 +10,12 @@ export default function Login() {
 
     const navigate = useNavigate();
 
+    // Limpiar cualquier sesión residual en localStorage al entrar a la pantalla de login
+    useEffect(() => {
+        localStorage.removeItem('usuarioActivo');
+        localStorage.removeItem('token');
+    }, []);
+
     // 2. La función que se dispara al presionar el botón
     const handleLogin = async (e) => {
         e.preventDefault(); // Evita que la página web titile/recargue
@@ -18,7 +24,7 @@ export default function Login() {
         try {
             const respuesta = await fetch('http://localhost:8080/api/auth/login', {
                 method: 'POST',
-                headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
+                headers: { 'Content-Type': 'application/json' },
                 body: JSON.stringify({ email: email, password: password })
             });
```

---

### Step 4 (Defensive recommendation): `frontend/src/pages/Dashboard.jsx`
Target file: `frontend/src/pages/Dashboard.jsx`

#### Proposed Diff:
```diff
--- a/frontend/src/pages/Dashboard.jsx
+++ b/frontend/src/pages/Dashboard.jsx
@@ -29,6 +29,7 @@ export default function Dashboard() {
             } catch (error) {
                 console.error("Error parsing user from localStorage:", error);
                 localStorage.removeItem('usuarioActivo');
+                localStorage.removeItem('token');
             }
         }
     }, []);
```

---

## 5. Verification Method

### 5.1 Verification Commands
1. **Branch Verification:**
   ```powershell
   git branch --show-current
   ```
   *Expected result:* Outputs `fabian`.

2. **Frontend Build Verification:**
   ```powershell
   cd "frontend"
   npm.cmd run build
   ```
   *Expected result:* Vite completes build successfully without syntax errors or broken imports (exit code 0).

3. **Frontend Linting Verification:**
   ```powershell
   cd "frontend"
   npm.cmd run lint
   ```
   *Expected result:* Oxlint completes with 0 errors (exit code 0).

4. **Backend Build Verification:**
   ```powershell
   cd "c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas"
   .\mvnw.cmd compile
   ```
   *Expected result:* Maven compiles backend successfully (exit code 0).

5. **Docker Compose Configuration Validation:**
   ```powershell
   docker compose config
   ```
   *Expected result:* Validates YAML syntax without error and shows `healthcheck` under `db` and `condition: service_healthy` under `backend.depends_on.db`.

### 5.2 Invalidation Conditions
- If `Login.jsx` continues to send `'Authorization'` in the POST request headers to `/api/auth/login`.
- If clicking "Cerrar Sesión" in `MainLayout.jsx` leaves `'token'` in `localStorage`.
- If `backend` service in `docker-compose.yml` does not wait for `service_healthy`.
- If changes are committed to any branch other than `fabian`.
