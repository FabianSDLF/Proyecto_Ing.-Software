# Handoff Report — Phase 3: Frontend Code Quality, Oxlint Audit, React Lifecycle Hygiene & Dead Code Cleanup

- **Agent:** explorer_m3_fe_3 (`teamwork_preview_explorer`)
- **Working Directory:** `c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_fe_3`
- **Target Subsystem:** `frontend/src/` (Pages, Lifecycle, Oxlint, Services & UI Integration, Assets & Configuration)
- **Recipient:** Parent Orchestrator (`403313ad-9e49-4fa6-96a7-fe91e61771bb`) / Phase 3 Worker Agents
- **Status:** Hard Handoff (Complete Exhaustive Investigation & Refactoring Blueprints)

---

## 1. Observation

A systematic static audit of `frontend/src/`, `frontend/index.html`, `frontend/package.json`, `frontend/.oxlintrc.json`, `frontend/Dockerfile`, and the repository `.gitignore` was conducted. The following facts were directly verified in the codebase:

### 1.1. Status of Phase 2 Artifacts vs. Page Adoption (FE-01, FE-02, FE-04, FE-06, FE-23)
- In Phase 2, the following foundational modules were created:
  * Centralized HTTP client: `frontend/src/services/apiClient.js` (with automatic JWT injection, error normalization, `signal` for `AbortController`, and `downloadBlob`).
  * 6 domain services: `assignmentService.js`, `authService.js`, `centerService.js`, `portfolioService.js`, `subjectService.js`, `userService.js`.
  * 8 UI components: `src/components/ui/` (`Button`, `Input`, `Select`, `Modal`, `DataTable`, `ConfirmDialog`, `Badge`, `Spinner`, `index.js`).
  * Context & Guards: `AuthContext.jsx`, `ProtectedRoute.jsx`, `RoleRoute.jsx`.
  * Routed Layout: `App.jsx` and `layouts/MainLayout.jsx`.
- **CRITICAL OBSERVATION:** **0 out of the 12 views in `frontend/src/pages/` have adopted these Phase 2 services or UI components.**
- All 12 pages continue executing raw `fetch('http://localhost:8080/api/...')` with manual `localStorage.getItem('token')` headers:
  1. `Asignaciones.jsx`: lines 35, 36, 37, 38, 39, 40 (inside `Promise.all`), 96 (POST), 121 (DELETE). (8 raw `fetch` calls).
  2. `CargaMasiva.jsx`: line 21 (`fetch('http://localhost:8080/api/usuarios/carga-masiva', ...)`).
  3. `Dashboard.jsx`: line 19 (`fetch('http://localhost:8080/api/asignaciones', ...)`), line 24 (`fetch('http://localhost:8080/api/centros', ...)`). (2 raw `fetch` calls).
  4. `ForgotPassword.jsx`: line 20 (`fetch('http://localhost:8080/api/auth/forgot-password', ...)`).
  5. `GestionAsignaturas.jsx`: line 14 (GET), line 58 (POST), line 84 (DELETE). (3 raw `fetch` calls).
  6. `GestionRoles.jsx`: line 43 (GET users), line 56 (GET roles), line 82 (POST user), line 110 (PUT user), line 134 (PATCH status). (5 raw `fetch` calls).
  7. `Login.jsx`: line 25 (`fetch('http://localhost:8080/api/auth/login', ...)`).
  8. `MisEstudiantes.jsx`: line 38 (`fetch(endpoint, ...)` with hardcoded string routing).
  9. `NuevaPlanificacion.jsx`: line 59 (`fetch('http://localhost:8080/api/portafolio/planificaciones/upload', ...)`).
  10. `Perfil.jsx`: line 22 (GET), line 65 (PUT). (2 raw `fetch` calls).
  11. `Portafolio.jsx`: line 33 (GET evidencias), line 43 (GET planificaciones), line 78 (POST evidencia), line 110 (POST plan), line 135 (GET file download), line 162 (DELETE). (6 raw `fetch` calls).
  12. `ResetPassword.jsx`: line 39 (`fetch('http://localhost:8080/api/auth/reset-password', ...)`).
  * **Total raw `fetch('http://localhost:8080...')` calls remaining:** **32 instances across 12 pages.**

