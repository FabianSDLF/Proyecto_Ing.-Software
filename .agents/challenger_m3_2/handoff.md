# 5-Component Adversarial Verification Report — challenger_m3_2 (Phase 3 Frontend Integration)

## Challenge Summary

**Overall risk assessment**: **LOW**  
**Explicit Verdict**: **APPROVE**

---

## 1. Observation

Direct empirical observations, commands, outputs, and file audits executed on branch `fabian` (HEAD commit `1c773f8`):

### 1.1 Static Code and Hardcoded URL Audit (Adversarial Test 3)
1. **Residual `localhost:8080` strings in `frontend/src/`**:
   - Ripgrep pattern `localhost:8080` across `frontend/src/`:
     ```
     {"File":"frontend\\src\\services\\apiClient.js","LineNumber":7,"LineContent":"const RAW_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';"}
     ```
   - Matches outside `apiClient.js:7`: **0**.
   - The only occurrence is the configurable environment fallback in `apiClient.js`.

2. **Native `fetch()` invocations in `frontend/src/`**:
   - Ripgrep pattern `fetch(` across `frontend/src/`:
     ```
     {"File":"frontend\\src\\services\\apiClient.js","LineNumber":74,"LineContent":"    const response = await fetch(url, config);"}
     ```
   - Matches outside `apiClient.js:74`: **0**.
   - Zero direct unintercepted `fetch()` calls exist in any page (`pages/`) or layout (`layouts/`).
   - Ripgrep queries for `window.fetch`, `global.fetch`, `XMLHttpRequest`, and `axios`: **0 matches**.

3. **Legacy browser anti-patterns audit**:
   - `window.location.reload`: **0 executable matches** in `frontend/src/` (1 documentation comment in `AuthContext.jsx:62`).
   - `window.confirm`: **0 matches** in `frontend/src/pages/`. Replaced uniformly with `<ConfirmDialog />`.
   - Dummy empty file hack (`planificacion_sin_archivo`): **0 matches** across `frontend/src/`.

### 1.2 Lint and Production Build Verification (Test 4)
1. **Linter Execution (`oxlint`)**:
   - Command: `cmd.exe /c npm run lint`
   - Exit code: `0`
   - Output:
     ```
     > frontend@0.0.0 lint
     > oxlint
     Found 6 warnings and 0 errors.
     Finished in 26ms on 36 files with 104 rules using 16 threads.
     ```
   - 0 syntax or type errors.

2. **Production Bundler Execution (`vite build`)**:
   - Command: `cmd.exe /c npm run build`
   - Exit code: `0`
   - Output:
     ```
     > frontend@0.0.0 build
     > vite build
     vite v8.3.0 building client environment for production...
     transforming...
     ✓ 61 modules transformed.
     rendering chunks...
     computing gzip size...
     dist/index.html                         0.47 kB │ gzip:   0.30 kB
     dist/assets/logoUBB-Ccsp2_Bd.png       84.73 kB
     dist/assets/loginImage-hCRWHrgu.jpeg  546.26 kB
     dist/assets/index-a22AebOJ.css         35.07 kB │ gzip:   6.91 kB
     dist/assets/index-CSRugaZH.js         350.21 kB │ gzip: 104.68 kB
     ✓ built in 240ms
     ```

3. **Backend Compilation Cross-Check**:
   - Command: `cmd.exe /c mvnw.cmd compile`
   - Output: `BUILD SUCCESS` (100 source files up to date).

### 1.3 Empirical Adversarial Test Harness Execution (Tests 1, 2, & Stress)
An automated adversarial test harness was executed using Node.js v24.20.0 with Vite SSR module resolution against `apiClient.js` and all domain service modules (`assignmentService`, `userService`, `subjectService`, `centerService`, `portfolioService`, `authService`).

