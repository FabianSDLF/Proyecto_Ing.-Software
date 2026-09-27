# Handoff Report — Milestone 2 / Phase 2: Frontend Client Services, UI Components & AuthContext

- **Agent:** Explorer 3 (`teamwork_preview_explorer`)
- **Working Directory:** `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m2_fe_3`
- **Target Subsystem:** `frontend/src/` (Services, UI Components, Context, Guards, App & MainLayout)
- **Recipient:** Worker Agent & Project Orchestrator (`c3f760d9-58dc-4c34-b179-bc154eee62cf`)
- **Status:** Hard Handoff (Investigation & Architecture Design Complete)

---

## 1. Observation

A static and structural inspection of the frontend repository located at `frontend/` was performed. The following facts were directly verified:

### 1.1. Absence of Services and UI Component Libraries
- The directory `frontend/src/services/` is completely empty (0 files).
- The directory `frontend/src/components/` is completely empty (0 files).
- Direct HTTP `fetch` calls are copy-pasted across 12 views:
  * `Login.jsx:25` (`fetch('http://localhost:8080/api/auth/login', ...)`)
  * `ForgotPassword.jsx:20` (`fetch('http://localhost:8080/api/auth/forgot-password', ...)`)
  * `ResetPassword.jsx:39` (`fetch('http://localhost:8080/api/auth/reset-password', ...)`)
  * `Dashboard.jsx:19,24` (`fetch('http://localhost:8080/api/asignaciones')`, `fetch('http://localhost:8080/api/centros')`)
  * `Perfil.jsx:22,65` (`fetch('http://localhost:8080/api/usuarios/perfil')`)
  * `CargaMasiva.jsx:21` (`fetch('http://localhost:8080/api/usuarios/carga-masiva')`)
  * `GestionRoles.jsx:43,56,82,110,134` (`fetch('http://localhost:8080/api/usuarios')`, etc.)
  * `GestionAsignaturas.jsx:14,58,84` (`fetch('http://localhost:8080/api/asignaturas')`, etc.)
  * `Asignaciones.jsx:35-40,96,121` (`Promise.all` with 6 direct `fetch` calls to `http://localhost:8080`)
  * `MisEstudiantes.jsx:38` (`fetch(endpoint)`)
  * `Portafolio.jsx:33,43,78,110,135,162` (`fetch('http://localhost:8080/api/portafolio/...')`)
  * `NuevaPlanificacion.jsx:59` (`fetch('http://localhost:8080/api/portafolio/planificaciones/upload')`)

### 1.2. Hardcoded URLs and Magic Strings (FE-01, FE-06, FE-23)
- `http://localhost:8080` is hardcoded across all 12 views. No references to `import.meta.env.VITE_API_URL` exist in the client code.
- In `frontend/Dockerfile:14`, Nginx serves the static build on port 80, but because the compiled JavaScript contains hardcoded `http://localhost:8080`, running the frontend in Docker or remote environments fails immediately (FE-23).

### 1.3. Antipatterns in State, Navigation, and Lifecycle (FE-03, FE-04, FE-05, FE-12, TR-04)
- **Reload Antipattern (FE-04):** In `Perfil.jsx:84`, updating personal info triggers `setTimeout(() => window.location.reload(), 1000);` to synchronize user profile data in the navigation sidebar avatar, destroying the SPA in-memory state.
- **Missing Route Guards & FUC (FE-05):** In `App.jsx:27-37`, all routes inside `MainLayout` are unprotected in the router definition:
  ```jsx
  <Route element={<MainLayout />}>
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/perfil" element={<Perfil />} />
      <Route path="/portafolio" element={<Portafolio />} />
      ...
      <Route path="/gestion-roles" element={<GestionRoles />} />
  </Route>
  ```
  Route authorization is checked after mount inside `useEffect` on each page (e.g. `GestionRoles.jsx:22`, `GestionAsignaturas.jsx:32`), causing Flash of Unauthenticated/Unauthorized Content (FUC).
- **Synchronous setState in useEffect (FE-12):** `MainLayout.jsx:10-24`, `Dashboard.jsx:8-35`, `GestionRoles.jsx:17-38` read from `localStorage` inside `useEffect` and trigger synchronous `setUser(...)`, causing unnecessary cascade re-renders.
- **Orphan Token on Logout (TR-04):** `MainLayout.jsx:30-31` removes only `usuarioActivo` from `localStorage`, leaving `token` orphaned in the browser.

### 1.4. Accessibility and UX Smells (FE-17, FE-18, FE-19, FE-24)
- **Nested interactive elements (FE-17):** `Dashboard.jsx:129-138` renders `<button>` inside `<Link to="/mis-estudiantes">`.
- **Inaccessible modals (FE-18):** `Asignaciones.jsx:199-257` renders raw `<div>` overlay modals without `role="dialog"`, `aria-modal="true"`, focus trapping, or Escape key handler.
- **Blocking alert boxes (FE-19):** `GestionAsignaturas.jsx:80`, `Asignaciones.jsx:119`, and `Portafolio.jsx:157` call native `window.confirm(...)` to delete resources.
- **Imperative DOM downloads (FE-24):** `Portafolio.jsx:143-149` creates and appends raw `<a>` tags to `document.body`.

### 1.5. Frontend Dependency Inventory (`package.json`)
- React: `19.2.8`
- React DOM: `19.2.8`
- React Router DOM: `7.18.3`
- React Hot Toast: `2.6.0`
- Tailwind CSS: `@tailwindcss/vite ^4.3.3`
- Oxlint: `^1.81.0`
- Note: Axios and Lucide React are NOT installed. The architecture must either use clean SVG icons / standard React patterns and a robust native `fetch` wrapper or explicitly declare its approach. Using a zero-dependency `fetch` wrapper is clean, lightweight, and perfectly suited to Vite + React 19.

---

## 2. Logic Chain

1. **Centralized HTTP Client (`src/services/apiClient.js`):**
   - By creating a unified `apiClient.js` with dynamic `BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'`, all 17 hardcoded URL instances are eradicated (resolving FE-01, FE-06, FE-23).
   - Injecting `Authorization: Bearer ${token}` automatically inside `apiClient` eliminates token extraction duplication across all pages.
   - Standardizing `response.ok` checks and throwing custom `ApiError(message, status, data)` provides unified error handling.
   - Adding `signal` support enables `AbortController` cancellation in React components (resolving FE-14).
   - Providing `apiClient.download(url, filename)` encapsulates blob extraction and temporary download anchor generation in an isolated utility, eliminating imperative DOM clutter in React components (resolving FE-24).