### 1.2. Leftover Reload Antipattern (`window.location.reload()`) (FE-04)
- In `frontend/src/pages/Perfil.jsx:84`:
  ```javascript
  83: // Recargar página para que se actualice el avatar del menú
  84: setTimeout(() => window.location.reload(), 1000);
  ```
  `Perfil.jsx` still forces a full browser reload instead of calling `updateUserProfile(updatedUser)` provided by `useAuth()`.

### 1.3. React Component Lifecycle & Hook Hygiene
- **Synchronous `setState` in `useEffect` (FE-12 / Oxlint `react(set-state-in-effect)`):**
  * `Dashboard.jsx:8-34`: `useEffect` reads `localStorage.getItem('usuarioActivo')` and executes `setUser(parsedUser)`.
  * `GestionRoles.jsx:17-38`: `useEffect` reads `localStorage` and executes `setUsuarioActual(parsedUser)`.
  * `GestionAsignaturas.jsx:26-47`: `useEffect` reads `localStorage` to check role permissions.
  * `Asignaciones.jsx:56-76`: `useEffect` reads `localStorage` to check role permissions.
- **Absence of Request Cancellation via `AbortController` (FE-14):**
  * None of the 12 views cancel active fetch requests on component unmount.
  * In `Dashboard.jsx:19-27`, `Asignaciones.jsx:26-53`, `Portafolio.jsx:30-48`, `MisEstudiantes.jsx:14-50`, `GestionRoles.jsx:40-64`, `GestionAsignaturas.jsx:12-24`, and `Perfil.jsx:15-48`, fast tab switching triggers unmounted state mutation warnings (`Warning: Can't perform a React state update on an unmounted component`).
- **Temporal Dead Zone (TDZ) in Hooks (FE-13):**
  * `MisEstudiantes.jsx:10-12`: `useEffect(() => { cargarEstudiantes(); }, []);` invokes `cargarEstudiantes` physically before its definition on line 14.
  * `GestionRoles.jsx:36-37`: `cargarUsuarios()` and `cargarRoles()` are called in `useEffect` on lines 36-37, before their declarations on lines 40 and 54.
- **`useEffect` Missing Dependencies:**
  * `MisEstudiantes.jsx:12`: `[]` omits `navigate` and `cargarEstudiantes`.
  * `Dashboard.jsx:35`: `[]` omits `stats` functions and dependencies.
  * `Portafolio.jsx:53`: `[]` omits `fetchEvidencias` and `fetchPlanificaciones`.
  * `GestionAsignaturas.jsx:47`: `[navigate]` omits `fetchAsignaturas`.
  * `GestionRoles.jsx:38`: `[navigate]` omits `cargarUsuarios` and `cargarRoles`.
  * `Asignaciones.jsx:76`: `[navigate]` omits `cargarDatos`.
  * `components/ui/Modal.jsx:47`: body scroll lock `useEffect` has missing dependency return handling.

### 1.4. Oxlint Audit & Fast Refresh Smells
- **Unused Imports and Variables:**
  * `NuevaPlanificacion.jsx:1`: `import React, { useState, useEffect } from 'react';` — `React` is redundant under React 19 JSX runtime; `useEffect` is imported but NEVER used in the file.
  * `NuevaPlanificacion.jsx:26`: `const [hasActiveAssignment, setHasActiveAssignment] = useState(true);` is declared but never read or referenced.
  * `Portafolio.jsx:18`: `const [showPlanModal, setShowPlanModal] = useState(false);` is never toggled by active UI.
  * `Portafolio.jsx:19-24`: `const [planForm, setPlanForm] = useState(...)` is completely dead code (only used in unreachable dead JSX).
  * `Portafolio.jsx:25`: `const [uploadingPlan, setUploadingPlan] = useState(false);` is unused.
