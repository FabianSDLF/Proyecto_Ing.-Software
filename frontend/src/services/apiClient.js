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