Output:
```
====================================================
PITAGORAS - Phase 3 Adversarial Verification Harness
====================================================

--- SUITE 1: ApiClient Error Parsing & Contracts ---
  ✓ PASS: 1.1: ApiClient parses Spring Boot 400 ApiErrorResponse with validationErrors
  ✓ PASS: 1.2: ApiClient falls back to error field when message is absent
  ✓ PASS: 1.3: ApiClient handles 404 ResourceNotFoundException payload
  ✓ PASS: 1.4: ApiClient handles 409 ResourceConflictException payload
  ✓ PASS: 1.5: ApiClient handles 500 Internal Server Error payload
  ✓ PASS: 1.6: ApiClient handles HTML non-JSON error (e.g. 502 Bad Gateway) without crashing JSON parser
  ✓ PASS: 1.7: ApiClient handles empty response body gracefully
  ✓ PASS: 1.8: ApiClient handles network / connection failure gracefully
  ✓ PASS: 1.9: ApiClient download throws ApiError when server returns error status
  ✓ PASS: 1.10: ApiClient preserves AbortError as raw AbortError on cancellation

--- SUITE 2: AbortController Unmount Safety Across 6 Views ---
  ✓ PASS: 2.1: Dashboard - unmount aborts Promise.all without console.error or unhandled rejections
  ✓ PASS: 2.2: MisEstudiantes - unmount aborts professor/tutor load without error
  ✓ PASS: 2.3: GestionRoles - unmount aborts concurrent users and roles loading
  ✓ PASS: 2.4: GestionAsignaturas - unmount aborts subject loading cleanly
  ✓ PASS: 2.5: Portafolio - unmount aborts concurrent evidences and lesson plans
  ✓ PASS: 2.6: Asignaciones - unmount aborts Promise.allSettled with 6 concurrent requests cleanly
  ✓ PASS: 2.7: Perfil - unmount aborts authService.getProfile cleanly

--- SUITE 3: Adversarial Edge Cases & Stress Scenarios ---
  ✓ PASS: 3.1: Pre-aborted signal (already aborted when passed) handled gracefully
  ✓ PASS: 3.2: Rapid thrashing (50 consecutive mount-abort cycles) produces 0 unhandled rejections
  ✓ PASS: 3.3: Malformed JSON body in error response handled gracefully without crashing UI
  ✓ PASS: 3.4: Status 204 No Content returns null without json parsing error
  ✓ PASS: 3.5: File upload with FormData supports AbortController cancellation

====================================================
Final Results: 22 passed, 0 failed
Unhandled Rejections: 0
====================================================
```

---

## 2. Logic Chain

1. **AbortController Cancellation Mechanics & Safety (Adversarial Test 1)**:
   - *Observation*: In `Dashboard.jsx`, `MisEstudiantes.jsx`, `GestionRoles.jsx`, `GestionAsignaturas.jsx`, `Portafolio.jsx`, and `Asignaciones.jsx`, every `useEffect` instantiates an `AbortController`, forwards its `signal` into the domain service calls, and returns `() => controller.abort()`.
   - *Observation*: In `apiClient.js` (lines 115-117), when `fetch` rejects due to an abort signal, `catch (error)` explicitly isolates `if (error.name === 'AbortError') throw error;`, preserving the standard `DOMException: AbortError` rather than wrapping it in a generic `ApiError`.
   - *Observation*: In all 6 views, the catch block guards error reporting with `if (err.name !== 'AbortError')`.
   - *Deduction*: When a component unmounts while HTTP requests are in flight, the promise rejects with `AbortError`. Because the rejection is intercepted by the local `try-catch` inside the effect's async function and guarded against `err.name !== 'AbortError'`, no toast notification is fired, `console.error` is suppressed, and no unhandled promise rejection leaks to `window` or `process`.
   - *Empirical Proof*: Tests 2.1 through 2.7 and 3.2 subjected all 6 views to in-flight aborts, pre-aborted signals, and 50 rapid mount/unmount iterations. Zero unhandled rejections occurred (`unhandledRejections.length === 0`), and zero `console.error` calls were logged.

2. **Structured Error Contract & UI Resilience (Adversarial Test 2)**:
   - *Observation*: In `apiClient.js` (lines 78-98), when `!response.ok`, the response content-type is checked. If JSON, it extracts `errorData = await response.json()` and sets `errorMessage = errorData.message || errorData.error || errorMessage`.
   - *Observation*: An `ApiError` is constructed with `message`, `status`, and `data` (where `data` is the entire deserialized `ApiErrorResponse` containing `validationErrors`, `timestamp`, `error`, `path`).
   - *Deduction*: Even under adverse payloads (missing `message`, missing `error`, empty bodies, or malformed JSON), `apiClient.js` never throws an unhandled JSON parse exception. It falls back to HTTP status strings (`Error HTTP 400: Bad Request`) or raw response text.
   - *Empirical Proof*: Tests 1.1 through 1.10 and 3.3 verified that Spring Boot validation errors (`Map<String, String>`), 400, 404, 409, 500, HTML 502, network offline errors, and corrupt JSON strings all produce structured `ApiError` instances with clean string `.message` properties. UI call patterns (`toast.error(err.message)`) receive safe strings and do not crash.