- **Fast Refresh Smell (`react/only-export-components`):**
  * In `src/context/AuthContext.jsx`: Line 9 exports component `AuthProvider`, line 102 exports custom hook `useAuth`, and line 110 exports context `AuthContext`.
- **Unhandled Promises & Error Swallowing:**
  * `Dashboard.jsx:22,27`: `.catch(err => console.error(err))` swallows network errors silently without notifying the user.
  * `GestionAsignaturas.jsx:21`: `catch { toast.error('Error cargando asignaturas'); }` swallows raw network rejections.

### 1.5. Dead Code, File Residue & Bad Hacks
- **50 Lines of Unreachable Modal Code (FE-07):**
  * `Portafolio.jsx:276-325`: An entire 50-line modal form is wrapped in `{false && ( <div className="fixed inset-0...">...</div> )}`.
- **Dummy File Workaround Hack (FE-08):**
  * `NuevaPlanificacion.jsx:49`:
    ```javascript
    formData.append('file', new Blob([""], { type: "text/plain" }), "planificacion_sin_archivo.txt");
    ```
    Creates a simulated empty text file to bypass backend requirement instead of sending standard optional multipart or null.
- **Redundant Navigation Branches (FE-07):**
  * `Login.jsx:44-53`:
    ```javascript
    if (rolDelUsuario === 'Administrador') { navigate('/dashboard'); }
    else if (rolDelUsuario === 'Estudiante') { navigate('/dashboard'); }
    else if (['Profesor de Asignatura', ...].includes(rolDelUsuario)) { navigate('/dashboard'); }
    ```
    Three distinct `if/else` branches all execute `navigate('/dashboard')`.
- **Orphan Static Assets:**
  * `frontend/src/assets/react.svg` (4,126 bytes): Unused Vite boilerplate asset.
  * `frontend/src/assets/vite.svg` (8,710 bytes): Unused Vite boilerplate asset.
- **Missing Asset in `frontend/public/` & Broken Favicon (FE-20):**
  * `frontend/index.html:5`: `<link rel="icon" type="image/png" href="/logoUBB.png" />`.
  * `frontend/public/` contains: `favicon.svg`, `hero.png`, `icons.svg`. **`logoUBB.png` is missing from `public/`**, resulting in an HTTP 404 for the favicon on every page load. (The image currently exists only under `frontend/src/assets/logoUBB.png`).
- **HTML Document Language Mismatch (FE-20):**
  * `frontend/index.html:2`: `<html lang="en">` for a 100% Spanish application.
- **Critical Exclusions Missing from Root `.gitignore` (TR-16):**
  * `.gitignore` does not exclude `.env`, `*.env`, `uploads/`, `*.b64`, or `*.dump`.
- **Frontend Dockerfile Vulnerabilities (TR-10, FE-23):**
  * `frontend/Dockerfile:5`: `RUN npm install` instead of deterministic `npm ci`.
  * `frontend/Dockerfile:14`: Nginx default config does not proxy `/api` requests to backend (`http://backend:8080`).

### 1.6. UTF-8 Character Corruption (FE-21)
- `MisEstudiantes.jsx:24`: `"No tienes permiso para ver esta pǭgina."`
- `NuevaPlanificacion.jsx:15`: `// Campos que irn en contentJson`
- `NuevaPlanificacion.jsx:141`: `placeholder="Sntesis, evaluación formativa..."`
- `NuevaPlanificacion.jsx:160`: `Archivo Adjunto (Opcional - Gua, Material)`

### 1.7. Blocking Native Dialogs & Non-Accessible Modals (FE-18, FE-19)
- `window.confirm()` blocking calls:
  * `GestionAsignaturas.jsx:80`: `if (!window.confirm('¿Está seguro de que desea eliminar esta asignatura?'))`
  * `Asignaciones.jsx:119`: `if (window.confirm('¿Está seguro de eliminar esta asignación?'))`
  * `Portafolio.jsx:157`: `if (!confirm(`¿Seguro que deseas eliminar est${isPlan ? 'a planificación' : 'e archivo'}?...`))`
