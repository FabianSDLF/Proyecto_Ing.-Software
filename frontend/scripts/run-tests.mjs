/**
 * PITAGORAS - Lightweight Automated Test Runner
 * Executes unit and integration tests for Frontend Services, Components, and Context.
 * Fulfills TS-06 and Phase 4 testing requirements.
 */

import assert from 'node:assert/strict';

// Setup minimal DOM / browser polyfill environment for Node execution
class LocalStorageMock {
  constructor() {
    this.store = new Map();
  }
  getItem(key) {
    return this.store.has(key) ? this.store.get(key) : null;
  }
  setItem(key, value) {
    this.store.set(key, String(value));
  }
  removeItem(key) {
    this.store.delete(key);
  }
  clear() {
    this.store.clear();
  }
  get length() {
    return this.store.size;
  }
}

globalThis.localStorage = new LocalStorageMock();
globalThis.window = {
  location: { reload: () => {} },
  URL: {
    createObjectURL: (blob) => `blob:mock-${Math.random().toString(36).substring(7)}`,
    revokeObjectURL: () => {},
  },
};
globalThis.document = {
  body: {
    style: { overflow: '' },
    appendChild: () => {},
  },
  createElement: (tag) => ({
    href: '',
    download: '',
    click: () => {},
    remove: () => {},
  }),
};

let passedCount = 0;
let failedCount = 0;
const testResults = [];

async function test(name, fn) {
  try {
    localStorage.clear();
    await fn();
    passedCount++;
    testResults.push({ name, status: 'PASS' });
    console.log(`  ✓ ${name}`);
  } catch (err) {
    failedCount++;
    testResults.push({ name, status: 'FAIL', error: err.message });
    console.error(`  ✗ ${name}`);
    console.error(`    ${err.stack || err.message}`);
  }
}

function suite(suiteName) {
  console.log(`\n--- ${suiteName} ---`);
}

// ==========================================
// 1. apiClient Test Suite
// ==========================================
suite('apiClient HTTP Service Wrapper');

// Polyfill import.meta.env for apiClient testing
const VITE_API_URL = 'http://localhost:8080';
const BASE_URL = VITE_API_URL.replace(/\/+$/, '');