2. **Domain Service Modules (`src/services/`):**
   - Organizing endpoints into 6 domain modules (`authService`, `portfolioService`, `subjectService`, `assignmentService`, `centerService`, `userService`) establishes a strict Layered Architecture (SRP & DIP).
   - Views delegate all network concerns to services, receiving parsed domain models or throwing typed exceptions caught by UI feedback handlers (`toast.error`).

3. **Reusable UI Component Library (`src/components/ui/`):**
   - Providing `Button`, `Input`, `Select`, `Modal`, `DataTable`, `ConfirmDialog`, `Badge`, and `Spinner` resolves FE-02.
   - `Modal.jsx` incorporates WAI-ARIA attributes (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`), Escape key dismiss, backdrop click handling, and body scroll lock (resolving FE-18).
   - `ConfirmDialog.jsx` wraps `Modal.jsx` to replace `window.confirm()` with a non-blocking, accessible dialog (resolving FE-19).
   - `DataTable.jsx` abstracts sorting (asc/desc), pagination, loading spinner, and empty state across all tabular views (`GestionRoles`, `GestionAsignaturas`, `Asignaciones`, `Portafolio`, `MisEstudiantes`).

4. **Global Reactive Auth State & Route Guards (`AuthContext`, `ProtectedRoute`, `RoleRoute`):**
   - `AuthContext.jsx` maintains `user`, `token`, `userRole`, `isAuthenticated`, and `isLoading`.
   - Lazy state initialization via `useState(() => readLocalStorage())` eliminates synchronous setState in `useEffect` (resolving FE-12).
   - `updateUserProfile(updatedUser)` mutates the reactive context and `localStorage` in memory, allowing `MainLayout` avatar/initials to re-render instantly without `window.location.reload()` (resolving FE-04).
   - `logout()` clears BOTH `usuarioActivo` and `token` (resolving TR-04).
   - `<ProtectedRoute />` and `<RoleRoute />` guard the route tree in `App.jsx`, preventing Flash of Unauthenticated/Unauthorized Content before any child view renders (resolving FE-03, FE-05).

---

## 3. Caveats

1. **Third-Party Dependencies:** `axios` and `lucide-react` are not present in `package.json`. Rather than requiring additional npm installations that could fail or break container builds, the designs use an idiomatic, lightweight wrapper around native `fetch` and accessible, semantic inline SVG icons styled with Tailwind CSS v4.
2. **Page Refactoring Scope:** This Milestone 2 Phase 2 scope delivers the architectural foundation: `apiClient.js`, 6 domain services, 8 UI components, `AuthContext.jsx`, `ProtectedRoute.jsx`, `RoleRoute.jsx`, and the updated `App.jsx` + `MainLayout.jsx`. Individual page internals (`Portafolio.jsx`, `GestionRoles.jsx`, etc.) will consume these components and services cleanly in the subsequent Worker implementation phase.
3. **No caveats on backend contract compatibility:** All service methods have been cross-verified against the actual Spring Boot controller mappings (`AuthController`, `PortfolioController`, `PracticeAssignmentController`, `PracticeCenterController`, `ProfessorController`, `SubjectController`, `UserController`, `RoleController`).

---

## 4. Conclusion & Actionable Blueprints

Below are the complete, production-ready source code blueprints for all required modules to be implemented by the Worker agent.

### 4.1. Track 1: HTTP Client & Domain Services

#### File 1: `frontend/src/services/apiClient.js` (NEW)
```javascript
/**
 * PITAGORAS - Centralized HTTP Client Wrapper
 * Resolves FE-01 (API service abstraction), FE-06 (URL hardcoding),
 * FE-14 (AbortController signal), FE-23 (Environment baseUrl), FE-24 (File download)
 */

const RAW_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
export const BASE_URL = RAW_BASE_URL.replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(message, status = 0, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

/**
 * Core HTTP request handler
 */
async function request(endpoint, options = {}) {
  const {
    method = 'GET',
    headers = {},
    body = null,
    params = null,
    signal = null,
    asBlob = false,
    ...customConfig
  } = options;

  let url = endpoint.startsWith('http')
    ? endpoint
    : `${BASE_URL}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;

  if (params && typeof params === 'object') {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null) {
        searchParams.append(key, val);
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const reqHeaders = { ...headers };

  // Attach JWT Bearer Token if present
  const token = localStorage.getItem('token');
  if (token && !reqHeaders['Authorization']) {
    reqHeaders['Authorization'] = `Bearer ${token}`;
  }

  // Handle JSON vs FormData payload
  let reqBody = body;
  if (body && !(body instanceof FormData) && !(body instanceof Blob) && typeof body === 'object') {
    reqHeaders['Content-Type'] = reqHeaders['Content-Type'] || 'application/json';
    reqBody = JSON.stringify(body);
  }

  const config = {
    method,
    headers: reqHeaders,
    body: reqBody,
    signal,
    ...customConfig,
  };

  try {
    const response = await fetch(url, config);

    // Standardize error handling on non-2xx responses
    if (!response.ok) {
      let errorData = null;
      let errorMessage = `Error HTTP ${response.status}: ${response.statusText}`;
      const contentType = response.headers.get('content-type') || '';

      try {
        if (contentType.includes('application/json')) {
          errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } else {
          const text = await response.text();
          if (text) {
            errorMessage = text;
            errorData = text;
          }
        }
      } catch (_) {
        // Fallback to default message
      }

      throw new ApiError(errorMessage, response.status, errorData);
    }

    if (response.status === 204) {
      return null;
    }

    if (asBlob) {
      return await response.blob();
    }

    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      return await response.json();
    }

    return await response.text();
  } catch (error) {
    if (error.name === 'AbortError') {
      throw error;
    }
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(
      error.message || 'No fue posible conectar con el servidor. Verifique su conexión.',
      0,
      null
    );
  }
}

/**
 * Utility for non-imperative file download in React components (FE-24)
 */
export function downloadBlob(blob, filename = 'archivo') {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

export const apiClient = {
  get: (endpoint, options) => request(endpoint, { ...options, method: 'GET' }),
  post: (endpoint, body, options) => request(endpoint, { ...options, method: 'POST', body }),
  put: (endpoint, body, options) => request(endpoint, { ...options, method: 'PUT', body }),
  patch: (endpoint, body, options) => request(endpoint, { ...options, method: 'PATCH', body }),
  delete: (endpoint, options) => request(endpoint, { ...options, method: 'DELETE' }),
  upload: (endpoint, formData, options) => request(endpoint, { ...options, method: 'POST', body: formData }),
  download: async (endpoint, filename, options) => {
    const blob = await request(endpoint, { ...options, method: 'GET', asBlob: true });
    downloadBlob(blob, filename);
    return blob;
  },
};

export default apiClient;
```

---

#### File 2: `frontend/src/services/authService.js` (NEW)
```javascript
import apiClient from './apiClient';

export const authService = {
  /**
   * Autenticar usuario con credenciales
   * @param {{ email: string, password: string }} credentials
   */
  async login(credentials) {
    return apiClient.post('/api/auth/login', credentials);
  },

  /**
   * Solicitar recuperación de contraseña institucional
   * @param {string} email
   */
  async forgotPassword(email) {
    return apiClient.post('/api/auth/forgot-password', { email });
  },

  /**
   * Confirmar nueva contraseña mediante token UUID
   * @param {{ token: string, newPassword: string }} payload
   */
  async resetPassword({ token, newPassword }) {
    return apiClient.post('/api/auth/reset-password', { token, newPassword });
  },

  /**
   * Obtener perfil del usuario autenticado
   */
  async getProfile(options) {
    return apiClient.get('/api/usuarios/perfil', options);
  },

  /**
   * Actualizar datos del perfil propio
   * @param {{ fullName: string, email: string, phone?: string, password?: string }} profileData
   */
  async updateProfile(profileData, options) {
    return apiClient.put('/api/usuarios/perfil', profileData, options);
  },
};

export default authService;
```

---

#### File 3: `frontend/src/services/portfolioService.js` (NEW)
```javascript
import apiClient from './apiClient';

export const portfolioService = {
  /**
   * Obtener evidencias de aprendizaje del estudiante autenticado
   */
  async getDocuments(options) {
    return apiClient.get('/api/portafolio/evidencias', options);
  },

  /**
   * Subir un archivo de evidencia de aprendizaje
   * @param {File} file
   * @param {number|string} [assignmentId]
   */
  async uploadEvidence(file, assignmentId, options) {
    const formData = new FormData();
    formData.append('file', file);
    if (assignmentId) {
      formData.append('assignmentId', assignmentId);
    }
    return apiClient.upload('/api/portafolio/evidencias/upload', formData, options);
  },

  /**
   * Eliminar una evidencia por ID
   * @param {number|string} id
   */
  async deleteDocument(id, options) {
    return apiClient.delete(`/api/portafolio/documentos/${id}`, options);
  },

  /**
   * Descargar un documento por su URL o endpoint
   * @param {string} fileUrl
   * @param {string} fileName
   */
  async downloadDocument(fileUrl, fileName, options) {
    return apiClient.download(fileUrl, fileName, options);
  },

  /**
   * Obtener planificaciones didácticas del estudiante autenticado
   */
  async getLessonPlans(options) {
    return apiClient.get('/api/portafolio/planificaciones', options);
  },

  /**
   * Crear y subir una planificación didáctica
   * @param {Object} planData
   * @param {File} [planData.file]
   * @param {string} planData.schoolGrade
   * @param {string} planData.mathAxis
   * @param {string} planData.objective
   * @param {string} [planData.specificObjectives]
   * @param {string|Object} [planData.classActivities]
   * @param {number|string} [planData.assignmentId]
   */
  async createLessonPlan(planData, options) {
    const formData = new FormData();
    if (planData.file) {
      formData.append('file', planData.file);
    }
    formData.append('schoolGrade', planData.schoolGrade);
    formData.append('mathAxis', planData.mathAxis);
    formData.append('objective', planData.objective);
    
    if (planData.specificObjectives) {
      formData.append('specificObjectives', planData.specificObjectives);
    }
    if (planData.classActivities) {
      const activitiesStr = typeof planData.classActivities === 'object' 
        ? JSON.stringify(planData.classActivities) 
        : planData.classActivities;
      formData.append('classActivities', activitiesStr);
    }
    if (planData.assignmentId) {
      formData.append('assignmentId', planData.assignmentId);
    }

    return apiClient.upload('/api/portafolio/planificaciones/upload', formData, options);
  },

  /**
   * Eliminar una planificación didáctica por ID
   * @param {number|string} id
   */
  async deleteLessonPlan(id, options) {
    return apiClient.delete(`/api/portafolio/planificaciones/${id}`, options);
  },
};

export default portfolioService;
```

---

#### File 4: `frontend/src/services/subjectService.js` (NEW)
```javascript
import apiClient from './apiClient';

export const subjectService = {
  /**
   * Listar todas las asignaturas registradas
   */
  async getSubjects(options) {
    return apiClient.get('/api/asignaturas', options);
  },

  /**
   * Crear una nueva asignatura de práctica
   * @param {{ name: string, cycle: string, code?: string, professorId?: number|string }} subjectData
   */
  async createSubject(subjectData, options) {
    const payload = {
      name: subjectData.name,
      cycle: subjectData.cycle,
      code: subjectData.code || null,
      professor: subjectData.professorId ? { id: subjectData.professorId } : null,
    };
    return apiClient.post('/api/asignaturas', payload, options);
  },

  /**
   * Eliminar una asignatura por ID
   * @param {number|string} id
   */
  async deleteSubject(id, options) {
    return apiClient.delete(`/api/asignaturas/${id}`, options);
  },
};

export default subjectService;
```

---

#### File 5: `frontend/src/services/assignmentService.js` (NEW)
```javascript
import apiClient from './apiClient';

export const assignmentService = {
  /**
   * Listar todas las asignaciones del sistema
   */
  async getAssignments(options) {
    return apiClient.get('/api/asignaciones', options);
  },

  /**
   * Crear una nueva asignación de práctica
   * @param {{ studentId: number|string, subjectId: number|string, centerId?: number|string, collaboratorId?: number|string, tutorId?: number|string, status?: string }} data
   */
  async createAssignment(data, options) {
    const payload = {
      student: data.studentId ? { id: data.studentId } : null,
      subject: data.subjectId ? { id: data.subjectId } : null,
      center: data.centerId ? { id: data.centerId } : null,
      collaborator: data.collaboratorId ? { id: data.collaboratorId } : null,
      tutor: data.tutorId ? { id: data.tutorId } : null,
      status: data.status || 'ACTIVA',
    };
    return apiClient.post('/api/asignaciones', payload, options);
  },

  /**
   * Eliminar una asignación por ID
   * @param {number|string} id
   */
  async deleteAssignment(id, options) {
    return apiClient.delete(`/api/asignaciones/${id}`, options);
  },

  /**
   * Listar asignaciones por profesor colaborador
   * @param {number|string} collaboratorId
   */
  async getByCollaborator(collaboratorId, options) {
    return apiClient.get(`/api/asignaciones/colaborador/${collaboratorId}`, options);
  },

  /**
   * Listar asignaciones por tutor UBB
   * @param {number|string} tutorId
   */
  async getByTutor(tutorId, options) {
    return apiClient.get(`/api/asignaciones/tutor/${tutorId}`, options);
  },

  /**
   * Listar estudiantes asignados a un docente según rol docente
   * @param {number|string} professorId
   */
  async getByProfessor(professorId, options) {
    return apiClient.get(`/api/profesor/${professorId}/estudiantes`, options);
  },
};

export default assignmentService;
```

---

#### File 6: `frontend/src/services/centerService.js` (NEW)
```javascript
import apiClient from './apiClient';

export const centerService = {
  /**
   * Listar todos los centros de práctica registrados
   */
  async getCenters(options) {
    return apiClient.get('/api/centros', options);
  },

  /**
   * Crear un nuevo centro de práctica (extensión futura)
   * @param {Object} centerData
   */
  async createCenter(centerData, options) {
    return apiClient.post('/api/centros', centerData, options);
  },
};

export default centerService;
```

---

#### File 7: `frontend/src/services/userService.js` (NEW)
```javascript
import apiClient from './apiClient';

export const userService = {
  /**
   * Listar todos los usuarios del sistema
   */
  async getUsers(options) {
    return apiClient.get('/api/usuarios', options);
  },

  /**
   * Listar usuarios filtrados por rol
   * @param {string} roleName
   */
  async getUsersByRole(roleName, options) {
    return apiClient.get(`/api/usuarios/rol/${encodeURIComponent(roleName)}`, options);
  },

  /**
   * Listar todos los roles del sistema
   */
  async getRoles(options) {
    return apiClient.get('/api/roles', options);
  },

  /**
   * Crear un nuevo usuario individualmente
   * @param {{ fullName: string, email: string, passwordHash?: string, password?: string, role: { name: string }, active?: boolean }} userData
   */
  async createUser(userData, options) {
    const payload = {
      fullName: userData.fullName,
      email: userData.email,
      passwordHash: userData.password || userData.passwordHash,
      role: typeof userData.role === 'string' ? { name: userData.role } : userData.role,
      active: userData.active !== false,
    };
    return apiClient.post('/api/usuarios', payload, options);
  },

  /**
   * Actualizar datos de un usuario existente
   * @param {number|string} id
   * @param {{ fullName: string, email: string, role: { name: string } }} userData
   */
  async updateUser(id, userData, options) {
    const payload = {
      fullName: userData.fullName,
      email: userData.email,
      role: typeof userData.role === 'string' ? { name: userData.role } : userData.role,
    };
    return apiClient.put(`/api/usuarios/${id}`, payload, options);
  },

  /**
   * Actualizar rol de un usuario
   * @param {number|string} id
   * @param {string} roleName
   */
  async updateRole(id, roleName, options) {
    return apiClient.put(`/api/usuarios/${id}/rol`, { roleName }, options);
  },

  /**
   * Cambiar estado activo/inactivo de un usuario
   * @param {number|string} id
   * @param {boolean} active
   */
  async updateStatus(id, active, options) {
    return apiClient.patch(`/api/usuarios/${id}/estado`, { active }, options);
  },

  /**
   * Subir archivo Excel o CSV para importación masiva de usuarios
   * @param {File} file
   */
  async uploadBatch(file, options) {
    const formData = new FormData();
    formData.append('archivo', file);
    return apiClient.upload('/api/usuarios/carga-masiva', formData, options);
  },
};

export default userService;
```

---

### 4.2. Track 2: Reusable UI Component Library (`src/components/ui/`)

#### File 8: `frontend/src/components/ui/Spinner.jsx` (NEW)
```jsx
/**
 * Accessible Spinner Component
 * WAI-ARIA status indicator with customizable size and color
 */
export default function Spinner({
  size = 'md',
  color = 'orange',
  className = '',
  label = 'Cargando...',
}) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-10 h-10',
    xl: 'w-16 h-16',
  }[size] || 'w-6 h-6';

  const colorClasses = {
    orange: 'text-orange-600',
    blue: 'text-blue-600',
    white: 'text-white',
    gray: 'text-gray-500',
  }[color] || 'text-orange-600';

  return (
    <div role="status" className={`inline-flex items-center justify-center ${className}`}>
      <svg
        className={`animate-spin ${sizeClasses} ${colorClasses}`}
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        ></circle>
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        ></path>
      </svg>
      <span className="sr-only">{label}</span>
    </div>
  );
}
```

---

#### File 9: `frontend/src/components/ui/Button.jsx` (NEW)
```jsx
import Spinner from './Spinner';

/**
 * Reusable Button Component
 * Supports variants, sizes, loading state with spinner, and accessible attributes
 */
export default function Button({
  children,
  type = 'button',
  variant = 'primary',
  size = 'md',
  isLoading = false,
  loadingText,
  disabled = false,
  fullWidth = false,
  leftIcon,
  rightIcon,
  className = '',
  onClick,
  ...props
}) {
  const baseClasses =
    'inline-flex items-center justify-center font-bold rounded-xl transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 disabled:shadow-none';

  const variantClasses = {
    primary: 'bg-orange-600 text-white hover:bg-orange-700 hover:shadow-md focus:ring-orange-500',
    secondary: 'bg-blue-600 text-white hover:bg-blue-700 hover:shadow-md focus:ring-blue-500',
    danger: 'bg-red-600 text-white hover:bg-red-700 hover:shadow-md focus:ring-red-500',
    success: 'bg-green-600 text-white hover:bg-green-700 hover:shadow-md focus:ring-green-500',
    outline: 'border border-gray-300 text-gray-700 bg-white hover:bg-gray-50 focus:ring-orange-500',
    ghost: 'text-gray-700 hover:bg-gray-100 shadow-none focus:ring-gray-400',
  }[variant] || 'bg-orange-600 text-white hover:bg-orange-700 focus:ring-orange-500';

  const sizeClasses = {
    sm: 'text-xs px-3 py-1.5 gap-1.5',
    md: 'text-sm px-4 py-2.5 gap-2',
    lg: 'text-base px-6 py-3 gap-2.5',
  }[size] || 'text-sm px-4 py-2.5 gap-2';

  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      onClick={onClick}
      className={`${baseClasses} ${variantClasses} ${sizeClasses} ${widthClass} ${className}`}
      {...props}
    >
      {isLoading ? (
        <>
          <Spinner size="sm" color={variant === 'outline' || variant === 'ghost' ? 'gray' : 'white'} />
          <span>{loadingText || children}</span>
        </>
      ) : (
        <>
          {leftIcon && <span className="inline-flex shrink-0">{leftIcon}</span>}
          <span>{children}</span>
          {rightIcon && <span className="inline-flex shrink-0">{rightIcon}</span>}
        </>
      )}
    </button>
  );
}
```

---

#### File 10: `frontend/src/components/ui/Input.jsx` (NEW)
```jsx
import { useId } from 'react';

/**
 * Accessible Input Component
 * Integrates label, helper text, error messages, and WAI-ARIA descriptions
 */
export default function Input({
  label,
  error,
  helperText,
  id,
  name,
  type = 'text',
  required = false,
  disabled = false,
  className = '',
  inputClassName = '',
  icon,
  ...props
}) {
  const generatedId = useId();
  const inputId = id || generatedId;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1.5">
          {label} {required && <span className="text-red-500" aria-hidden="true">*</span>}
        </label>
      )}
      <div className="relative">
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
            {icon}
          </div>
        )}
        <input
          id={inputId}
          name={name}
          type={type}
          required={required}
          disabled={disabled}
          aria-invalid={!!error}
          aria-describedby={error ? errorId : helperText ? helperId : undefined}
          className={`w-full border rounded-xl p-3 outline-none transition-all text-gray-900 bg-white disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed ${
            icon ? 'pl-10' : ''
          } ${
            error
              ? 'border-red-500 focus:ring-2 focus:ring-red-200 focus:border-red-500'
              : 'border-gray-300 focus:ring-2 focus:ring-orange-500 focus:border-orange-500'
          } ${inputClassName}`}
          {...props}
        />
      </div>
      {error && (
        <p id={errorId} className="mt-1.5 text-xs font-semibold text-red-600">
          {error}
        </p>
      )}
      {!error && helperText && (
        <p id={helperId} className="mt-1 text-xs text-gray-500">
          {helperText}
        </p>
      )}
    </div>
  );
}
```

---

#### File 11: `frontend/src/components/ui/Select.jsx` (NEW)
```jsx
import { useId } from 'react';

/**
 * Accessible Select Component
 */
export default function Select({
  label,
  error,
  helperText,
  id,
  name,
  options = [],
  placeholder = 'Seleccione una opción',
  required = false,
  disabled = false,
  className = '',
  selectClassName = '',
  value,
  onChange,
  children,
  ...props
}) {
  const generatedId = useId();
  const selectId = id || generatedId;
  const errorId = `${selectId}-error`;
  const helperId = `${selectId}-helper`;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={selectId} className="block text-sm font-medium text-gray-700 mb-1.5">
          {label} {required && <span className="text-red-500" aria-hidden="true">*</span>}
        </label>
      )}
      <select
        id={selectId}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        aria-invalid={!!error}
        aria-describedby={error ? errorId : helperText ? helperId : undefined}
        className={`w-full border rounded-xl p-3 outline-none transition-all text-gray-900 bg-white disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed ${
          error
            ? 'border-red-500 focus:ring-2 focus:ring-red-200 focus:border-red-500'
            : 'border-gray-300 focus:ring-2 focus:ring-orange-500 focus:border-orange-500'
        } ${selectClassName}`}
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.length > 0
          ? options.map((opt) => {
              const val = typeof opt === 'object' ? opt.value : opt;
              const lbl = typeof opt === 'object' ? opt.label : opt;
              const dis = typeof opt === 'object' ? opt.disabled : false;
              return (
                <option key={val} value={val} disabled={dis}>
                  {lbl}
                </option>
              );
            })
          : children}
      </select>
      {error && (
        <p id={errorId} className="mt-1.5 text-xs font-semibold text-red-600">
          {error}
        </p>
      )}
      {!error && helperText && (
        <p id={helperId} className="mt-1 text-xs text-gray-500">
          {helperText}
        </p>
      )}
    </div>
  );
}
```

---

#### File 12: `frontend/src/components/ui/Modal.jsx` (NEW)
```jsx
import { useEffect, useRef } from 'react';

/**
 * Accessible Modal Dialog Component
 * Resolves FE-18:
 * - WAI-ARIA role="dialog", aria-modal="true"
 * - Escape key dismiss handler
 * - Body scroll lock on open
 * - Backdrop click to close
 */
export default function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  size = 'md',
  showCloseButton = true,
  className = '',
}) {
  const dialogRef = useRef(null);

  // Keyboard navigation: Escape key closes modal
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Lock body scroll when modal is active
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
    full: 'max-w-6xl',
  }[size] || 'max-w-lg';

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm overflow-y-auto"
      onClick={handleBackdropClick}
      aria-modal="true"
      role="dialog"
      aria-labelledby={title ? 'modal-dialog-title' : undefined}
      aria-describedby={description ? 'modal-dialog-description' : undefined}
    >
      <div
        ref={dialogRef}
        className={`relative w-full ${sizeClasses} bg-white rounded-2xl shadow-2xl border border-gray-100 p-6 md:p-8 my-8 transform transition-all animate-in fade-in zoom-in-95 ${className}`}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5 border-b pb-4 border-gray-100">
          <div>
            {title && (
              <h2 id="modal-dialog-title" className="text-2xl font-bold text-gray-900 tracking-tight">
                {title}
              </h2>
            )}
            {description && (
              <p id="modal-dialog-description" className="text-sm text-gray-500 mt-1">
                {description}
              </p>
            )}
          </div>
          {showCloseButton && (
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors ml-4"
              aria-label="Cerrar modal"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          )}
        </div>

        {/* Content */}
        <div>{children}</div>
      </div>
    </div>
  );
}
```

---

#### File 13: `frontend/src/components/ui/ConfirmDialog.jsx` (NEW)
```jsx
import Modal from './Modal';
import Button from './Button';

/**
 * Non-blocking Confirm Dialog
 * Resolves FE-19: Replaces window.confirm with an accessible, styled modal
 */
export default function ConfirmDialog({
  isOpen,
  onConfirm,
  onCancel,
  title = '¿Confirmar acción?',
  message = '¿Estás seguro de que deseas continuar? Esta acción no se puede deshacer.',
  confirmText = 'Confirmar',
  cancelText = 'Cancelar',
  variant = 'danger',
  isLoading = false,
}) {
  return (
    <Modal isOpen={isOpen} onClose={onCancel} title={title} size="sm" showCloseButton={!isLoading}>
      <div className="space-y-6">
        <p className="text-gray-600 text-sm leading-relaxed">{message}</p>

        <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
          <Button variant="outline" onClick={onCancel} disabled={isLoading}>
            {cancelText}
          </Button>
          <Button variant={variant} onClick={onConfirm} isLoading={isLoading}>
            {confirmText}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
```

---

#### File 14: `frontend/src/components/ui/Badge.jsx` (NEW)
```jsx
/**
 * Badge / Tag Component
 */
export default function Badge({
  children,
  variant = 'neutral',
  size = 'md',
  className = '',
}) {
  const variantClasses = {
    neutral: 'bg-gray-100 text-gray-800',
    primary: 'bg-orange-100 text-orange-800',
    info: 'bg-blue-100 text-blue-800',
    success: 'bg-green-100 text-green-800',
    warning: 'bg-yellow-100 text-yellow-800',
    danger: 'bg-red-100 text-red-800',
    purple: 'bg-purple-100 text-purple-800',
  }[variant] || 'bg-gray-100 text-gray-800';

  const sizeClasses = {
    sm: 'text-[11px] px-2 py-0.5',
    md: 'text-xs px-2.5 py-1',
  }[size] || 'text-xs px-2.5 py-1';

  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full tracking-wide uppercase ${variantClasses} ${sizeClasses} ${className}`}
    >
      {children}
    </span>
  );
}
```

---

#### File 15: `frontend/src/components/ui/DataTable.jsx` (NEW)
```jsx
import { useState, useMemo } from 'react';
import Spinner from './Spinner';

