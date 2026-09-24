import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import apiClient, { BASE_URL, ApiError, downloadBlob } from '../../../frontend/src/services/apiClient';

describe('apiClient HTTP Service Wrapper', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    localStorage.clear();
    global.fetch = vi.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  describe('Base URL and Endpoint Resolution', () => {
    it('should configure BASE_URL without trailing slashes', () => {
      expect(BASE_URL).toBeDefined();
      expect(BASE_URL.endsWith('/')).toBe(false);
    });

    it('should prepend BASE_URL to relative endpoints starting with slash', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get('/api/usuarios');

      expect(global.fetch).toHaveBeenCalledTimes(1);
      const calledUrl = global.fetch.mock.calls[0][0];
      expect(calledUrl).toBe(`${BASE_URL}/api/usuarios`);
    });

    it('should prepend BASE_URL to relative endpoints without leading slash', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get('api/asignaturas');

      const calledUrl = global.fetch.mock.calls[0][0];
      expect(calledUrl).toBe(`${BASE_URL}/api/asignaturas`);
    });

    it('should preserve absolute URLs starting with http:// or https://', async () => {
      const externalUrl = 'https://external-service.ubb.cl/api/data';
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get(externalUrl);

      const calledUrl = global.fetch.mock.calls[0][0];
      expect(calledUrl).toBe(externalUrl);
    });

    it('should correctly serialize query parameters into URL search string', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get('/api/asignaciones', {
        params: { search: 'Juan', role: 'Estudiante', emptyVal: null, undefVal: undefined },
      });

      const calledUrl = global.fetch.mock.calls[0][0];
      expect(calledUrl).toContain('search=Juan');
      expect(calledUrl).toContain('role=Estudiante');
      expect(calledUrl).not.toContain('emptyVal');
      expect(calledUrl).not.toContain('undefVal');
    });

    it('should append params using & if URL already has query parameters', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get('/api/asignaciones?active=true', {
        params: { page: 2 },
      });

      const calledUrl = global.fetch.mock.calls[0][0];
      expect(calledUrl).toBe(`${BASE_URL}/api/asignaciones?active=true&page=2`);
    });
  });

  describe('Authorization Header & Token Injection (FE-01, TR-04)', () => {
    it('should automatically inject Bearer token from localStorage when present', async () => {
      localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiJ9.test-token');

      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ user: 'test' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get('/api/usuarios/perfil');

      const requestOptions = global.fetch.mock.calls[0][1];
      expect(requestOptions.headers['Authorization']).toBe('Bearer eyJhbGciOiJIUzI1NiJ9.test-token');
    });

    it('should not inject Authorization header if token is absent in localStorage', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ public: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.post('/api/auth/login', { email: 'user@ubiobio.cl', password: '123' });

      const requestOptions = global.fetch.mock.calls[0][1];
      expect(requestOptions.headers['Authorization']).toBeUndefined();
    });

    it('should respect custom Authorization header if explicitly provided in options', async () => {
      localStorage.setItem('token', 'stored-token');

      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.get('/api/test', {
        headers: { Authorization: 'Bearer custom-token-override' },
      });

      const requestOptions = global.fetch.mock.calls[0][1];
      expect(requestOptions.headers['Authorization']).toBe('Bearer custom-token-override');
    });
  });

  describe('Payload Serialization & Content-Type', () => {
    it('should serialize JavaScript object to JSON and set Content-Type application/json', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ id: 1 }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }));

      const payload = { fullName: 'Fabián Sepúlveda', email: 'fabian@ubiobio.cl' };
      await apiClient.post('/api/usuarios', payload);

      const requestOptions = global.fetch.mock.calls[0][1];
      expect(requestOptions.headers['Content-Type']).toBe('application/json');
      expect(requestOptions.body).toBe(JSON.stringify(payload));
    });

    it('should leave FormData body as-is and avoid setting manual Content-Type (multipart)', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ uploaded: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      const formData = new FormData();
      formData.append('title', 'Guía de Geometría');

      await apiClient.upload('/api/portafolio/evidencias', formData);

      const requestOptions = global.fetch.mock.calls[0][1];
      expect(requestOptions.body).toBe(formData);
      expect(requestOptions.headers['Content-Type']).toBeUndefined();
    });
  });

  describe('Error Response Handling (FE-01, FE-09)', () => {
    it('should parse JSON error response and throw ApiError with status and custom message', async () => {
      const errorPayload = { message: 'Credenciales inválidas. Verifique correo o clave.', code: 'BAD_CREDENTIALS' };
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify(errorPayload), {
        status: 401,
        statusText: 'Unauthorized',
        headers: { 'Content-Type': 'application/json' },
      }));

      await expect(apiClient.post('/api/auth/login', { email: 'bad@ubb.cl', password: 'wrong' }))
        .rejects
        .toThrowError(ApiError);

      try {
        await apiClient.post('/api/auth/login', { email: 'bad@ubb.cl', password: 'wrong' });
      } catch (err) {
        expect(err.name).toBe('ApiError');
        expect(err.status).toBe(401);
        expect(err.message).toBe('Credenciales inválidas. Verifique correo o clave.');
        expect(err.data).toEqual(errorPayload);
      }
    });

    it('should fallback to errorData.error if message field is absent', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ error: 'Acceso Denegado' }), {
        status: 403,
        statusText: 'Forbidden',
        headers: { 'Content-Type': 'application/json' },
      }));

      await expect(apiClient.get('/api/usuarios/admin-only'))
        .rejects
        .toMatchObject({
          status: 403,
          message: 'Acceso Denegado',
        });
    });

    it('should handle non-JSON text error responses (e.g. 500 HTML/text)', async () => {
      global.fetch.mockResolvedValueOnce(new Response('Internal Server Error', {
        status: 500,
        statusText: 'Internal Server Error',
        headers: { 'Content-Type': 'text/plain' },
      }));

      await expect(apiClient.get('/api/crash'))
        .rejects
        .toMatchObject({
          status: 500,
          message: 'Internal Server Error',
        });
    });

    it('should catch network disconnections (status 0) and throw user-friendly ApiError', async () => {
      global.fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

      await expect(apiClient.get('/api/asignaturas'))
        .rejects
        .toMatchObject({
          status: 0,
          message: 'Failed to fetch',
        });
    });

    it('should rethrow AbortError directly without wrapping into ApiError (FE-14)', async () => {
      const abortError = new DOMException('The user aborted a request.', 'AbortError');
      global.fetch.mockRejectedValueOnce(abortError);

      try {
        await apiClient.get('/api/long-polling');
        expect.unreachable('Should have thrown AbortError');
      } catch (err) {
        expect(err.name).toBe('AbortError');
        expect(err instanceof ApiError).toBe(false);
      }
    });
  });

  describe('HTTP Status Codes and Return Types', () => {
    it('should return null for HTTP 204 No Content', async () => {
      global.fetch.mockResolvedValueOnce(new Response(null, {
        status: 204,
        statusText: 'No Content',
      }));

      const result = await apiClient.delete('/api/asignaciones/10');
      expect(result).toBeNull();
    });

    it('should return parsed JSON when Content-Type is application/json', async () => {
      const mockData = [{ id: 1, name: 'Práctica Inicial' }];
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify(mockData), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      const result = await apiClient.get('/api/asignaturas');
      expect(result).toEqual(mockData);
    });

    it('should return plain text when Content-Type is text/plain', async () => {
      global.fetch.mockResolvedValueOnce(new Response('OK - Pong', {
        status: 200,
        headers: { 'Content-Type': 'text/plain' },
      }));

      const result = await apiClient.get('/api/health');
      expect(result).toBe('OK - Pong');
    });
  });

  describe('Convenience Methods and File Download (FE-24)', () => {
    it('should call apiClient.put with PUT method and payload', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ updated: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.put('/api/usuarios/perfil', { fullName: 'Fabián' });

      expect(global.fetch.mock.calls[0][1].method).toBe('PUT');
      expect(global.fetch.mock.calls[0][1].body).toBe(JSON.stringify({ fullName: 'Fabián' }));
    });

    it('should call apiClient.patch with PATCH method', async () => {
      global.fetch.mockResolvedValueOnce(new Response(JSON.stringify({ patched: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }));

      await apiClient.patch('/api/usuarios/1/status', { active: false });

      expect(global.fetch.mock.calls[0][1].method).toBe('PATCH');
    });

    it('should handle apiClient.download creating a Blob and triggering downloadBlob', async () => {
      const mockBlob = new Blob(['sample-pdf-content'], { type: 'application/pdf' });
      global.fetch.mockResolvedValueOnce(new Response(mockBlob, {
        status: 200,
        headers: { 'Content-Type': 'application/pdf' },
      }));

      const returnedBlob = await apiClient.download('/api/portafolio/descargar/5', 'evidencia.pdf');

      expect(returnedBlob).toBeDefined();
      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalled();
    });
  });
});