- Inaccessible artisanal `<div>` modals:
  * `Asignaciones.jsx:200-257`: Modal lack `role="dialog"`, `aria-modal="true"`, focus trap, and Escape key dismissal.

### 1.8. Leaky Abstractions & Technical Detail Exposure (FE-09, FE-22)
- `GestionRoles.jsx:75`: Sends `passwordHash: formData.password` directly in user creation payload.
- `Login.jsx:62`: `"El servidor de Java está apagado o no responde"`.
- `CargaMasiva.jsx:38`: `"El servidor Java está apagado."`.

---

## 2. Logic Chain

1. **Phase 2 Infrastructure Ready for Integration:**
   - In Phase 2, `apiClient.js` was created with comprehensive capabilities: auto JWT injection, baseUrl from `import.meta.env.VITE_API_URL`, `signal` support for `AbortController`, error class `ApiError`, and `downloadBlob`.
   - The 6 domain services (`assignmentService`, `authService`, `centerService`, `portfolioService`, `subjectService`, `userService`) define standard async methods for all REST operations.
   - The 8 UI components in `src/components/ui/` (`Button`, `Input`, `Select`, `Modal`, `DataTable`, `ConfirmDialog`, `Badge`, `Spinner`) provide accessible, styled building blocks.
   - Therefore, the remaining work is purely to refactor the 12 views in `frontend/src/pages/` to consume these services and UI components, eliminating all 32 raw `fetch` calls, all raw `localStorage` calls, and all manual modal/table code.

2. **Lifecycle Hygiene & Performance:**
   - Replacing direct `localStorage` reads in `Dashboard.jsx`, `GestionRoles.jsx`, `GestionAsignaturas.jsx`, and `Asignaciones.jsx` with `useAuth()` eliminates cascading re-renders and resolves FE-12 (`react(set-state-in-effect)`).
   - In every view that loads data on mount, adding `const controller = new AbortController();` inside `useEffect`, passing `{ signal: controller.signal }` to the service method, and returning `() => controller.abort()` guarantees that unmounted components never leak memory or trigger state updates (resolving FE-14).
   - Moving function definitions before `useEffect` hooks or declaring them inside the effect resolves FE-13 (Temporal Dead Zone).

3. **Dead Code & Hygiene Cleanup:**
   - Deleting lines 276-325 in `Portafolio.jsx` eliminates 50 lines of dead JSX (FE-07).
   - Removing unused imports (`React`, `useEffect`) and unused state (`hasActiveAssignment`) from `NuevaPlanificacion.jsx` resolves Oxlint unused variable warnings.
   - Deleting `react.svg` and `vite.svg` cleans up 12 KB of unreferenced template files.
   - Copying `logoUBB.png` into `frontend/public/` fixes the 404 favicon error and changing `index.html` to `<html lang="es">` satisfies WCAG 3.1.1 (FE-20).
   - Updating `.gitignore` with `.env`, `uploads/`, `*.b64`, `*.dump` closes security risk TR-16.

4. **Component Decoupling (SRP, DRY, WAI-ARIA):**
   - Replacing native `window.confirm()` with `<ConfirmDialog />` in `GestionAsignaturas`, `Asignaciones`, and `Portafolio` makes confirmations non-blocking, keyboard accessible, and visually consistent (FE-19).
   - Replacing artisanal modals with `<Modal />` provides automatic Escape key dismiss, backdrop click, body scroll lock, and WAI-ARIA compliance (FE-18).
   - Replacing manual HTML tables with `<DataTable />` provides built-in pagination, sorting, loading spinners, and empty states.
   - Replacing `document.createElement('a')` in `Portafolio.jsx` with `portfolioService.downloadDocument()` and `downloadBlob()` adheres to React's declarative model (FE-24).

---

## 3. Caveats

1. **No Backend Modifications in Frontend Scope:**
   - The dummy file workaround in `NuevaPlanificacion.jsx:49` (`planificacion_sin_archivo.txt`) was originally added because the backend `PortfolioController` historically expected a multipart file. While `portfolioService.createLessonPlan` supports optional file uploads without generating dummy blobs, backend controller testing must confirm `@RequestPart(value = "file", required = false)`.
