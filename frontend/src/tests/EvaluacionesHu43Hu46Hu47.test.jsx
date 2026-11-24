import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import MisEstudiantes from '../pages/MisEstudiantes';
import StudentEvaluationsPanel from '../components/StudentEvaluationsPanel';
import assignmentService from '../services/assignmentService';
import evaluationService from '../services/evaluationService';
import apiClient from '../services/apiClient';

const mockAuthState = {
  user: { id: 6, fullName: 'Mg. Claudia Arriagada', email: 'tutor@ubb.cl' },
  userRole: 'Tutor',
};

vi.mock('../context/AuthContext', () => ({
  useAuth: () => mockAuthState,
}));

vi.mock('react-hot-toast', () => {
  const toast = {
    success: vi.fn(),
    error: vi.fn(),
    loading: vi.fn(),
  };
  return { default: toast, toast };
});

vi.mock('../services/assignmentService', () => ({
  default: {
    getByTutor: vi.fn(),
    getByCollaborator: vi.fn(),
    getByProfessor: vi.fn(),
  },
}));

vi.mock('../services/evaluationService', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    default: {
      ...actual.default,
      getTutorRubrics: vi.fn(),
      getCollaboratorRubrics: vi.fn(),
      getRubrics: vi.fn(),
      getByAssignment: vi.fn(),
      getById: vi.fn(),
      createOrUpdate: vi.fn(),
      finalize: vi.fn(),
    },
  };
});

