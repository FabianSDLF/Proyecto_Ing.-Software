## 2026-09-23T19:30:42Z
You are Explorer 3 for Milestone 2 (Phase 2: Frontend Client Services, UI Components & AuthContext).

## 🔒 Your Identity & Working Directory
- Identity: teamwork_preview_explorer
- Working directory: c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m2_fe_3
- Parent Conversation ID: c3f760d9-58dc-4c34-b179-bc154eee62cf

## Scope & Objective
Investigate and design the complete frontend architecture refactoring for Phase 2:
1. `src/services/apiClient.js` (FE-01, FE-06, FE-23):
   - Centralized HTTP client wrapper around `fetch` or Axios.
   - Resolves `baseURL` from `import.meta.env.VITE_API_URL` (defaulting gracefully to `/api` or `http://localhost:8080`).
   - Automatically attaches `Authorization: Bearer <token>` from localStorage.
   - Standardizes response JSON parsing and throws structured errors on non-2xx responses.
2. Domain service modules in `src/services/`:
   - `authService.js` (login, forgotPassword, resetPassword, getProfile, updateProfile)
   - `portfolioService.js` (getDocuments, uploadEvidence, deleteDocument, downloadDocumentUrl, createLessonPlan, getLessonPlans, deleteLessonPlan)
   - `subjectService.js` (getSubjects, createSubject)
   - `assignmentService.js` (getAssignments, createAssignment, deleteAssignment, getByCollaborator, getByTutor)
   - `centerService.js` (getCenters, createCenter)
   - `userService.js` (getUsers, createUser, updateRole, updateStatus, uploadBatch)
3. Reusable UI components library in `src/components/ui/` (FE-02, FE-18, FE-19):
   - `Button.jsx`, `Input.jsx`, `Select.jsx`, `Modal.jsx` (accessible dialog with WAI-ARIA and Escape key support), `DataTable.jsx`, `ConfirmDialog.jsx` (replaces `window.confirm`), `Badge.jsx`, `Spinner.jsx`.
4. Global Auth State & Route Guards (FE-03, FE-04, FE-05):
   - `src/context/AuthContext.jsx` with `AuthProvider` and `useAuth` hook (replaces `window.location.reload()`).
   - `src/components/ProtectedRoute.jsx` (redirects unauthenticated users to `/`).
   - `src/components/RoleRoute.jsx` (verifies allowed roles and redirects unauthorized users).
   - Integration plan with `App.jsx`.

## Mandatory Files to Read
- ORIGINAL_REQUEST.md: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\ORIGINAL_REQUEST.md`
- Diagnostic report: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\reporte_codigo.md`
- `frontend/src/App.jsx`, `frontend/src/layouts/MainLayout.jsx`, `frontend/src/pages/`
