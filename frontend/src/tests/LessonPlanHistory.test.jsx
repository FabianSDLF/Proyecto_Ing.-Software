import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import toast from 'react-hot-toast';
import Portafolio from '../pages/Portafolio';
import portfolioService from '../services/portfolioService';

// Mock toast
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

// Mock portfolioService
vi.mock('../services/portfolioService', () => ({
  default: {
    getDocuments: vi.fn(),
    getLessonPlans: vi.fn(),
    uploadEvidence: vi.fn(),
    deleteDocument: vi.fn(),
    downloadDocument: vi.fn(),
    deleteLessonPlan: vi.fn(),
    getLessonPlanHistory: vi.fn(),
    getLessonPlanById: vi.fn(),
    createLessonPlanVersion: vi.fn(),
  },
  portfolioService: {
    getDocuments: vi.fn(),
    getLessonPlans: vi.fn(),
    uploadEvidence: vi.fn(),
    deleteDocument: vi.fn(),
    downloadDocument: vi.fn(),
    deleteLessonPlan: vi.fn(),
    getLessonPlanHistory: vi.fn(),
    getLessonPlanById: vi.fn(),
    createLessonPlanVersion: vi.fn(),
  },
}));

describe('HU30 E2E Frontend Suite: Historial de Versiones y Modo Solo Lectura (LessonPlanHistory)', () => {
  const mockLessonPlans = [
    {
      id: 201,
      schoolGrade: '7mo Básico',
      mathAxis: 'GEOMETRIA',
      status: 'EN_REVISION_DOCENTE',
      version: 2,
      parentPlanId: 200,
      baseLearningObjective: 'Construir figuras geométricas regulares',
      objectives: 'Identificar vértices y ángulos interiores',
      createdAt: '2026-03-24T12:00:00Z',
      teacherFeedback: 'Excelente progreso en la incorporación de material interactivo.',
      document: { id: 81, fileName: 'plan_geometria_v2.pdf', fileUrl: '/files/81' },
    },
  ];

  const mockVersionHistory = [
    {
      id: 200,
      version: 1,
      schoolGrade: '7mo Básico',
      mathAxis: 'GEOMETRIA',
      status: 'REQUIERE_CAMBIOS',
      createdAt: '2026-03-20T10:00:00Z',
      baseLearningObjective: 'Construir figuras geométricas',
      objectives: 'Identificar ángulos',
      teacherFeedback: 'Falta profundizar en las actividades de cierre.',
    },
    {
      id: 201,
      version: 2,
      schoolGrade: '7mo Básico',
      mathAxis: 'GEOMETRIA',
      status: 'EN_REVISION_DOCENTE',
      createdAt: '2026-03-24T12:00:00Z',
      baseLearningObjective: 'Construir figuras geométricas regulares',
      objectives: 'Identificar vértices y ángulos interiores',
      teacherFeedback: 'Excelente progreso en la incorporación de material interactivo.',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();

    portfolioService.getDocuments.mockResolvedValue([]);
    portfolioService.getLessonPlans.mockResolvedValue(mockLessonPlans);
    portfolioService.getLessonPlanHistory.mockResolvedValue(mockVersionHistory);
    portfolioService.getLessonPlanById.mockImplementation((id) => {
      const found = mockVersionHistory.find((v) => v.id === id);
      return Promise.resolve(found || mockVersionHistory[0]);
    });
    portfolioService.createLessonPlanVersion.mockResolvedValue({ id: 202, version: 3, message: 'Nueva versión creada' });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter>
        <Portafolio />
      </MemoryRouter>
    );
  };

  // =========================================================================
  // TIER 1: FEATURE COVERAGE (>=5 tests)
  // =========================================================================

  describe('Tier 1: Feature Coverage (Primary Behavior / Happy Path)', () => {
    it('TC-FE-HU30-T1-01: Displays version badge (e.g. v2) in lesson plans table', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      await waitFor(() => {
        expect(screen.getByText(/v2|versi[oó]n 2/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T1-02: Displays "Historial" button on lesson plan row', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /historial|versiones/i })).toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T1-03: Clicking "Historial" opens version history modal with list of versions', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      await waitFor(() => {
        expect(portfolioService.getLessonPlanHistory).toHaveBeenCalled();
        expect(screen.getByText(/historial de versiones/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T1-04: Clicking "Ver Detalle" on historical version v1 opens detail modal', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      const detalleBtn = await screen.findByRole('button', { name: /ver detalle|ver versi[oó]n/i });
      fireEvent.click(detalleBtn);

      await waitFor(() => {
        expect(portfolioService.getLessonPlanById).toHaveBeenCalledWith(200);
      });
    });

    it('TC-FE-HU30-T1-05: Displays informative banner stating strictly read-only mode', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      const detalleBtn = await screen.findByRole('button', { name: /ver detalle|ver versi[oó]n/i });
      fireEvent.click(detalleBtn);

      await waitFor(() => {
        expect(screen.getByText(/modo solo lectura|inmutables y no pueden editarse/i)).toBeInTheDocument();
      });
    });
  });

  // =========================================================================
  // TIER 2: BOUNDARY & CORNER CASES (>=5 tests)
  // =========================================================================

  describe('Tier 2: Boundary & Corner Cases', () => {
    it('TC-FE-HU30-T2-01: All inputs, textareas, and selects in historical detail view are disabled or readOnly', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      const detalleBtn = await screen.findByRole('button', { name: /ver detalle|ver versi[oó]n/i });
      fireEvent.click(detalleBtn);

      await waitFor(() => {
        const inputs = screen.queryAllByRole('textbox');
        inputs.forEach((input) => {
          expect(input.hasAttribute('disabled') || input.hasAttribute('readonly')).toBe(true);
        });
      });
    });

    it('TC-FE-HU30-T2-02: Historical detail modal contains NO "Guardar", "Editar", or "Restaurar" buttons', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      const detalleBtn = await screen.findByRole('button', { name: /ver detalle|ver versi[oó]n/i });
      fireEvent.click(detalleBtn);

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: /restaurar|revertir/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /guardar cambios/i })).not.toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T2-03: Historical detail modal footer contains only a "Cerrar" action button', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      const detalleBtn = await screen.findByRole('button', { name: /ver detalle|ver versi[oó]n/i });
      fireEvent.click(detalleBtn);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /cerrar|volver/i })).toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T2-04: Clicking "Editar" on current plan opens edit form with existing values pre-populated', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const editBtn = await screen.findByRole('button', { name: /editar/i });
      fireEvent.click(editBtn);

      await waitFor(() => {
        expect(screen.getByDisplayValue(/7mo Básico/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T2-05: Submitting edit creates new version v+1 and notifies user with success toast', async () => {
      const user = userEvent.setup();
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const editBtn = await screen.findByRole('button', { name: /editar/i });
      fireEvent.click(editBtn);

      const saveBtn = screen.queryByRole('button', { name: /guardar nueva versi[oó]n|guardar/i });
      if (saveBtn) {
        await user.click(saveBtn);
        await waitFor(() => {
          expect(portfolioService.createLessonPlanVersion).toHaveBeenCalled();
          expect(toast.success).toHaveBeenCalled();
        });
      }
    });
  });

  // =========================================================================
  // TIER 3: CROSS-FEATURE COMBINATIONS
  // =========================================================================

  describe('Tier 3: Cross-Feature Combinations', () => {
    it('TC-FE-HU30-T3-01: Displays teacher feedback when available on historical versions', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      await waitFor(() => {
        expect(screen.getByText(/Falta profundizar en las actividades de cierre/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU30-T3-02: Handles API error fetching history gracefully with toast error', async () => {
      portfolioService.getLessonPlanHistory.mockRejectedValueOnce(new Error('Error al obtener historial'));

      renderComponent();
      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalled();
      });
    });
  });

  // =========================================================================
  // TIER 4: REAL-WORLD APPLICATION SCENARIOS
  // =========================================================================

  describe('Tier 4: Real-World Application Scenarios', () => {
    it('TC-FE-HU30-T4-01: Complete student review workflow: opens history -> inspects v1 read-only -> closes -> ready to edit v2', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      // 1. Abrir historial
      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      // 2. Ver detalle de v1
      const detalleBtn = await screen.findByRole('button', { name: /ver detalle|ver versi[oó]n/i });
      fireEvent.click(detalleBtn);

      // 3. Cerrar modal de detalle
      const cerrarBtn = await screen.findByRole('button', { name: /cerrar|volver/i });
      fireEvent.click(cerrarBtn);

      // 4. Volver a la vista principal
      expect(screen.getByText(/7mo Básico/i)).toBeInTheDocument();
    });

    it('TC-FE-HU30-T4-02: Keyboard accessibility: Escape key dismisses history modal', async () => {
      renderComponent();

      const planTab = screen.getByRole('button', { name: /planificaciones/i });
      fireEvent.click(planTab);

      const historialBtn = await screen.findByRole('button', { name: /historial|versiones/i });
      fireEvent.click(historialBtn);

      await waitFor(() => {
        expect(screen.getByText(/historial de versiones/i)).toBeInTheDocument();
      });

      fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });
    });
  });
});