2. **Third-Party NPM Packages:**
   - `lucide-react` is not installed in `package.json`. Rather than installing external icon dependencies that might conflict, all icons should remain semantic inline SVGs styled with Tailwind CSS, matching the pattern established in `MainLayout.jsx` and `src/components/ui/`.
3. **Vitest / Testing Suite:**
   - Playwright is present in `devDependencies` without config. Unit tests for components (Vitest + React Testing Library) belong to Phase 4.

---

## 4. Conclusion & Actionable Implementation Blueprints

All 12 pages under `frontend/src/pages/` must be refactored to consume `apiClient` / domain services and UI components. Below are the precise implementation blueprints for the Worker agent:

### Blueprint 1: `Login.jsx` Refactoring
- **Target:** `frontend/src/pages/Login.jsx`
- **Actions:**
  1. Import `useAuth` from `../context/AuthContext`, `Input` and `Button` from `../components/ui`.
  2. Remove manual `localStorage.setItem('token', ...)` and `localStorage.setItem('usuarioActivo', ...)`.
  3. Replace raw `fetch` with `await login({ email, password })`.
  4. Replace 3 redundant role `if/else` checks with a single `navigate('/dashboard')`.
  5. Replace error message with generic `"Correo o contraseña incorrectos"` or `"No fue posible conectar con el servidor"`.
  6. Remove tutorial comments.
  7. Use `<Input>` for email/password and `<Button isLoading={isLoading}>`.

### Blueprint 2: `ForgotPassword.jsx` & `ResetPassword.jsx` Refactoring
- **Targets:** `frontend/src/pages/ForgotPassword.jsx`, `frontend/src/pages/ResetPassword.jsx`
- **Actions:**
  1. In `ForgotPassword.jsx`, replace raw `fetch` with `authService.forgotPassword(email)`. Use `<Input>` and `<Button>`.
  2. In `ResetPassword.jsx`, replace raw `fetch` with `authService.resetPassword({ token, newPassword })`.
  3. In `ResetPassword.jsx`, fix FE-16 orphan timer by storing timer ID in `useRef` and clearing on unmount:
     ```javascript
     const timerRef = useRef(null);
     useEffect(() => () => { if (timerRef.current) clearTimeout(timerRef.current); }, []);
     ```

### Blueprint 3: `Dashboard.jsx` Refactoring
- **Target:** `frontend/src/pages/Dashboard.jsx`
- **Actions:**
  1. Replace `localStorage` parsing and `setUser(parsedUser)` with `const { user, userRole } = useAuth();`.
  2. Implement `AbortController`:
     ```javascript
     useEffect(() => {
       if (userRole !== 'Administrador') return;
       const controller = new AbortController();
       async function loadStats() {
         try {
           const [assignments, centers] = await Promise.all([
             assignmentService.getAssignments({ signal: controller.signal }),
             centerService.getCenters({ signal: controller.signal }),
           ]);
           setStats({ asignaciones: assignments?.length || 0, centros: centers?.length || 0 });
         } catch (err) {
           if (err.name !== 'AbortError') console.error(err);
         }
       }
       loadStats();
       return () => controller.abort();
     }, [userRole]);
     ```
  3. Fix FE-17: Replace nested `<button>` inside `<Link to="/mis-estudiantes">` (line 135) with a styled `<span>`.

### Blueprint 4: `Perfil.jsx` Refactoring
- **Target:** `frontend/src/pages/Perfil.jsx`
- **Actions:**
  1. Replace direct `fetch` with `authService.getProfile({ signal })` and `authService.updateProfile(formData)`.
  2. Fix FE-04: Remove `setTimeout(() => window.location.reload(), 1000)`. Call `updateUserProfile(updatedUser)` from `useAuth()`.
  3. Add `AbortController` in `useEffect`.
  4. Replace form controls with `<Input>` and `<Button isLoading={loading}>`.