3. **URL Centralization & Zero Leaks (Adversarial Test 3)**:
   - *Observation*: The entire `frontend/src/` contains 0 instances of `localhost:8080` outside the environment fallback in `apiClient.js:7`.
   - *Observation*: Exactly 1 `fetch()` call exists in the entire frontend codebase (`frontend/src/services/apiClient.js:74`).
   - *Deduction*: All network traffic without exception flows through `apiClient.js`. This guarantees that JWT authentication headers, query param serialization, error parsing, and base URL resolution are 100% centralized.

---

## 3. Adversarial Challenges & Stress Matrix

### Challenges

#### Challenge 1: `Promise.all` Abort Race Condition
- **Assumption Challenged**: In views like `Dashboard` and `GestionRoles`, multiple requests are dispatched concurrently via `Promise.all([p1, p2])`. When `controller.abort()` is called, both promises reject with `AbortError`. If one settles before the other, the second rejection could become an unhandled promise rejection if not properly tracked.
- **Attack Scenario**: Dispatch requests with asymmetric latency (10ms vs 100ms) and abort immediately.
- **Blast Radius**: Potential unhandled promise rejection crashing Node test runners or triggering error monitoring alerts (e.g. Sentry) in the browser.
- **Empirical Result**: **PASSED**. In ECMAScript specification, `Promise.all` attaches rejection handlers to all input promises synchronously at invocation time. When `p1` rejects, `Promise.all` settles, but `p2` already has an internal handler registered. The adversarial harness confirmed 0 unhandled rejections across 50 consecutive cycles.

#### Challenge 2: Non-JSON Response on Error (e.g. Nginx 502/504)
- **Assumption Challenged**: `apiClient.js` assumes JSON responses on error. If a gateway returns an HTML error page, calling `response.json()` would throw `SyntaxError: Unexpected token < in JSON at position 0`.
- **Attack Scenario**: Mock an HTTP 502 Bad Gateway response with `content-type: text/html` and an HTML body.
- **Blast Radius**: Unhandled syntax error masking the actual HTTP failure and crashing consumer `try-catch` blocks that expect `ApiError`.
- **Empirical Result**: **PASSED**. `apiClient.js` checks `if (contentType.includes('application/json'))` and falls back to `await response.text()`. Test 1.6 proved `ApiError` is cleanly constructed with status 502 and HTML body in `err.message`.

#### Challenge 3: Pre-aborted Signal
- **Assumption Challenged**: If a component mounts and is synchronously unmounted before the first microtask (e.g. rapid route switching or React StrictMode duplicate mounting), the signal is passed already in `aborted: true` state.
- **Attack Scenario**: Pass `signal` with `signal.aborted === true` directly into service calls.
- **Blast Radius**: Potential uncaught rejection if the service did not check signal status before starting.
- **Empirical Result**: **PASSED**. Both native `fetch` and `apiClient` immediately reject with `DOMException: AbortError`, caught cleanly by the view effect handler without executing network calls or logging errors.

### Stress Test Matrix

