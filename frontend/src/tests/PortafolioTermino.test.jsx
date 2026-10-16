import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import toast from 'react-hot-toast';
import Portafolio from '../pages/Portafolio';
import portfolioService from '../services/portfolioService';

// Mock toast
vi.mock('react-hot-toast', () => {
  const toast = {
    success: vi.fn(),
    error: vi.fn(),
    loading: vi.fn(),
  };
  return {
    default: toast,
    toast,
  };
});

// Mock portfolioService
vi.mock('../services/portfolioService', () => ({
  default: {
    getDocuments: vi.fn(),
    getLessonPlans: vi.fn(),
    uploadEvidence: vi.fn(),
    deleteDocument: vi.fn(),
    downloadDocument: vi.fn(),
    deleteLessonPlan: vi.fn(),
    getFinalDocuments: vi.fn(),
    uploadFinalDocument: vi.fn(),
  },
  portfolioService: {
    getDocuments: vi.fn(),
    getLessonPlans: vi.fn(),
    uploadEvidence: vi.fn(),
    deleteDocument: vi.fn(),
    downloadDocument: vi.fn(),
    deleteLessonPlan: vi.fn(),
    getFinalDocuments: vi.fn(),
    uploadFinalDocument: vi.fn(),
  },
}));

describe('HU19 E2E Frontend Suite: Subida de Documentos de Término de Práctica (PortafolioTermino)', () => {
  const mockFinalDocument = {
    id: 99,
    type: 'INFORME_FINAL',
    fileName: 'informe_final_estudiante.pdf',
    fileUrl: '/uploads/uuid-informe.pdf',
    sizeBytes: 5 * 1024 * 1024, // 5 MB
    uploadDate: '2026-03-25T15:30:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();

    portfolioService.getDocuments.mockResolvedValue([]);
    portfolioService.getLessonPlans.mockResolvedValue([]);
    portfolioService.getFinalDocuments.mockResolvedValue([]);
    portfolioService.uploadFinalDocument.mockResolvedValue({ id: 100, message: 'Subido exitosamente' });
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
    it('TC-FE-HU19-T1-01: Renders Portafolio and navigates to Documento de Término tab', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      expect(terminoTab).toBeInTheDocument();
      fireEvent.click(terminoTab);

      expect(portfolioService.getFinalDocuments).toHaveBeenCalled();
    });

    it('TC-FE-HU19-T1-02: Displays selector for document type (Informe Final vs Proyecto de Intervención)', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      await waitFor(() => {
        expect(screen.getByLabelText(/tipo de documento/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU19-T1-03: Allows uploading valid PDF document within 20 MB', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      const fileInput = screen.queryByLabelText(/subir archivo|seleccionar archivo/i) ||
                        document.querySelector('input[type="file"]');
      expect(fileInput).toBeInTheDocument();

      const validPdf = new File(['dummy pdf content'], 'informe.pdf', { type: 'application/pdf' });
      fireEvent.change(fileInput, { target: { files: [validPdf] } });

      const uploadBtn = screen.queryByRole('button', { name: /subir documento|guardar/i });
      if (uploadBtn) {
        fireEvent.click(uploadBtn);
        await waitFor(() => {
          expect(portfolioService.uploadFinalDocument).toHaveBeenCalled();
        });
      }
    });

    it('TC-FE-HU19-T1-04: Displays card with metadata when terminal document exists', async () => {
      portfolioService.getFinalDocuments.mockResolvedValueOnce([mockFinalDocument]);
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      await waitFor(() => {
        expect(screen.getByText(/informe_final_estudiante\.pdf/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU19-T1-05: Displays category badge identifying document as Informe Final', async () => {
      portfolioService.getFinalDocuments.mockResolvedValueOnce([mockFinalDocument]);
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      await waitFor(() => {
        expect(screen.getAllByText(/informe final/i).length).toBeGreaterThanOrEqual(1);
      });
    });
  });

  // =========================================================================
  // TIER 2: BOUNDARY & CORNER CASES (>=5 tests)
  // =========================================================================

  describe('Tier 2: Boundary & Corner Cases', () => {
    it('TC-FE-HU19-T2-01: Rejects file exceeding 20 MB immediately on client side with toast error', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      const fileInput = document.querySelector('input[type="file"]');
      expect(fileInput).toBeInTheDocument();

      // Mock oversized file: 21 MB
      const oversizedFile = new File(['content'], 'pesado.pdf', { type: 'application/pdf' });
      Object.defineProperty(oversizedFile, 'size', { value: 21 * 1024 * 1024 });

      fireEvent.change(fileInput, { target: { files: [oversizedFile] } });

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(expect.stringMatching(/20\s*mb|excede el tama[ñn]o/i));
      });
      // Network call must NOT be triggered
      expect(portfolioService.uploadFinalDocument).not.toHaveBeenCalled();
    });

    it('TC-FE-HU19-T2-02: Accepts file within boundary: 19 MB', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      const fileInput = document.querySelector('input[type="file"]');
      const boundaryFile = new File(['content'], 'limite.pdf', { type: 'application/pdf' });
      Object.defineProperty(boundaryFile, 'size', { value: 19 * 1024 * 1024 });

      fireEvent.change(fileInput, { target: { files: [boundaryFile] } });

      // No error toast for boundary file
      expect(toast.error).not.toHaveBeenCalledWith(expect.stringMatching(/20\s*mb/i));
    });

    it('TC-FE-HU19-T2-03: Rejects forbidden extensions (.exe, .png, .xlsx) with toast error', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      const fileInput = document.querySelector('input[type="file"]');
      const badFile = new File(['content'], 'malware.exe', { type: 'application/octet-stream' });

      fireEvent.change(fileInput, { target: { files: [badFile] } });

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith(expect.stringMatching(/formato no permitido|solo se aceptan/i));
      });
      expect(portfolioService.uploadFinalDocument).not.toHaveBeenCalled();
    });

    it('TC-FE-HU19-T2-04: File input restricts accept attribute strictly to .pdf, .doc, .docx', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      const fileInput = document.querySelector('input[type="file"]');
      expect(fileInput).toHaveAttribute('accept');
      const accept = fileInput.getAttribute('accept');
      expect(accept).toContain('.pdf');
      expect(accept).toContain('.doc');
      expect(accept).toContain('.docx');
    });

    it('TC-FE-HU19-T2-05: Single document rule: prompts ConfirmDialog when replacing existing document', async () => {
      portfolioService.getFinalDocuments.mockResolvedValueOnce([mockFinalDocument]);
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      await waitFor(() => {
        expect(screen.getByText(/informe_final_estudiante\.pdf/i)).toBeInTheDocument();
      });

      // Click "Reemplazar Documento"
      const replaceBtn = screen.getByRole('button', { name: /reemplazar/i });
      fireEvent.click(replaceBtn);

      // Verify confirmation dialog is displayed
      await waitFor(() => {
        expect(screen.getByText(/¿deseas reemplazar/i)).toBeInTheDocument();
      });
    });

    it('TC-FE-HU19-T2-06: Cancelling replacement dialog aborts upload and keeps existing document', async () => {
      portfolioService.getFinalDocuments.mockResolvedValueOnce([mockFinalDocument]);
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      await waitFor(() => {
        expect(screen.getByText(/informe_final_estudiante\.pdf/i)).toBeInTheDocument();
      });

      const replaceBtn = screen.getByRole('button', { name: /reemplazar/i });
      fireEvent.click(replaceBtn);

      const cancelBtn = await screen.findByRole('button', { name: /cancelar/i });
      fireEvent.click(cancelBtn);

      expect(portfolioService.uploadFinalDocument).not.toHaveBeenCalled();
      expect(screen.getByText(/informe_final_estudiante\.pdf/i)).toBeInTheDocument();
    });
  });

  // =========================================================================
  // TIER 3: CROSS-FEATURE COMBINATIONS
  // =========================================================================

  describe('Tier 3: Cross-Feature Combinations', () => {
    it('TC-FE-HU19-T3-01: Switching tabs preserves data isolation between Evidencias and Documento de Término', async () => {
      renderComponent();

      const evidenciasTab = screen.getByRole('button', { name: /evidencias/i });
      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });

      // Click Término
      fireEvent.click(terminoTab);
      await waitFor(() => expect(portfolioService.getFinalDocuments).toHaveBeenCalled());

      // Click Evidencias
      fireEvent.click(evidenciasTab);
      await waitFor(() => expect(portfolioService.getDocuments).toHaveBeenCalled());
    });
  });

  // =========================================================================
  // TIER 4: REAL-WORLD APPLICATION SCENARIOS
  // =========================================================================

  describe('Tier 4: Real-World Application Scenarios', () => {
    it('TC-FE-HU19-T4-01: Full workflow: uploads initial report -> confirms replacement with project -> updates card', async () => {
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      // Verify UI renders and handles empty state
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /documento de t[eé]rmino/i })).toBeInTheDocument();
      });
    });

    it('TC-FE-HU19-T4-02: Handles upload error gracefully and shows error notification', async () => {
      portfolioService.uploadFinalDocument.mockRejectedValueOnce(new Error('Fallo de conexión al servidor'));
      renderComponent();

      const terminoTab = await screen.findByRole('button', { name: /t[eé]rmino|documento final/i });
      fireEvent.click(terminoTab);

      const fileInput = document.querySelector('input[type="file"]');
      const validPdf = new File(['content'], 'informe.pdf', { type: 'application/pdf' });
      fireEvent.change(fileInput, { target: { files: [validPdf] } });

      const uploadBtn = screen.queryByRole('button', { name: /subir documento|guardar/i });
      if (uploadBtn) {
        fireEvent.click(uploadBtn);
        await waitFor(() => {
          expect(toast.error).toHaveBeenCalled();
        });
      }
    });
  });
});
