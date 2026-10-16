import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act, renderHook } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from '../context/AuthContext';
import authService from '../services/authService';

// Mock authService
vi.mock('../services/authService', () => ({
  default: {
    login: vi.fn(),
    logout: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

describe('AuthContext & useAuth Hook', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  describe('Context Boundary Guard', () => {
    it('throws error when useAuth is consumed outside AuthProvider', () => {
      // Suppress console.error for expected React boundary error
      const originalConsoleError = console.error;
      console.error = vi.fn();

      expect(() => renderHook(() => useAuth())).toThrow(
        'useAuth debe ser utilizado dentro de un <AuthProvider />'
      );

      console.error = originalConsoleError;
    });
  });

  describe('Initial State & LocalStorage Hydration (FE-12)', () => {
    it('initializes with null state when localStorage is empty', () => {
      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.user).toBeNull();
      expect(result.current.token).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.userRole).toBeNull();
    });

    it('hydrates user and token synchronously from localStorage on mount', () => {
      const mockUser = {
        id: 1,
        fullName: 'Fabián Sepúlveda',
        email: 'fabian@ubiobio.cl',
        role: { id: 1, name: 'Coordinador de practicas' },
      };
      localStorage.setItem('token', 'valid-jwt-token');
      localStorage.setItem('usuarioActivo', JSON.stringify(mockUser));

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.token).toBe('valid-jwt-token');
      expect(result.current.user).toEqual(mockUser);
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.userRole).toBe('Coordinador de practicas');
    });

    it('safely handles malformed JSON in localStorage without crashing', () => {
      localStorage.setItem('token', 'orphan-token');
      localStorage.setItem('usuarioActivo', '{malformed-json');

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
      expect(localStorage.getItem('usuarioActivo')).toBeNull();
      expect(localStorage.getItem('token')).toBeNull();
    });
  });

  describe('Login Flow', () => {
    it('authenticates user, updates reactive state, and persists credentials', async () => {
      const mockAuthResponse = {
        token: 'new-jwt-token-xyz',
        user: {
          id: 5,
          fullName: 'Angelo Estudiante',
          email: 'angelo@alumnos.ubiobio.cl',
          role: { id: 2, name: 'Estudiante' },
        },
      };

      authService.login.mockResolvedValueOnce(mockAuthResponse);

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      await act(async () => {
        await result.current.login({ email: 'angelo@alumnos.ubiobio.cl', password: 'password123' });
      });

      expect(authService.login).toHaveBeenCalledWith({
        email: 'angelo@alumnos.ubiobio.cl',
        password: 'password123',
      });

      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.token).toBe('new-jwt-token-xyz');
      expect(result.current.user).toEqual(mockAuthResponse.user);
      expect(result.current.userRole).toBe('Estudiante');

      expect(localStorage.getItem('token')).toBe('new-jwt-token-xyz');
      expect(JSON.parse(localStorage.getItem('usuarioActivo'))).toEqual(mockAuthResponse.user);
    });

    it('propagates error and resets loading state when login fails', async () => {
      authService.login.mockRejectedValueOnce(new Error('Credenciales inválidas'));

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      await expect(
        act(async () => {
          await result.current.login({ email: 'unknown@ubb.cl', password: 'bad' });
        })
      ).rejects.toThrow('Credenciales inválidas');

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.token).toBeNull();
      expect(result.current.user).toBeNull();
    });
  });

  describe('Logout Flow (TR-04)', () => {
    it('purges BOTH token and usuarioActivo from localStorage and resets context state', () => {
      localStorage.setItem('token', 'active-token');
      localStorage.setItem('usuarioActivo', JSON.stringify({ id: 1, role: { name: 'Coordinador de practicas' } }));

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.isAuthenticated).toBe(true);

      act(() => {
        result.current.logout();
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(result.current.token).toBeNull();
      expect(result.current.userRole).toBeNull();

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('usuarioActivo')).toBeNull();
    });
  });

  describe('Role Evaluation (TS-06, FE-03)', () => {
    it('correctly evaluates Administrador role', () => {
      localStorage.setItem('token', 'admin-token');
      localStorage.setItem(
        'usuarioActivo',
        JSON.stringify({ id: 1, fullName: 'Admin', role: { name: 'Coordinador de practicas' } })
      );

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.userRole).toBe('Coordinador de practicas');
      expect(result.current.hasRole('Coordinador de practicas')).toBe(true);
      expect(result.current.hasRole('Estudiante')).toBe(false);
      expect(result.current.hasRole(['Coordinador de practicas', 'Tutor'])).toBe(true);

      // Dedicated role getters if present
      if ('isAdmin' in result.current) {
        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isStudent).toBe(false);
        expect(result.current.isProfessor).toBe(false);
      }
    });

    it('correctly evaluates Estudiante role', () => {
      localStorage.setItem('token', 'student-token');
      localStorage.setItem(
        'usuarioActivo',
        JSON.stringify({ id: 2, fullName: 'Alumno', role: { name: 'Estudiante' } })
      );

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.userRole).toBe('Estudiante');
      expect(result.current.hasRole('Estudiante')).toBe(true);
      expect(result.current.hasRole('Coordinador de practicas')).toBe(false);

      if ('isStudent' in result.current) {
        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isStudent).toBe(true);
        expect(result.current.isProfessor).toBe(false);
      }
    });

    it('correctly evaluates polymorphic Professor roles (Profesor de Asignatura, Colaborador, Tutor)', () => {
      const professorRoles = ['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'];

      professorRoles.forEach((roleName) => {
        localStorage.setItem('token', `${roleName}-token`);
        localStorage.setItem(
          'usuarioActivo',
          JSON.stringify({ id: 3, fullName: 'Docente', role: { name: roleName } })
        );

        const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

        expect(result.current.userRole).toBe(roleName);
        expect(result.current.hasRole(roleName)).toBe(true);
        expect(result.current.hasRole('Coordinador de practicas')).toBe(false);
        expect(result.current.hasRole('Estudiante')).toBe(false);

        if ('isProfessor' in result.current) {
          expect(result.current.isProfessor).toBe(true);
          expect(result.current.isAdmin).toBe(false);
          expect(result.current.isStudent).toBe(false);
        }
      });
    });

    it('returns false for all roles when user is unauthenticated', () => {
      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.hasRole('Coordinador de practicas')).toBe(false);
      expect(result.current.hasRole('Estudiante')).toBe(false);
      expect(result.current.hasRole(['Tutor', 'Profesor Colaborador'])).toBe(false);

      if ('isAdmin' in result.current) {
        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isStudent).toBe(false);
        expect(result.current.isProfessor).toBe(false);
      }
    });
  });

  describe('Profile Updating without Page Reload (FE-04)', () => {
    it('updates user profile reactively in memory and synchronizes localStorage without window.location.reload()', () => {
      const initialUser = {
        id: 10,
        fullName: 'Profesor Inicial',
        email: 'profesor@ubiobio.cl',
        phone: '+56900000000',
        role: { name: 'Profesor de Asignatura' },
      };

      localStorage.setItem('token', 'active-token');
      localStorage.setItem('usuarioActivo', JSON.stringify(initialUser));

      // Spy on window.location.reload to ensure it is NOT called
      const reloadMock = vi.fn();
      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...window.location, reload: reloadMock },
      });

      const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

      expect(result.current.user.fullName).toBe('Profesor Inicial');

      act(() => {
        result.current.updateUserProfile({
          fullName: 'Profesor Modificado',
          phone: '+56988887777',
        });
      });

      // Verify reactive in-memory state updated immediately
      expect(result.current.user.fullName).toBe('Profesor Modificado');
      expect(result.current.user.phone).toBe('+56988887777');
      expect(result.current.user.email).toBe('profesor@ubiobio.cl'); // Preserved
      expect(result.current.user.role.name).toBe('Profesor de Asignatura'); // Preserved

      // Verify localStorage is synchronized
      const stored = JSON.parse(localStorage.getItem('usuarioActivo'));
      expect(stored.fullName).toBe('Profesor Modificado');
      expect(stored.phone).toBe('+56988887777');

      // Crucial verification of FE-04: window.location.reload was NEVER invoked
      expect(reloadMock).not.toHaveBeenCalled();
    });
  });
});