### Blueprint 5: `CargaMasiva.jsx` Refactoring
- **Target:** `frontend/src/pages/CargaMasiva.jsx`
- **Actions:**
  1. Fix FE-10: Correct function declaration syntax: `export default function CargaMasiva() {`.
  2. Replace raw `fetch` with `userService.uploadBatch(file)`.
  3. Fix FE-09: Replace `"El servidor Java está apagado."` with `"No fue posible conectar con el servidor."`.
  4. Use `<Button>` with loading indicator.

### Blueprint 6: `GestionRoles.jsx` Refactoring
- **Target:** `frontend/src/pages/GestionRoles.jsx`
- **Actions:**
  1. Fix FE-10: Rename exported function from `GestionUsuarios` to `GestionRoles`.
  2. Remove redundant `localStorage` role check in `useEffect` (handled by `RoleRoute`).
  3. Fix FE-13: Declare `cargarUsuarios` and `cargarRoles` before calling them, or bundle inside `useEffect` with `AbortController`.
  4. Replace 5 raw `fetch` calls with `userService.getUsers()`, `userService.getRoles()`, `userService.createUser()`, `userService.updateUser()`, and `userService.updateStatus()`.
  5. Fix FE-22: Send `password: formData.password` instead of `passwordHash`.
  6. Replace table with `<DataTable columns={...} data={usuarios} isLoading={loading} />`.
  7. Replace form popup with `<Modal isOpen={showForm} onClose={() => setShowForm(false)} title={...}>`.
  8. Use `<Input>`, `<Select>`, `<Button>`, `<Badge>`.

### Blueprint 7: `GestionAsignaturas.jsx` Refactoring
- **Target:** `frontend/src/pages/GestionAsignaturas.jsx`
- **Actions:**
  1. Remove redundant `localStorage` role check in `useEffect`.
  2. Replace raw `fetch` with `subjectService.getSubjects()`, `subjectService.createSubject()`, `subjectService.deleteSubject()`.
  3. Add `AbortController` in load effect.
  4. Fix FE-19: Replace `window.confirm()` with `<ConfirmDialog />`.
  5. Replace table with `<DataTable />`, form with `<Input>`, `<Select>`, `<Button>`, and status tags with `<Badge>`.

### Blueprint 8: `Asignaciones.jsx` Refactoring
- **Target:** `frontend/src/pages/Asignaciones.jsx`
- **Actions:**
  1. Fix FE-15: Replace fragile `Promise.all` with `Promise.allSettled` using domain services:
     ```javascript
     const [asigRes, estudRes, asigListRes, centrosRes, profesRes, tutoresRes] = await Promise.allSettled([
       assignmentService.getAssignments({ signal }),
       userService.getUsersByRole('Estudiante', { signal }),
       subjectService.getSubjects({ signal }),
       centerService.getCenters({ signal }),
       userService.getUsersByRole('Profesor Colaborador', { signal }),
       userService.getUsersByRole('Tutor', { signal }),
     ]);
     ```
  2. Replace raw create/delete `fetch` with `assignmentService.createAssignment()` and `assignmentService.deleteAssignment()`.
  3. Fix FE-18: Replace artisanal `<div>` modal with `<Modal isOpen={isModalOpen} onClose={...} title="Nueva Asignación">`.
  4. Fix FE-19: Replace `window.confirm()` with `<ConfirmDialog />`.
  5. Replace table with `<DataTable />` and badges with `<Badge>`.
  6. Add `AbortController` in `useEffect`.

### Blueprint 9: `MisEstudiantes.jsx` Refactoring
- **Target:** `frontend/src/pages/MisEstudiantes.jsx`
- **Actions:**
  1. Fix FE-13: Move `cargarEstudiantes` definition before `useEffect`.
  2. Fix FE-21: Correct `"pǭgina"` to `"página"`.
  3. Replace raw `fetch` with `assignmentService.getByProfessor(user.id, { signal })` or unified endpoint.
  4. Consume `useAuth()` instead of `localStorage`.
  5. Add `AbortController` in `useEffect`.
  6. Replace table with `<DataTable />`.

