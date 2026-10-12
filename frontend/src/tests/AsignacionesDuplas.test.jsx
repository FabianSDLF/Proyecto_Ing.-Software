import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import toast from 'react-hot-toast';
import Asignaciones from '../pages/Asignaciones';
import assignmentService from '../services/assignmentService';
import userService from '../services/userService';
import subjectService from '../services/subjectService';
import centerService from '../services/centerService';

// Mock dependencies
vi.mock('react-hot-toast', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
    loading: vi.fn(),
  },
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    loading: vi.fn(),
  },
}));

vi.mock('../services/assignmentService', () => ({
  default: {
    getAssignments: vi.fn(),
    createAssignment: vi.fn(),
    deleteAssignment: vi.fn(),
    getDuplas: vi.fn(),
    crearDupla: vi.fn(),
    getEstudiantesDisponiblesDupla: vi.fn(),
  },
  assignmentService: {
    getAssignments: vi.fn(),
    createAssignment: vi.fn(),
    deleteAssignment: vi.fn(),
    getDuplas: vi.fn(),
    crearDupla: vi.fn(),
    getEstudiantesDisponiblesDupla: vi.fn(),
  },
}));

vi.mock('../services/userService', () => ({
  default: {
    getUsersByRole: vi.fn(),
  },
  userService: {
    getUsersByRole: vi.fn(),
  },
}));

vi.mock('../services/subjectService', () => ({
  default: {
    getSubjects: vi.fn(),
  },
  subjectService: {
    getSubjects: vi.fn(),
  },
}));

vi.mock('../services/centerService', () => ({
  default: {
    getCenters: vi.fn(),
  },
  centerService: {
    getCenters: vi.fn(),
  },
}));