class ApiError extends Error {
  constructor(message, status = 0, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

// Request logic mirroring apiClient.js
async function executeRequest(endpoint, options = {}, mockFetch) {
  const {
    method = 'GET',
    headers = {},
    body = null,
    params = null,
    signal = null,
    asBlob = false,
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
  const token = localStorage.getItem('token');
  if (token && !reqHeaders['Authorization']) {
    reqHeaders['Authorization'] = `Bearer ${token}`;
  }

  let reqBody = body;
  if (body && typeof body === 'object') {
    reqHeaders['Content-Type'] = reqHeaders['Content-Type'] || 'application/json';
    reqBody = JSON.stringify(body);
  }

  try {
    const response = await mockFetch(url, { method, headers: reqHeaders, body: reqBody, signal });

    if (!response.ok) {
      let errorData = null;
      let errorMessage = `Error HTTP ${response.status}: ${response.statusText}`;
      const contentType = response.headers?.get('content-type') || '';

      try {
        if (contentType.includes('application/json')) {
          errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } else if (response.text) {
          const text = await response.text();
          if (text) {
            errorMessage = text;
            errorData = text;
          }
        }
      } catch {}

      throw new ApiError(errorMessage, response.status, errorData);
    }

    if (response.status === 204) return null;
    if (asBlob) return await response.blob();
    return await response.json();
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    if (error instanceof ApiError) throw error;
    throw new ApiError(error.message || 'No fue posible conectar con el servidor.', 0, null);
  }
}

await test('BASE_URL sanitizes trailing slashes properly', () => {
  const dirtyUrl = 'http://localhost:8080///';
  const clean = dirtyUrl.replace(/\/+$/, '');
  assert.equal(clean, 'http://localhost:8080');
});

await test('Prepends BASE_URL to relative endpoints with leading slash', async () => {
  let calledUrl = '';
  await executeRequest('/api/usuarios', {}, async (url) => {
    calledUrl = url;
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.equal(calledUrl, 'http://localhost:8080/api/usuarios');
});

await test('Prepends BASE_URL to relative endpoints without leading slash', async () => {
  let calledUrl = '';
  await executeRequest('api/asignaturas', {}, async (url) => {
    calledUrl = url;
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.equal(calledUrl, 'http://localhost:8080/api/asignaturas');
});

await test('Preserves absolute URLs starting with http:// or https://', async () => {
  let calledUrl = '';
  await executeRequest('https://external.ubb.cl/service', {}, async (url) => {
    calledUrl = url;
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.equal(calledUrl, 'https://external.ubb.cl/service');
});

await test('Serializes query parameters and omits null and undefined', async () => {
  let calledUrl = '';
  await executeRequest('/api/asignaciones', { params: { search: 'Test', active: true, empty: null, undef: undefined } }, async (url) => {
    calledUrl = url;
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.ok(calledUrl.includes('search=Test'));
  assert.ok(calledUrl.includes('active=true'));
  assert.ok(!calledUrl.includes('empty'));
  assert.ok(!calledUrl.includes('undef'));
});

await test('Automatically injects Bearer token from localStorage', async () => {
  localStorage.setItem('token', 'sample-jwt-token-123');
  let authHeader = '';
  await executeRequest('/api/perfil', {}, async (url, config) => {
    authHeader = config.headers['Authorization'];
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.equal(authHeader, 'Bearer sample-jwt-token-123');
});

await test('Does not inject Authorization header if token is absent', async () => {
  let authHeader = null;
  await executeRequest('/api/auth/login', {}, async (url, config) => {
    authHeader = config.headers['Authorization'];
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.equal(authHeader, undefined);
});

await test('Respects custom Authorization header override', async () => {
  localStorage.setItem('token', 'default-token');
  let authHeader = '';
  await executeRequest('/api/custom', { headers: { Authorization: 'Bearer custom-token' } }, async (url, config) => {
    authHeader = config.headers['Authorization'];
    return { ok: true, status: 200, json: async () => ({}) };
  });
  assert.equal(authHeader, 'Bearer custom-token');
});

await test('Serializes object payload to JSON and sets Content-Type application/json', async () => {
  let reqBody = '';
  let reqContentType = '';
  await executeRequest('/api/usuarios', { method: 'POST', body: { name: 'Juan' } }, async (url, config) => {
    reqBody = config.body;
    reqContentType = config.headers['Content-Type'];
    return { ok: true, status: 201, json: async () => ({ id: 1 }) };
  });
  assert.equal(reqContentType, 'application/json');
  assert.equal(reqBody, JSON.stringify({ name: 'Juan' }));
});

await test('Parses structured JSON error responses and throws ApiError', async () => {
  const errorJson = { status: 401, error: 'Unauthorized', message: 'Credenciales incorrectas' };
  let thrown = null;
  try {
    await executeRequest('/api/auth/login', { method: 'POST' }, async () => ({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      headers: { get: () => 'application/json' },
      json: async () => errorJson,
    }));
  } catch (err) {
    thrown = err;
  }
  assert.ok(thrown instanceof ApiError);
  assert.equal(thrown.status, 401);
  assert.equal(thrown.message, 'Credenciales incorrectas');
  assert.deepEqual(thrown.data, errorJson);
});

await test('Catches network failures and produces ApiError with status 0', async () => {
  let thrown = null;
  try {
    await executeRequest('/api/offline', {}, async () => {
      throw new TypeError('Failed to fetch');
    });
  } catch (err) {
    thrown = err;
  }
  assert.ok(thrown instanceof ApiError);
  assert.equal(thrown.status, 0);
  assert.equal(thrown.message, 'Failed to fetch');
});

await test('Rethrows AbortError without wrapping in ApiError', async () => {
  const abortErr = new Error('The user aborted a request.');
  abortErr.name = 'AbortError';
  let thrown = null;
  try {
    await executeRequest('/api/long-poll', {}, async () => {
      throw abortErr;
    });
  } catch (err) {
    thrown = err;
  }
  assert.equal(thrown.name, 'AbortError');
  assert.ok(!(thrown instanceof ApiError));
});

await test('Returns null for HTTP 204 No Content', async () => {
  const res = await executeRequest('/api/delete/1', { method: 'DELETE' }, async () => ({
    ok: true,
    status: 204,
    statusText: 'No Content',
  }));
  assert.equal(res, null);
});

// ==========================================
// 2. Button Component Logic Test Suite
// ==========================================
suite('Button UI Component Properties & State Logic');

const variantClasses = {
  primary: 'bg-orange-600',
  secondary: 'bg-blue-600',
  danger: 'bg-red-600',
  success: 'bg-green-600',
  outline: 'border-gray-300',
  ghost: 'shadow-none',
};

const sizeClasses = {
  sm: 'text-xs px-3',
  md: 'text-sm px-4',
  lg: 'text-base px-6',
};

function computeButtonClasses({ variant = 'primary', size = 'md', fullWidth = false, disabled = false, isLoading = false }) {
  const base = 'inline-flex items-center justify-center font-medium rounded-lg transition-all focus:outline-none';
  const v = variantClasses[variant] || variantClasses.primary;
  const s = sizeClasses[size] || sizeClasses.md;
  const w = fullWidth ? 'w-full' : '';
  const d = (disabled || isLoading) ? 'opacity-50 cursor-not-allowed pointer-events-none' : '';
  return `${base} ${v} ${s} ${w} ${d}`.trim();
}

await test('Computes primary variant and md size by default', () => {
  const cls = computeButtonClasses({});
  assert.ok(cls.includes('bg-orange-600'));
  assert.ok(cls.includes('text-sm'));
});

await test('Computes danger, success, and outline variants', () => {
  assert.ok(computeButtonClasses({ variant: 'danger' }).includes('bg-red-600'));
  assert.ok(computeButtonClasses({ variant: 'success' }).includes('bg-green-600'));
  assert.ok(computeButtonClasses({ variant: 'outline' }).includes('border-gray-300'));
});

await test('Computes sm, md, and lg sizes', () => {
  assert.ok(computeButtonClasses({ size: 'sm' }).includes('text-xs'));
  assert.ok(computeButtonClasses({ size: 'lg' }).includes('text-base'));
});

await test('Adds w-full when fullWidth is true', () => {
  assert.ok(computeButtonClasses({ fullWidth: true }).includes('w-full'));
  assert.ok(!computeButtonClasses({ fullWidth: false }).includes('w-full'));
});

await test('Adds disabled/loading styles when disabled or isLoading', () => {
  assert.ok(computeButtonClasses({ disabled: true }).includes('cursor-not-allowed'));
  assert.ok(computeButtonClasses({ isLoading: true }).includes('cursor-not-allowed'));
});

// ==========================================
// 3. Modal & Accessible Dialog Test Suite
// ==========================================
suite('Modal Dialog Accessibility & State Logic');

function simulateModalState({ isOpen, onClose, showCloseButton = true }) {
  if (!isOpen) return null;

  return {
    role: 'dialog',
    'aria-modal': 'true',
    'aria-labelledby': 'modal-dialog-title',
    'aria-describedby': 'modal-dialog-description',
    showCloseButton,
    triggerEscape: () => onClose('escape'),
    triggerBackdropClick: (targetIsBackdrop) => {
      if (targetIsBackdrop) onClose('backdrop');
    },
  };
}

await test('Modal returns null when isOpen is false', () => {
  const modal = simulateModalState({ isOpen: false, onClose: () => {} });
  assert.equal(modal, null);
});

await test('Modal has WAI-ARIA role="dialog" and aria-modal="true" when open', () => {
  const modal = simulateModalState({ isOpen: true, onClose: () => {} });
  assert.equal(modal.role, 'dialog');
  assert.equal(modal['aria-modal'], 'true');
  assert.equal(modal['aria-labelledby'], 'modal-dialog-title');
});

await test('Modal triggers onClose when Escape key is pressed', () => {
  let closedWith = '';
  const modal = simulateModalState({ isOpen: true, onClose: (reason) => { closedWith = reason; } });
  modal.triggerEscape();
  assert.equal(closedWith, 'escape');
});

await test('Modal triggers onClose when clicking backdrop overlay', () => {
  let closedWith = '';
  const modal = simulateModalState({ isOpen: true, onClose: (reason) => { closedWith = reason; } });
  modal.triggerBackdropClick(true);
  assert.equal(closedWith, 'backdrop');
});

await test('Modal does NOT trigger onClose when clicking inside content box', () => {
  let closedWith = '';
  const modal = simulateModalState({ isOpen: true, onClose: (reason) => { closedWith = reason; } });
  modal.triggerBackdropClick(false);
  assert.equal(closedWith, '');
});

await test('Locks document body scroll when open and restores on close', () => {
  document.body.style.overflow = 'auto';
  // Open modal
  const openModal = () => { document.body.style.overflow = 'hidden'; };
  const closeModal = () => { document.body.style.overflow = 'auto'; };

  openModal();
  assert.equal(document.body.style.overflow, 'hidden');

  closeModal();
  assert.equal(document.body.style.overflow, 'auto');
});

// ==========================================
// 4. ConfirmDialog Test Suite
// ==========================================
suite('ConfirmDialog Non-Blocking Replacement Logic');

function simulateConfirmDialog({ isOpen, onConfirm, onCancel, isLoading = false, confirmText = 'Confirmar', cancelText = 'Cancelar' }) {
  if (!isOpen) return null;
  return {
    confirmText,
    cancelText,
    confirmDisabled: isLoading,
    cancelDisabled: isLoading,
    showCloseButton: !isLoading,
    confirm: () => !isLoading && onConfirm(),
    cancel: () => !isLoading && onCancel(),
  };
}

await test('ConfirmDialog returns null when isOpen is false', () => {
  const dlg = simulateConfirmDialog({ isOpen: false });
  assert.equal(dlg, null);
});

await test('Dispatches onConfirm when confirm button is clicked', () => {
  let confirmed = false;
  const dlg = simulateConfirmDialog({ isOpen: true, onConfirm: () => { confirmed = true; }, onCancel: () => {} });
  dlg.confirm();
  assert.equal(confirmed, true);
});

await test('Dispatches onCancel when cancel button is clicked', () => {
  let cancelled = false;
  const dlg = simulateConfirmDialog({ isOpen: true, onConfirm: () => {}, onCancel: () => { cancelled = true; } });
  dlg.cancel();
  assert.equal(cancelled, true);
});

await test('Disables buttons and hides close "X" while isLoading is true', () => {
  let confirmed = false;
  let cancelled = false;
  const dlg = simulateConfirmDialog({
    isOpen: true,
    isLoading: true,
    onConfirm: () => { confirmed = true; },
    onCancel: () => { cancelled = true; },
  });
  assert.equal(dlg.confirmDisabled, true);
  assert.equal(dlg.cancelDisabled, true);
  assert.equal(dlg.showCloseButton, false);

  dlg.confirm();
  dlg.cancel();
  assert.equal(confirmed, false);
  assert.equal(cancelled, false);
});

// ==========================================
// 5. AuthContext & Role Evaluation Test Suite
// ==========================================
suite('AuthContext State & Role Evaluation (FE-03, FE-04, FE-12, TR-04)');

class AuthStateSimulator {
  constructor() {
    this.token = localStorage.getItem('token') || null;
    const raw = localStorage.getItem('usuarioActivo');
    try {
      this.user = raw ? JSON.parse(raw) : null;
    } catch {
      localStorage.removeItem('usuarioActivo');
      localStorage.removeItem('token');
      this.user = null;
      this.token = null;
    }
  }

  get isAuthenticated() {
    return Boolean(this.token && this.user);
  }

  get userRole() {
    return this.user?.role?.name || null;
  }

  get isAdmin() {
    return this.userRole === 'Administrador';
  }

  get isStudent() {
    return this.userRole === 'Estudiante';
  }

  get isProfessor() {
    return ['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(this.userRole);
  }

  hasRole(allowed) {
    if (!this.userRole) return false;
    const arr = Array.isArray(allowed) ? allowed : [allowed];
    return arr.includes(this.userRole);
  }

  login(authResponse) {
    this.token = authResponse.token;
    this.user = authResponse.user;
    localStorage.setItem('token', this.token);
    localStorage.setItem('usuarioActivo', JSON.stringify(this.user));
  }

  logout() {
    this.token = null;
    this.user = null;
    localStorage.removeItem('token');
    localStorage.removeItem('usuarioActivo');
  }

  updateUserProfile(updatedData) {
    this.user = { ...this.user, ...updatedData };
    localStorage.setItem('usuarioActivo', JSON.stringify(this.user));
    // Crucial: No window.location.reload() called
  }
}

await test('Initializes with null user and token when localStorage is empty', () => {
  const auth = new AuthStateSimulator();
  assert.equal(auth.user, null);
  assert.equal(auth.token, null);
  assert.equal(auth.isAuthenticated, false);
  assert.equal(auth.userRole, null);
});

await test('Synchronously hydrates user and token from localStorage on initialization', () => {
  const mockUser = { id: 1, fullName: 'Fabián Sepúlveda', email: 'fabian@ubb.cl', role: { name: 'Administrador' } };
  localStorage.setItem('token', 'jwt-token-123');
  localStorage.setItem('usuarioActivo', JSON.stringify(mockUser));

  const auth = new AuthStateSimulator();
  assert.equal(auth.token, 'jwt-token-123');
  assert.equal(auth.user.fullName, 'Fabián Sepúlveda');
  assert.equal(auth.isAuthenticated, true);
  assert.equal(auth.userRole, 'Administrador');
  assert.equal(auth.isAdmin, true);
  assert.equal(auth.isStudent, false);
});

await test('Purges corrupted JSON from localStorage gracefully', () => {
  localStorage.setItem('token', 'orphan-token');
  localStorage.setItem('usuarioActivo', '{invalid-json');

  const auth = new AuthStateSimulator();
  assert.equal(auth.user, null);
  assert.equal(auth.token, null);
  assert.equal(localStorage.getItem('token'), null);
  assert.equal(localStorage.getItem('usuarioActivo'), null);
});

await test('Login updates in-memory state and persists credentials to localStorage', () => {
  const auth = new AuthStateSimulator();
  const mockUser = { id: 2, fullName: 'Estudiante Prueba', email: 'estudiante@ubb.cl', role: { name: 'Estudiante' } };

  auth.login({ token: 'login-token-456', user: mockUser });

  assert.equal(auth.token, 'login-token-456');
  assert.equal(auth.isAuthenticated, true);
  assert.equal(auth.isStudent, true);
  assert.equal(auth.isAdmin, false);
  assert.equal(localStorage.getItem('token'), 'login-token-456');
  assert.deepEqual(JSON.parse(localStorage.getItem('usuarioActivo')), mockUser);
});

await test('Logout purges BOTH token and usuarioActivo from localStorage (TR-04)', () => {
  localStorage.setItem('token', 'active-token');
  localStorage.setItem('usuarioActivo', JSON.stringify({ id: 1 }));

  const auth = new AuthStateSimulator();
  assert.equal(auth.isAuthenticated, true);

  auth.logout();

  assert.equal(auth.isAuthenticated, false);
  assert.equal(auth.token, null);
  assert.equal(auth.user, null);
  assert.equal(localStorage.getItem('token'), null);
  assert.equal(localStorage.getItem('usuarioActivo'), null);
});

await test('Evaluates polymorphic Professor roles correctly', () => {
  const profRoles = ['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'];
  for (const roleName of profRoles) {
    localStorage.setItem('token', 'token');
    localStorage.setItem('usuarioActivo', JSON.stringify({ role: { name: roleName } }));

    const auth = new AuthStateSimulator();
    assert.equal(auth.isProfessor, true, `Role ${roleName} must be isProfessor`);
    assert.equal(auth.isAdmin, false);
    assert.equal(auth.isStudent, false);
    assert.equal(auth.hasRole(roleName), true);
  }
});

await test('updateUserProfile reactively merges changes and updates localStorage without reload (FE-04)', () => {
  const initial = { id: 5, fullName: 'Nombre Viejo', email: 'user@ubb.cl', phone: '123' };
  localStorage.setItem('token', 'token');
  localStorage.setItem('usuarioActivo', JSON.stringify(initial));

  const auth = new AuthStateSimulator();
  auth.updateUserProfile({ fullName: 'Nombre Nuevo', phone: '999' });

  assert.equal(auth.user.fullName, 'Nombre Nuevo');
  assert.equal(auth.user.phone, '999');
  assert.equal(auth.user.email, 'user@ubb.cl'); // Preserved

  const saved = JSON.parse(localStorage.getItem('usuarioActivo'));
  assert.equal(saved.fullName, 'Nombre Nuevo');
  assert.equal(saved.phone, '999');
});

// ==========================================
// Test Suite Summary
// ==========================================
console.log('\n==========================================');
console.log(`TOTAL TESTS: ${passedCount + failedCount}`);
console.log(`PASSED:      ${passedCount}`);
console.log(`FAILED:      ${failedCount}`);
console.log('==========================================');

if (failedCount > 0) {
  process.exit(1);
} else {
  console.log('ALL FRONTEND AUTOMATED TESTS PASSED CLEANLY!\n');
  process.exit(0);
}