| # | Scenario | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|:---:|
| 1.1 | Spring Boot 400 with `validationErrors` | `ApiError` with status 400, message, and `data.validationErrors` | Populated `err.data.validationErrors` and string `err.message` | **PASS** |
| 1.2 | 400 with missing `message` field | Fallback to `error` string without throwing | `err.message === 'Validation Failed'` | **PASS** |
| 1.3 | 404 `ResourceNotFoundException` | `ApiError` status 404 with backend domain message | `err.message === 'Usuario con ID 99 no encontrado.'` | **PASS** |
| 1.4 | 409 `ResourceConflictException` | `ApiError` status 409 with conflict message | `err.message === 'El correo electrónico ya se encuentra registrado.'` | **PASS** |
| 1.5 | 500 `Internal Server Error` | `ApiError` status 500 with generic server message | `err.message === 'Ocurrió un error inesperado en el servidor.'` | **PASS** |
| 1.6 | HTML non-JSON error (Nginx 502) | Safe fallback to response text without JSON crash | `err.status === 502`, `err.message` contains HTML text | **PASS** |
| 1.7 | Empty response body (HTTP 403) | `Error HTTP 403: Forbidden` | `err.message === 'Error HTTP 403: Forbidden'` | **PASS** |
| 1.8 | Network failure (ECONNREFUSED) | `ApiError` status 0 with descriptive connection message | `err.status === 0`, connection message present | **PASS** |
| 1.9 | File download 404 error | `apiClient.download` throws `ApiError` | Throws `ApiError` with 404 status | **PASS** |
| 1.10 | Abort signal cancellation | Rethrows raw `AbortError`, not wrapped in `ApiError` | `err.name === 'AbortError'`, `!(err instanceof ApiError)` | **PASS** |
| 2.1 | Dashboard unmount during load | `Promise.all` aborts, 0 console errors, 0 unhandled rejections | Clean abort, 0 errors logged | **PASS** |
| 2.2 | MisEstudiantes unmount (3 roles) | Aborts professor/collaborator/tutor calls cleanly | Clean abort, `loading: false`, 0 errors logged | **PASS** |
| 2.3 | GestionRoles unmount | Aborts concurrent users & roles load | Clean abort, `loadingUsuarios: false`, 0 errors logged | **PASS** |
| 2.4 | GestionAsignaturas unmount | Aborts subject loading cleanly | Clean abort, `loadingData: false`, 0 errors logged | **PASS** |
| 2.5 | Portafolio unmount | Aborts concurrent evidences & plans load | Clean abort, both loadings reset, 0 errors logged | **PASS** |
| 2.6 | Asignaciones unmount (6 requests) | `Promise.allSettled` aborts all 6 requests cleanly | All 6 settle with rejected status, 0 errors logged | **PASS** |
| 2.7 | Perfil unmount | Aborts `authService.getProfile` cleanly | Clean abort, `initialLoading: false`, 0 errors logged | **PASS** |
| 3.1 | Pre-aborted signal | Immediate cancellation without console error | Clean abort, 0 errors logged | **PASS** |
| 3.2 | Rapid thrashing (50 mount-abort cycles) | Zero unhandled promise rejections | 0 unhandled rejections across 50 cycles | **PASS** |
| 3.3 | Malformed JSON in error response | Graceful fallback to HTTP status message | Handled cleanly, no JSON parse exception | **PASS** |
| 3.4 | Status 204 No Content | Returns `null` without parsing error | Returns `null` | **PASS** |
| 3.5 | File upload FormData abort | `apiClient.upload` aborts in-flight upload | Throws `AbortError` cleanly | **PASS** |

---

## 4. Caveats

- **Chatbot & WebSocket Streaming**: Real-time Chatbot assistance (`ChatbotLog`, `ChatbotMessage`) is currently stubbed in models and is slated for subsequent feature implementation. No streaming SSE/WebSocket connections exist yet in the frontend.
- **Playwright E2E**: Playwright is present in `devDependencies` without active configuration or test specs (deferred to Phase 4 Automated Testing per `PROJECT.md`).

---

## 5. Conclusion

Adversarial stress-testing and empirical verification of Phase 3 Frontend changes on branch `fabian` confirms:
1. **AbortController Unmount Safety**: 100% compliant across all 6 data-fetching views. Unmounting components cancels pending network requests cleanly without emitting console errors or unhandled promise rejections.
2. **ApiClient Error Parsing**: 100% compliant. RFC 7807 structured responses (including `validationErrors` maps and custom error messages), non-JSON payloads, network drops, and abort signals are handled robustly without crashing React UI rendering.
3. **Absence of Hardcoded URLs**: 100% compliant. Exactly 0 raw `localhost:8080` calls and 0 unintercepted `fetch()` calls remain in `frontend/src/pages/`.
4. **Build & Lint**: `oxlint` reports 0 errors; `vite build` succeeds in ~240ms with 0 errors.

**Explicit Gate Verdict**: **APPROVE**

---

## 6. Verification Method

To independently reproduce and verify this assessment:

1. **Verify absence of hardcoded URLs in `frontend/src/`**:
   ```bash
   grep -rn "localhost:8080" frontend/src/
   # Expected: Exactly 1 match (apiClient.js:7 default env fallback)
   
   grep -rn "fetch(" frontend/src/
   # Expected: Exactly 1 match (apiClient.js:74 centralized fetch call)
   ```

2. **Verify linter and build**:
   ```bash
   cmd.exe /c npm run lint --prefix frontend
   # Expected: 0 errors
   
   cmd.exe /c npm run build --prefix frontend
   # Expected: vite build exits with code 0 in ~250ms
   ```

3. **Verify backend compilation**:
   ```bash
   cmd.exe /c mvnw.cmd compile
   # Expected: BUILD SUCCESS
   ```