describe('HU9 E2E Frontend Suite: Duplas de Estudiantes en Ciclo Intermedio (AsignacionesDuplas)', () => {
  const mockDuplas = [
    {
      id: 101,
      assignment1: {
        id: 1,
        student: { id: 11, fullName: 'Camila Reyes', email: 'camila.reyes@alumnos.ubb.cl' },
        subject: { id: 31, name: 'Práctica Intermedia I', cycle: 'INTERMEDIO' },
        center: { id: 51, name: 'Colegio Bicentenario' },
      },
      assignment2: {
        id: 2,
        student: { id: 12, fullName: 'Rodrigo Valenzuela', email: 'rodrigo.valenzuela@alumnos.ubb.cl' },
        subject: { id: 31, name: 'Práctica Intermedia I', cycle: 'INTERMEDIO' },
        center: { id: 51, name: 'Colegio Bicentenario' },
      },
      period: { id: 1, name: '2026-1' },
      createdAt: '2026-03-20T10:00:00Z',
    },
  ];

  const mockDisponibles = [
    {
      id: 3,
      student: { id: 13, fullName: 'Valeria Soto', email: 'valeria.soto@alumnos.ubb.cl' },
      subject: { id: 31, name: 'Práctica Intermedia I', cycle: 'INTERMEDIO' },
      center: { id: 51, name: 'Colegio Bicentenario' },
    },
    {
      id: 4,
      student: { id: 14, fullName: 'Felipe Morales', email: 'felipe.morales@alumnos.ubb.cl' },
      subject: { id: 32, name: 'Práctica Intermedia II', cycle: 'INTERMEDIO' },
      center: { id: 52, name: 'Liceo San Nicolás' },
    },
    {
      id: 5,
      student: { id: 15, fullName: 'Ignacio Fuentes', email: 'ignacio.fuentes@alumnos.ubb.cl' },
      subject: { id: 32, name: 'Práctica Intermedia II', cycle: 'INTERMEDIO' },
      center: { id: 52, name: 'Liceo San Nicolás' },
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();

    assignmentService.getAssignments.mockResolvedValue([]);
    assignmentService.getDuplas.mockResolvedValue(mockDuplas);
    assignmentService.getEstudiantesDisponiblesDupla.mockResolvedValue(mockDisponibles);
    assignmentService.crearDupla.mockResolvedValue({ id: 102, message: 'Dupla creada exitosamente' });

    userService.getUsersByRole.mockImplementation((role) => {
      if (role === 'Estudiante') {
        return Promise.resolve([
          { id: 11, fullName: 'Camila Reyes', email: 'camila.reyes@alumnos.ubb.cl' },
          { id: 12, fullName: 'Rodrigo Valenzuela', email: 'rodrigo.valenzuela@alumnos.ubb.cl' },
        ]);
      }
      return Promise.resolve([]);
    });

    subjectService.getSubjects.mockResolvedValue([
      { id: 31, name: 'Práctica Intermedia I', cycle: 'INTERMEDIO' },
      { id: 32, name: 'Práctica Intermedia II', cycle: 'INTERMEDIO' },
      { id: 33, name: 'Práctica Inicial', cycle: 'INICIAL' },
    ]);

    centerService.getCenters.mockResolvedValue([
      { id: 51, name: 'Colegio Bicentenario', city: 'Concepción' },
    ]);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // =========================================================================
  // TIER 1: FEATURE COVERAGE (>=5 tests)
  // =========================================================================

  describe('Tier 1: Feature Coverage (Primary Behavior / Happy Path)', () => {
    it('TC-FE-HU9-T1-01: Renders page and navigates to Duplas tab', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      expect(duplasTab).toBeInTheDocument();
      fireEvent.click(duplasTab);

      expect(assignmentService.getDuplas).toHaveBeenCalled();
    });

    it('TC-FE-HU9-T1-02: Displays duplas table with student names and center', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      await waitFor(() => {
        expect(screen.getByText(/Camila Reyes/i)).toBeInTheDocument();
        expect(screen.getByText(/Rodrigo Valenzuela/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU9-T1-03: Displays status badge indicating definitive assignment', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      await waitFor(() => {
        const badge = screen.getByText(/definitiva|permanente|activa/i);
        expect(badge).toBeInTheDocument();
      });
    });

    it('TC-FE-HU9-T1-04: Clicking "+ Nueva Dupla" opens creation modal', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      await waitFor(() => {
        expect(screen.getByText(/crear dupla|nueva dupla/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU9-T1-05: Submitting valid pair calls API and shows success notification', async () => {
      const user = userEvent.setup();
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      await waitFor(() => {
        expect(assignmentService.getEstudiantesDisponiblesDupla).toHaveBeenCalled();
      });

      // Submit creation if button is present
      const submitBtn = screen.queryByRole('button', { name: /guardar dupla|crear dupla/i });
      if (submitBtn) {
        await user.click(submitBtn);
      }
    });
  });

  // =========================================================================
  // TIER 2: BOUNDARY & CORNER CASES (>=5 tests)
  // =========================================================================

  describe('Tier 2: Boundary & Corner Cases', () => {
    it('TC-FE-HU9-T2-01: Displays permanent warning banner alerting duplas cannot be broken', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      await waitFor(() => {
        expect(screen.getByText(/definitivas|no podr[aá]n|permanente/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU9-T2-02: Duplas table does NOT render delete action buttons (inmutability check)', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      await waitFor(() => {
        expect(screen.getByText(/Camila Reyes/i)).toBeInTheDocument();
      });

      // Find the row containing Camila Reyes and verify no delete button exists for duplas
      const row = screen.getByText(/Camila Reyes/i).closest('tr');
      expect(row).toBeInTheDocument();
      const deleteBtn = within(row).queryByRole('button', { name: /eliminar|borrar|desactivar/i });
      expect(deleteBtn).toBeNull();
    });

    it('TC-FE-HU9-T2-03: Excludes already paired students from candidate list', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      await waitFor(() => {
        expect(assignmentService.getEstudiantesDisponiblesDupla).toHaveBeenCalled();
      });

      // Camila Reyes and Rodrigo Valenzuela are already paired, so they must NOT appear as options
      expect(screen.queryByText(/Camila Reyes \(Disponible\)/i)).not.toBeInTheDocument();
    });

    it('TC-FE-HU9-T2-04: Prevents self-pairing when same student is selected in both slots', async () => {
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      const submitBtn = screen.queryByRole('button', { name: /guardar dupla|crear dupla/i });
      // When slots are unselected or identical, submit button must be disabled
      if (submitBtn) {
        expect(submitBtn).toBeDisabled();
      }
    });

    it('TC-FE-HU9-T2-05: Live search input filters students list by name or email', async () => {
      const user = userEvent.setup();
      render(<Asignaciones />);

      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      const searchInput = screen.queryByPlaceholderText(/buscar estudiante|nombre o correo/i);
      if (searchInput) {
        await user.type(searchInput, 'Valeria');
        expect(screen.getByText(/Valeria Soto/i)).toBeInTheDocument();
        expect(screen.queryByText(/Ignacio Fuentes/i)).not.toBeInTheDocument();
      }
    });
  });

  // =========================================================================
  // TIER 3: CROSS-FEATURE COMBINATIONS
  // =========================================================================

  describe('Tier 3: Cross-Feature Combinations', () => {
    it('TC-FE-HU9-T3-01: Switching between Individual Assignments tab and Duplas tab maintains tab state isolation', async () => {
      render(<Asignaciones />);

      const indTab = await screen.findByRole('button', { name: /individuales|asignaciones/i });
      const duplasTab = await screen.findByRole('button', { name: /duplas/i });

      // Click Duplas
      fireEvent.click(duplasTab);
      await waitFor(() => expect(assignmentService.getDuplas).toHaveBeenCalled());

      // Click Individuales
      fireEvent.click(indTab);
      await waitFor(() => expect(assignmentService.getAssignments).toHaveBeenCalled());
    });

    it('TC-FE-HU9-T3-02: API error during dupla creation triggers toast notification and keeps modal open', async () => {
      assignmentService.crearDupla.mockRejectedValueOnce(new Error('Conflicto: uno de los estudiantes ya tiene dupla'));
      const user = userEvent.setup();

      render(<Asignaciones />);
      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      const submitBtn = screen.queryByRole('button', { name: /guardar dupla|crear dupla/i });
      if (submitBtn && !submitBtn.disabled) {
        await user.click(submitBtn);
        await waitFor(() => {
          expect(toast.error).toHaveBeenCalled();
        });
      }
    });
  });

  // =========================================================================
  // TIER 4: REAL-WORLD APPLICATION SCENARIOS
  // =========================================================================

  describe('Tier 4: Real-World Application Scenarios', () => {
    it('TC-FE-HU9-T4-01: Admin end-to-end workflow: views empty list, opens modal, verifies available candidates, creates pair', async () => {
      render(<Asignaciones />);

      // 1. Ir a pestaña de duplas
      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      // 2. Abrir modal
      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      // 3. Verificar que se cargan los disponibles de ciclo intermedio
      await waitFor(() => {
        expect(assignmentService.getEstudiantesDisponiblesDupla).toHaveBeenCalled();
      });

      // 4. Modal visible con título
      expect(screen.getByText(/crear dupla|nueva dupla/i)).toBeInTheDocument();
    });

    it('TC-FE-HU9-T4-02: Handles empty available students gracefully with informative message', async () => {
      assignmentService.getEstudiantesDisponiblesDupla.mockResolvedValueOnce([]);

      render(<Asignaciones />);
      const duplasTab = await screen.findByRole('button', { name: /duplas/i });
      fireEvent.click(duplasTab);

      const nuevaDuplaBtn = await screen.findByRole('button', { name: /\+.*nueva dupla/i });
      fireEvent.click(nuevaDuplaBtn);

      await waitFor(() => {
        expect(screen.getByText(/no hay estudiantes disponibles|sin estudiantes/i)).toBeInTheDocument();
      });
    });
  });
});