vi.mock('../services/apiClient', () => ({
  BASE_URL: '',
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const sampleTutorRubrics = [
  {
    type: 'TUTOR_ESP_1',
    name: 'Primera observación de Especialidad (Tutor)',
    role: 'TUTOR',
    area: 'Especialidad',
    visit: 1,
    rubricVersion: '2025-2-v1',
    scale: [2, 3, 4, 5, 6, 7],
    indicatorCount: 2,
    maxPoints: 14,
    indicators: [
      { id: 'A1', code: 'A1', criterionCode: 'A1', dimension: 'A', order: 1, text: 'Comprende las bases curriculares.' },
      { id: 'B1', code: 'B1', criterionCode: 'B1', dimension: 'B', order: 2, text: 'Establece un clima respetuoso.' },
    ],
  },
  {
    type: 'TUTOR_ORI_1',
    name: 'Primera observación de Orientación (Tutor)',
    role: 'TUTOR',
    area: 'Orientación',
    visit: 1,
    rubricVersion: '2025-2-v1',
    scale: [2, 3, 4, 5, 6, 7],
    indicatorCount: 2,
    maxPoints: 14,
    indicators: [
      { id: 'A1', code: 'A1', criterionCode: 'A1', dimension: 'A', order: 1, text: 'Comunica los propósitos de orientación.' },
      { id: 'B1', code: 'B1', criterionCode: 'B1', dimension: 'B', order: 2, text: 'Escucha activamente al curso.' },
    ],
  },
];

const sampleAssignment = {
  id: 10,
  status: 'ACTIVA',
  student: { id: 9, fullName: 'Valentina Castro', email: 'estudiante@ubb.cl' },
  subject: { id: 20, name: 'Práctica Profesional' },
  center: { id: 3, name: 'Colegio Salesiano' },
  period: { id: 1, name: 'Segundo Semestre 2026', status: 'ACTIVO' },
};

describe('HU43, HU46 y HU47 - Suite Frontend de Evaluaciones', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthState.user = { id: 6, fullName: 'Mg. Claudia Arriagada', email: 'tutor@ubb.cl' };
    mockAuthState.userRole = 'Tutor';
    assignmentService.getByTutor.mockResolvedValue([sampleAssignment]);
    evaluationService.getTutorRubrics.mockResolvedValue(sampleTutorRubrics);
    evaluationService.getByAssignment.mockResolvedValue([]);
  });

  it('HU43 y HU46: El Tutor accede a sus pautas, guarda borrador completo, revisa resumen en FinalizeEvaluationDialog y firma/publica oficialmente', async () => {
    const savedDraft = {
      id: 101,
      practiceAssignmentId: 10,
      type: 'TUTOR_ESP_1',
      instrumentName: 'Primera observación de Especialidad (Tutor)',
      area: 'Especialidad',
      visit: 1,
      rubricVersion: '2025-2-v1',
      revision: 1,
      totalPoints: 14,
      maxPoints: 14,
      score: 7.0,
      isFinalized: false,
      qualitativeComments: 'Excelente conducción didáctica.',
      observedIndicators: 2,
      totalIndicators: 2,
      criteria: [
        { criterionCode: 'A1', dimension: 'A', order: 1, text: 'Comprende las bases curriculares.', points: 7 },
        { criterionCode: 'B1', dimension: 'B', order: 2, text: 'Establece un clima respetuoso.', points: 7 },
      ],
    };

    const finalizedEval = {
      ...savedDraft,
      revision: 2,
      isFinalized: true,
      finalizedAt: '2026-11-18T20:00:00Z',
      finalizedByName: 'Mg. Claudia Arriagada',
      finalizedByEmail: 'tutor@ubb.cl',
      finalizedByRole: 'Tutor',
      snapshotHash: 'a'.repeat(64),
    };

    evaluationService.createOrUpdate.mockResolvedValue(savedDraft);
    evaluationService.finalize.mockResolvedValue(finalizedEval);

    render(
      <MemoryRouter>
        <MisEstudiantes />
      </MemoryRouter>
    );

    const pautaSelect = await screen.findByLabelText(/Pauta de evaluación/i);
    expect(pautaSelect).toBeInTheDocument();
    expect(evaluationService.getTutorRubrics).toHaveBeenCalled();

    const evalBtn = await screen.findByRole('button', { name: /Evaluar \(Pauta\)/i });
    fireEvent.click(evalBtn);

    // Completar indicadores usando asignación rápida a 7 puntos
    const fillAll7Btn = await screen.findByRole('button', { name: /Todos 7/i });
    fireEvent.click(fillAll7Btn);

    const textarea = screen.getByPlaceholderText(/Escriba sus observaciones/i);
    fireEvent.change(textarea, { target: { value: 'Excelente conducción didáctica.' } });

    const prepareSignBtn = screen.getByRole('button', { name: /Revisar y Firmar Oficialmente/i });
    fireEvent.click(prepareSignBtn);

    await waitFor(() => {
      expect(evaluationService.createOrUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          practiceAssignmentId: 10,
          type: 'TUTOR_ESP_1',
          rubricVersion: '2025-2-v1',
          expectedRevision: null,
          isFinalized: false,
        })
      );
    });

    // Diálogo de confirmación abierto con nota oficial calculada por servidor
    expect(await screen.findByText(/Confirmar Firma Digital y Publicación Oficial/i)).toBeInTheDocument();
    expect(screen.getByTestId('dialog-official-score')).toHaveTextContent('7.0');

    const confirmBtn = screen.getByRole('button', { name: /Confirmar Firma y Publicar/i });
    expect(confirmBtn).toBeDisabled();

    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);
    expect(confirmBtn).not.toBeDisabled();

    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(evaluationService.finalize).toHaveBeenCalledWith(101, {
        expectedRevision: 1,
        confirmed: true,
      });
    });

    // La vista pasa a modo lectura oficial sin botones de edición
    expect(await screen.findByText(/Firmada y Publicada/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Guardar Borrador/i })).not.toBeInTheDocument();
  });

  it('HU46: Ante un conflicto 409 al firmar, recupera el estado oficial actual del servidor', async () => {
    const savedDraft = {
      id: 102,
      practiceAssignmentId: 10,
      type: 'TUTOR_ESP_1',
      rubricVersion: '2025-2-v1',
      revision: 1,
      totalPoints: 14,
      maxPoints: 14,
      score: 7.0,
      isFinalized: false,
      qualitativeComments: 'Observación',
      criteria: [
        { criterionCode: 'A1', points: 7 },
        { criterionCode: 'B1', points: 7 },
      ],
    };

    const alreadyClosedOnServer = {
      ...savedDraft,
      revision: 2,
      isFinalized: true,
      finalizedByName: 'Mg. Claudia Arriagada',
      finalizedAt: '2026-11-18T20:05:00Z',
    };

    evaluationService.createOrUpdate.mockResolvedValue(savedDraft);
    const conflictErr = new Error('Conflicto de revisión');
    conflictErr.status = 409;
    evaluationService.finalize.mockRejectedValue(conflictErr);

    render(
      <MemoryRouter>
        <MisEstudiantes />
      </MemoryRouter>
    );

    const evalBtn = await screen.findByRole('button', { name: /Evaluar \(Pauta\)/i });
    fireEvent.click(evalBtn);

    fireEvent.click(await screen.findByRole('button', { name: /Todos 7/i }));
    fireEvent.change(screen.getByPlaceholderText(/Escriba sus observaciones/i), {
      target: { value: 'Observación' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Revisar y Firmar Oficialmente/i }));

    const checkbox = await screen.findByRole('checkbox');
    fireEvent.click(checkbox);

    evaluationService.getByAssignment.mockResolvedValueOnce([alreadyClosedOnServer]);
    fireEvent.click(screen.getByRole('button', { name: /Confirmar Firma y Publicar/i }));

    await waitFor(() => {
      expect(evaluationService.getByAssignment).toHaveBeenCalledWith(10);
    });
    expect(await screen.findByText(/Firmada y Publicada/i)).toBeInTheDocument();
  });

  it('HU47: El Estudiante consulta sus evaluaciones publicadas, abre el desglose por indicadores y no ve datos de borradores', async () => {
    const publishedEval = {
      id: 201,
      practiceAssignmentId: 10,
      type: 'TUTOR_ESP_1',
      instrumentName: 'Primera observación de Especialidad (Tutor)',
      area: 'Especialidad',
      visit: 1,
      rubricVersion: '2025-2-v1',
      revision: 2,
      totalPoints: 78,
      maxPoints: 133,
      score: 4.0,
      isFinalized: true,
      finalizedAt: '2026-11-18T20:10:00Z',
      finalizedByName: 'Mg. Claudia Arriagada',
      finalizedByRole: 'Tutor',
      qualitativeComments: 'Buen progreso en el manejo de preguntas y tiempos.',
      criteria: [
        { criterionCode: 'A1', dimension: 'A', order: 1, text: 'Comprende las bases curriculares.', points: 4 },
        { criterionCode: 'B1', dimension: 'B', order: 2, text: 'Establece un clima respetuoso.', points: 5 },
      ],
    };

    const leakedDraft = {
      id: 202,
      practiceAssignmentId: 10,
      type: 'COLAB_ESP_1',
      score: 2.5,
      isFinalized: false,
      qualitativeComments: 'COMENTARIO_PRIVADO_BORRADOR',
    };

    apiClient.get.mockResolvedValue([sampleAssignment]);
    evaluationService.getByAssignment.mockResolvedValue([publishedEval, leakedDraft]);
    evaluationService.getById.mockResolvedValue(publishedEval);

    render(
      <MemoryRouter>
        <StudentEvaluationsPanel history={false} />
      </MemoryRouter>
    );

    expect(await screen.findByText(/Primera observación de Especialidad \(Tutor\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Buen progreso en el manejo de preguntas y tiempos\./i)).toBeInTheDocument();
    expect(screen.getByText('4.0')).toBeInTheDocument();
    expect(screen.getByText(/78 \/ 133 pts/i)).toBeInTheDocument();

    // El borrador no publicado jamás se muestra al estudiante
    expect(screen.queryByText(/COMENTARIO_PRIVADO_BORRADOR/i)).not.toBeInTheDocument();

    // Abrir desglose oficial en modo lectura
    const detailBtn = screen.getByRole('button', { name: /Ver Desglose por Indicadores/i });
    fireEvent.click(detailBtn);

    expect(await screen.findByText(/Desglose Oficial de Evaluación Publicada/i)).toBeInTheDocument();
    expect(screen.getByText(/Dimensión A/i)).toBeInTheDocument();
    expect(screen.getByText(/Comprende las bases curriculares\./i)).toBeInTheDocument();
    expect(screen.getByText(/4 \/ 7 pts/i)).toBeInTheDocument();
  });
});