### Blueprint 10: `Portafolio.jsx` Refactoring
- **Target:** `frontend/src/pages/Portafolio.jsx`
- **Actions:**
  1. Fix FE-07: Delete 50 lines of dead modal code (`Portafolio.jsx:276-325`).
  2. Delete unused state variables (`showPlanModal`, `planForm`, `uploadingPlan`).
  3. Replace 6 raw `fetch` calls with `portfolioService.getDocuments()`, `portfolioService.getLessonPlans()`, `portfolioService.uploadEvidence()`, `portfolioService.downloadDocument()`, `portfolioService.deleteDocument()`, and `portfolioService.deleteLessonPlan()`.
  4. Fix FE-24: Use `portfolioService.downloadDocument(doc.fileUrl, doc.fileName)` which cleanly delegates to `downloadBlob()` without imperative DOM clutter in the page.
  5. Fix FE-19: Replace `confirm(...)` with `<ConfirmDialog />`.
  6. Add `AbortController` to load effects.
  7. Use `<DataTable />`, `<Button>`, `<Badge>`.

### Blueprint 11: `NuevaPlanificacion.jsx` Refactoring
- **Target:** `frontend/src/pages/NuevaPlanificacion.jsx`
- **Actions:**
  1. Remove unused imports `React`, `useEffect`.
  2. Remove unused state `hasActiveAssignment`.
  3. Fix FE-21: Correct corrupted accent strings: `"irn"` -> `"irán"`, `"Sntesis"` -> `"Síntesis"`, `"Gua"` -> `"Guía"`.
  4. Replace raw `fetch` with `portfolioService.createLessonPlan(planData)`.
  5. Clean up the dummy file hack: `portfolioService.createLessonPlan` only appends `file` if `planForm.file` is truthy.
  6. Use `<Input>`, `<Select>`, `<Button>`.

### Blueprint 12: Hygiene, Assets & Config Cleanup
- **Actions:**
  1. Delete unreferenced assets: `frontend/src/assets/react.svg` and `frontend/src/assets/vite.svg`.
  2. Copy `frontend/src/assets/logoUBB.png` into `frontend/public/logoUBB.png` to fix broken favicon 404 (FE-20).
  3. Fix `frontend/index.html:2`: Change `<html lang="en">` to `<html lang="es">`.
  4. Fix Root `.gitignore`: Add `.env`, `*.env`, `uploads/`, `*.b64`, `*.dump` (TR-16).
  5. Fix `frontend/Dockerfile`: Change `RUN npm install` to `RUN npm ci` (TR-10).

---

## 5. Verification Method

### 5.1. Static Code Quality & Oxlint Verification
Run Oxlint across the entire frontend:
```powershell
cd "c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\frontend"
npm run lint
```
**Expected Result:** 0 errors and 0 warnings. No `react/rules-of-hooks` errors, no unused imports/variables.

### 5.2. Frontend Build Verification
Verify production build compiles cleanly without errors:
```powershell
cd "c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\frontend"
npm run build
```
**Expected Result:** Vite output generated in `dist/` with exit code 0.

### 5.3. Search Verification for Eradicated Antipatterns
Run ripgrep / search to ensure zero leftover raw fetches or reloads:
1. Search for raw localhost: `grep -r "localhost:8080" frontend/src/` -> **Must return 0 results** (all URLs managed via `apiClient.js` / `VITE_API_URL`).
2. Search for reload: `grep -r "window.location.reload" frontend/src/` -> **Must return 0 results** (all state handled reactively via `AuthContext`).
3. Search for native confirms: `grep -r "window.confirm" frontend/src/` -> **Must return 0 results** (all confirmations use `<ConfirmDialog />`).
4. Search for dummy file: `grep -r "planificacion_sin_archivo" frontend/src/` -> **Must return 0 results**.
5. Check `.gitignore`: Verify `.env`, `uploads/`, `*.b64` are present.

### 5.4. Invalidation Conditions
- Any occurrence of `fetch("http://localhost:8080...")` remaining in `frontend/src/pages/`.
- Any component mutating state after unmount without `AbortController`.
- Any build failure in `npm run build` or lint failure in `npm run lint`.