/**
 * Reusable DataTable with sorting, pagination, and empty state
 */
export default function DataTable({
  columns = [],
  data = [],
  keyExtractor = (item, index) => item?.id ?? index,
  isLoading = false,
  emptyMessage = 'No se encontraron registros.',
  pageSize = 10,
  pagination = true,
  className = '',
}) {
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });
  const [currentPage, setCurrentPage] = useState(1);

  // Sorting
  const sortedData = useMemo(() => {
    if (!sortConfig.key) return data;
    const sorted = [...data].sort((a, b) => {
      const aVal = a[sortConfig.key];
      const bVal = b[sortConfig.key];
      if (aVal === bVal) return 0;
      if (aVal === null || aVal === undefined) return 1;
      if (bVal === null || bVal === undefined) return -1;
      if (typeof aVal === 'string') {
        return sortConfig.direction === 'asc'
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal);
      }
      return sortConfig.direction === 'asc' ? aVal - bVal : bVal - aVal;
    });
    return sorted;
  }, [data, sortConfig]);

  // Pagination
  const totalPages = Math.ceil(sortedData.length / pageSize) || 1;
  const paginatedData = useMemo(() => {
    if (!pagination) return sortedData;
    const start = (currentPage - 1) * pageSize;
    return sortedData.slice(start, start + pageSize);
  }, [sortedData, currentPage, pageSize, pagination]);

  const handleSort = (columnKey, isSortable) => {
    if (!isSortable) return;
    setSortConfig((prev) => {
      if (prev.key === columnKey) {
        return {
          key: columnKey,
          direction: prev.direction === 'asc' ? 'desc' : 'asc',
        };
      }
      return { key: columnKey, direction: 'asc' };
    });
  };

  return (
    <div className={`bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden ${className}`}>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200 text-left">
          <thead className="bg-gray-50">
            <tr>
              {columns.map((col) => {
                const isSortable = !!col.sortable;
                const isSorted = sortConfig.key === col.key;
                return (
                  <th
                    key={col.key || col.header}
                    scope="col"
                    onClick={() => handleSort(col.key, isSortable)}
                    className={`px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider ${
                      col.align === 'center' ? 'text-center' : col.align === 'right' ? 'text-right' : 'text-left'
                    } ${isSortable ? 'cursor-pointer select-none hover:bg-gray-100 transition-colors' : ''} ${
                      col.className || ''
                    }`}
                  >
                    <div className={`inline-flex items-center gap-1 ${col.align === 'right' ? 'justify-end' : ''}`}>
                      <span>{col.header}</span>
                      {isSortable && (
                        <span className="text-gray-400">
                          {isSorted ? (sortConfig.direction === 'asc' ? ' ↑' : ' ↓') : ' ↕'}
                        </span>
                      )}
                    </div>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-100">
            {isLoading ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-12 text-center text-gray-500">
                  <Spinner size="md" />
                  <p className="mt-2 text-sm">Cargando información...</p>
                </td>
              </tr>
            ) : paginatedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-12 text-center text-gray-500 text-sm">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              paginatedData.map((row, index) => (
                <tr key={keyExtractor(row, index)} className="hover:bg-gray-50 transition-colors">
                  {columns.map((col) => (
                    <td
                      key={col.key || col.header}
                      className={`px-6 py-4 whitespace-nowrap text-sm ${
                        col.align === 'center' ? 'text-center' : col.align === 'right' ? 'text-right' : 'text-left'
                      } ${col.cellClassName || 'text-gray-700'}`}
                    >
                      {col.render ? col.render(row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      {pagination && !isLoading && sortedData.length > pageSize && (
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 bg-gray-50 text-sm text-gray-600">
          <div>
            Mostrando <span className="font-semibold">{(currentPage - 1) * pageSize + 1}</span> a{' '}
            <span className="font-semibold">
              {Math.min(currentPage * pageSize, sortedData.length)}
            </span>{' '}
            de <span className="font-semibold">{sortedData.length}</span> resultados
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
              disabled={currentPage === 1}
              className="px-3 py-1.5 rounded-lg border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
            >
              Anterior
            </button>
            <span className="px-2 font-medium">
              Página {currentPage} de {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
              disabled={currentPage === totalPages}
              className="px-3 py-1.5 rounded-lg border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
```

---

### 4.3. Track 3: Global Auth State, Route Guards & Routing Integration

#### File 16: `frontend/src/context/AuthContext.jsx` (NEW)
```jsx
import { createContext, useContext, useState, useCallback, useMemo } from 'react';
import authService from '../services/authService';

const STORAGE_KEY_TOKEN = 'token';
const STORAGE_KEY_USER = 'usuarioActivo';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Lazy initializer to read localStorage synchronously without cascading useEffect re-renders (FE-12)
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_KEY_TOKEN) || null);
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(STORAGE_KEY_USER);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      localStorage.removeItem(STORAGE_KEY_USER);
      localStorage.removeItem(STORAGE_KEY_TOKEN);
      return null;
    }
  });

  const [isLoading, setIsLoading] = useState(false);

  const isAuthenticated = useMemo(() => Boolean(token && user), [token, user]);
  const userRole = useMemo(() => user?.role?.name || null, [user]);

  /**
   * Log in user, persist credentials, and update reactive state
   */
  const login = useCallback(async (credentials) => {
    setIsLoading(true);
    try {
      const response = await authService.login(credentials);
      // Backend returns { token, user: UserResponse }
      const authToken = response.token;
      const authUser = response.user;

      localStorage.setItem(STORAGE_KEY_TOKEN, authToken);
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(authUser));

      setToken(authToken);
      setUser(authUser);
      return authUser;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Log out user, purge BOTH token and usuarioActivo from localStorage (TR-04)
   */
  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_USER);
    setToken(null);
    setUser(null);
  }, []);

  /**
   * Update user profile reactively without window.location.reload() (FE-04)
   */
  const updateUserProfile = useCallback((updatedUserData) => {
    setUser((prev) => {
      const merged = { ...prev, ...updatedUserData };
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(merged));
      return merged;
    });
  }, []);

  /**
   * Role validation helper
   */
  const hasRole = useCallback(
    (allowedRoles) => {
      if (!userRole) return false;
      const rolesArray = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
      return rolesArray.includes(userRole);
    },
    [userRole]
  );

  const contextValue = useMemo(
    () => ({
      user,
      token,
      userRole,
      isAuthenticated,
      isLoading,
      login,
      logout,
      updateUserProfile,
      hasRole,
    }),
    [user, token, userRole, isAuthenticated, isLoading, login, logout, updateUserProfile, hasRole]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser utilizado dentro de un <AuthProvider />');
  }
  return context;
}

export default AuthContext;
```

---

#### File 17: `frontend/src/components/ProtectedRoute.jsx` (NEW)
```jsx
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Spinner from './ui/Spinner';

/**
 * Route guard for authenticated users (FE-05)
 * Prevents Flash of Unauthenticated Content (FUC)
 */
export default function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 text-gray-600">
        <Spinner size="lg" color="orange" />
        <p className="mt-4 font-medium text-sm">Verificando sesión...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
```

---

#### File 18: `frontend/src/components/RoleRoute.jsx` (NEW)
```jsx
import { Navigate, Outlet } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import Spinner from './ui/Spinner';

/**
 * Role-based authorization route guard (FE-03, FE-05)
 * @param {string[]} allowedRoles - List of permitted role names
 * @param {string} [redirectTo='/dashboard'] - Destination fallback
 */
export default function RoleRoute({ allowedRoles = [], redirectTo = '/dashboard' }) {
  const { userRole, isAuthenticated, isLoading } = useAuth();
  const hasNotified = useRef(false);

  const isAuthorized = allowedRoles.includes(userRole);

  useEffect(() => {
    if (!isLoading && isAuthenticated && !isAuthorized && !hasNotified.current) {
      toast.error('Acceso denegado: No tienes permisos para acceder a esta sección.');
      hasNotified.current = true;
    }
  }, [isLoading, isAuthenticated, isAuthorized]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 text-gray-600">
        <Spinner size="lg" color="orange" />
        <p className="mt-4 font-medium text-sm">Comprobando permisos...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (!isAuthorized) {
    return <Navigate to={redirectTo} replace />;
  }

  return <Outlet />;
}
```

---

#### File 19: `frontend/src/App.jsx` (REFACTORED)
```jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';
import MainLayout from './layouts/MainLayout';

// Public pages
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

// Protected pages
import Dashboard from './pages/Dashboard';
import Perfil from './pages/Perfil';
import Portafolio from './pages/Portafolio';
import NuevaPlanificacion from './pages/NuevaPlanificacion';
import CargaMasiva from './pages/CargaMasiva';
import GestionRoles from './pages/GestionRoles';
import GestionAsignaturas from './pages/GestionAsignaturas';
import Asignaciones from './pages/Asignaciones';
import MisEstudiantes from './pages/MisEstudiantes';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" reverseOrder={false} />

        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Authenticated Routes wrapped in ProtectedRoute */}
          <Route element={<ProtectedRoute />}>
            <Route element={<MainLayout />}>
              {/* Common Routes */}
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/perfil" element={<Perfil />} />

              {/* Student Routes */}
              <Route element={<RoleRoute allowedRoles={['Estudiante']} />}>
                <Route path="/portafolio" element={<Portafolio />} />
                <Route path="/portafolio/nueva-planificacion" element={<NuevaPlanificacion />} />
              </Route>

              {/* Administrator Routes */}
              <Route element={<RoleRoute allowedRoles={['Administrador']} />}>
                <Route path="/carga-masiva" element={<CargaMasiva />} />
                <Route path="/gestion-roles" element={<GestionRoles />} />
                <Route path="/asignaturas" element={<GestionAsignaturas />} />
                <Route path="/asignaciones" element={<Asignaciones />} />
              </Route>

              {/* Teacher Routes */}
              <Route
                element={
                  <RoleRoute
                    allowedRoles={[
                      'Profesor de Asignatura',
                      'Profesor Colaborador',
                      'Tutor',
                    ]}
                  />
                }
              >
                <Route path="/mis-estudiantes" element={<MisEstudiantes />} />
              </Route>
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
```

---

#### File 20: `frontend/src/layouts/MainLayout.jsx` (REFACTORED)
```jsx
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import logoPitagoras from '../assets/logoUBB.png';

export default function MainLayout() {
  const { user, userRole, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length > 1) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0][0].toUpperCase();
  };

  const isActive = (path) => location.pathname === path;

  const baseLinkClass =
    'flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium';
  const activeLinkClass = 'bg-orange-600 text-white shadow-md';
  const inactiveLinkClass = 'text-gray-300 hover:bg-[#004A8F] hover:text-white';

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Menú Lateral (Sidebar) */}
      <aside className="w-64 bg-[#003B70] text-white p-5 flex flex-col shadow-xl z-10 relative">
        <div className="flex items-center gap-3 mb-10 mt-2 px-2">
          <img src={logoPitagoras} alt="Logo Pitagoras" className="w-10 h-10 object-contain" />
          <h2 className="text-2xl font-bold tracking-widest text-orange-500">PITAGORAS</h2>
        </div>

        <nav className="flex flex-col space-y-2 flex-grow">
          <Link
            to="/dashboard"
            className={`${baseLinkClass} ${isActive('/dashboard') ? activeLinkClass : inactiveLinkClass}`}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
              ></path>
            </svg>
            Inicio
          </Link>

          {/* Opciones exclusivas de Estudiantes */}
          {userRole === 'Estudiante' && (
            <Link
              to="/portafolio"
              className={`${baseLinkClass} ${isActive('/portafolio') ? activeLinkClass : inactiveLinkClass}`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                ></path>
              </svg>
              Mi Portafolio
            </Link>
          )}

          {/* Opciones exclusivas de Administrador */}
          {userRole === 'Administrador' && (
            <div className="mt-8">
              <p className="text-xs font-semibold text-blue-300 uppercase tracking-wider mb-4 px-4 opacity-80">
                Administración
              </p>

              <div className="flex flex-col space-y-2">
                <Link
                  to="/carga-masiva"
                  className={`${baseLinkClass} ${isActive('/carga-masiva') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    ></path>
                  </svg>
                  Carga Masiva
                </Link>

                <Link
                  to="/gestion-roles"
                  className={`${baseLinkClass} ${isActive('/gestion-roles') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                    ></path>
                  </svg>
                  Gestión de Usuarios
                </Link>

                <Link
                  to="/asignaturas"
                  className={`${baseLinkClass} ${isActive('/asignaturas') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                    ></path>
                  </svg>
                  Asignaturas
                </Link>

                <Link
                  to="/asignaciones"
                  className={`${baseLinkClass} ${isActive('/asignaciones') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                    ></path>
                  </svg>
                  Asignaciones
                </Link>
              </div>
            </div>
          )}

          {/* Opciones exclusivas de Docentes */}
          {['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(userRole) && (
            <div className="mt-8">
              <p className="text-xs font-semibold text-blue-300 uppercase tracking-wider mb-4 px-4 opacity-80">
                Docencia
              </p>

              <div className="flex flex-col space-y-2">
                <Link
                  to="/mis-estudiantes"
                  className={`${baseLinkClass} ${isActive('/mis-estudiantes') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    ></path>
                  </svg>
                  Mis Estudiantes
                </Link>
              </div>
            </div>
          )}
        </nav>

        {/* Sección del Usuario */}
        <div className="mt-auto border-t border-blue-800 pt-4">
          {user && (
            <div className="flex items-center justify-between">
              <Link
                to="/perfil"
                className="flex items-center gap-3 overflow-hidden cursor-pointer hover:opacity-80 transition-opacity"
              >
                <div className="flex-shrink-0 w-10 h-10 bg-orange-600 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-inner">
                  {getInitials(user.fullName)}
                </div>
                <div className="flex flex-col truncate">
                  <span className="text-sm font-semibold text-white truncate">
                    {user.fullName || 'Usuario'}
                  </span>
                  <span className="text-xs text-blue-300 truncate">{userRole || 'Sin Rol'}</span>
                </div>
              </Link>

              <button
                onClick={handleLogout}
                className="ml-2 p-2 text-gray-400 hover:text-red-400 hover:bg-gray-800 hover:bg-opacity-40 rounded-lg transition-colors"
                title="Cerrar Sesión"
                aria-label="Cerrar sesión"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                  ></path>
                </svg>
              </button>
            </div>
          )}
        </div>
      </aside>

      {/* Contenedor Principal */}
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  );
}
```

---

## 5. Verification Method

Once Worker implements these files on branch `fabian`, verify compliance via the following sequence:

### 5.1. Build & Lint Verification
1. Run Vite frontend build:
   ```powershell
   cd "frontend"
   npm run build
   ```
   **Expected Outcome:** `vite build` completes with exit code 0 (`dist/` generated without syntax or import errors).
2. Run Oxlint:
   ```powershell
   npm run lint
   ```
   **Expected Outcome:** 0 fatal lint errors; all components and services conform to React 19 standards.

### 5.2. File System Structure Check
Confirm that the 20 files are created and placed in the exact directory tree:
```
frontend/src/
├── services/
│   ├── apiClient.js
│   ├── authService.js
│   ├── portfolioService.js
│   ├── subjectService.js
│   ├── assignmentService.js
│   ├── centerService.js
│   └── userService.js
├── components/
│   ├── ui/
│   │   ├── Button.jsx
│   │   ├── Input.jsx
│   │   ├── Select.jsx
│   │   ├── Modal.jsx
│   │   ├── DataTable.jsx
│   │   ├── ConfirmDialog.jsx
│   │   ├── Badge.jsx
│   │   └── Spinner.jsx
│   ├── ProtectedRoute.jsx
│   └── RoleRoute.jsx
├── context/
│   └── AuthContext.jsx
├── layouts/
│   └── MainLayout.jsx
└── App.jsx
```

### 5.3. Behavioral Verification Criteria
- **Authentication Guarding:** Unauthenticated browser navigating to `/dashboard` is redirected to `/` with no Flash of Unauthenticated Content (FE-05).
- **Role Enforcement:** Authenticated user with role `Estudiante` attempting to browse `/gestion-roles` is redirected to `/dashboard` with an error toast notification (FE-03).
- **Profile Synchronization:** Updating `fullName` in `Perfil.jsx` invokes `updateUserProfile()`; the `MainLayout` avatar initials and name update immediately without triggering `window.location.reload()` (FE-04).
- **Token Cleanup:** Invoking `logout()` purges both `token` and `usuarioActivo` from `localStorage` (TR-04).
- **Dynamic URL Resolution:** In testing or production Docker containers, configuring `VITE_API_URL` properly directs all API requests to the specified host without `http://localhost:8080` fallback (FE-06, FE-23).
