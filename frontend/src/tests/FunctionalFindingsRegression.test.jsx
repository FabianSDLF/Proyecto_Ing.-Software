import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import chatbotService from '../services/chatbotService';
import { formatEvaluationTimestamp, hasOfficialRubricScale } from '../utils/evaluationFormat';
import EvaluationForm from '../components/EvaluationForm';
import RubricIndicators from '../components/RubricIndicators';
import Portafolio from '../pages/Portafolio';
import NuevaPlanificacion from '../pages/NuevaPlanificacion';
import CargaMasiva from '../pages/CargaMasiva';
import MisEstudiantes from '../pages/MisEstudiantes';
import { AuthContext } from '../context/AuthContext';
import apiClient from '../services/apiClient';
import portfolioService from '../services/portfolioService';
import userService from '../services/userService';
import assignmentService from '../services/assignmentService';
import evaluationService from '../services/evaluationService';

vi.mock('react-hot-toast', () => {
  const toastFn = vi.fn();
  toastFn.success = vi.fn();
  toastFn.error = vi.fn();
  toastFn.loading = vi.fn(() => 'loading-id');
  toastFn.dismiss = vi.fn();
  return { default: toastFn, toast: toastFn };
});

describe('Regresiones funcionales F01-F14 (Frontend)', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('F01: sendMessage y evaluateLessonPlan envían cabecera Authorization y distinguen 401', async () => {
    localStorage.setItem('token', 'jwt-demo-token');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ response: 'ok' }),
    });

    await chatbotService.sendMessage('Consulta de fracciones');
    expect(fetchSpy).toHaveBeenCalledWith(
      expect.stringContaining('/api/chat'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer jwt-demo-token',
          'Content-Type': 'application/json',
        }),
      })
    );

    await chatbotService.evaluateLessonPlan({ schoolGrade: '1° Medio', mathAxis: 'NUMEROS' });
    expect(fetchSpy).toHaveBeenCalledWith(
      expect.stringContaining('/api/evaluar-planificacion'),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer jwt-demo-token',
          'Content-Type': 'application/json',
        }),
      })
    );

    fetchSpy.mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({}),
    });
    await expect(chatbotService.sendMessage('Sin sesión')).rejects.toThrow(/Sesión inválida o expirada/i);
  });

  it('F03 y F04: Portafolio en modo supervisión carga ficha para encabezado y coordinación regresa a /reportes', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({
      id: 7,
      status: 'ACTIVA',
      student: { fullName: 'Camila Rojas' },
      subject: { name: 'Práctica Profesional II' },
      center: { name: 'Liceo Bicentenario' },
    });
    vi.spyOn(portfolioService, 'obtenerEvidenciasPorAsignacion').mockResolvedValue([]);
    vi.spyOn(portfolioService, 'obtenerPlanificacionesPorAsignacion').mockResolvedValue([]);
    vi.spyOn(portfolioService, 'obtenerDocumentosFinalesPorAsignacion').mockResolvedValue([]);

    render(
      <AuthContext.Provider value={{ user: { email: 'coord@ubb.cl' }, userRole: 'Coordinador de practicas' }}>
        <MemoryRouter initialEntries={['/portafolio/estudiante/7']}>
          <Routes>
            <Route path="/portafolio/estudiante/:assignmentId" element={<Portafolio isReadOnly />} />
            <Route path="/reportes" element={<div>Vista Reportes</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText(/Estudiante: Camila Rojas/i)).toBeInTheDocument();
      expect(screen.getByText(/Práctica Profesional II • Liceo Bicentenario/i)).toBeInTheDocument();
    });

    const backButton = screen.getByRole('button', { name: /Volver a Reportes/i });
    fireEvent.click(backButton);
    await waitFor(() => {
      expect(screen.getByText('Vista Reportes')).toBeInTheDocument();
    });
  });

  it('F05: Evaluaciones históricas no muestran escala 2-7 ni "/ 7 pts"', () => {
    const historicalEval = {
      id: 10,
      type: 'DESEMPENO_AULA',
      score: 6.8,
      isFinalized: true,
      rubricVersion: null,
    };
    expect(hasOfficialRubricScale(historicalEval)).toBe(false);

    render(
      <EvaluationForm
        existingEvaluation={historicalEval}
        activeRubric={null}
        isLocked
        rubricProgress={null}
        handleSetAllIndicators={() => {}}
        displayedIndicators={[
          { criterionCode: 'H1', dimension: 'General', text: 'Dominio disciplinar', points: 0.8 },
        ]}
        indicatorPoints={{ H1: 0.8 }}
        handleSetIndicatorPoint={() => {}}
        justifications={{}}
        onJustificationChange={() => {}}
      />
    );

    expect(screen.getByText(/Registro histórico sin escala oficial documentada/i)).toBeInTheDocument();
    expect(screen.getByText(/0\.8 pts \(histórico\)/i)).toBeInTheDocument();
    expect(screen.queryByText(/\/ 7 pts/i)).not.toBeInTheDocument();
  });

  it('F05: RubricIndicators mantiene "/ 7 pts" para evaluaciones oficiales vigentes', () => {
    render(
      <RubricIndicators
        indicators={[{ criterionCode: 'I1', dimension: 'A', text: 'Planifica clase', points: 6 }]}
        points={{ I1: 6 }}
        readOnly
        maxIndicatorPoints={7}
      />
    );
    expect(screen.getByText('6 / 7 pts')).toBeInTheDocument();
  });

  it('F08 y F09: CargaMasiva muestra instrucciones de 4 columnas y detalle de errores parciales', async () => {
    vi.spyOn(userService, 'getRoles').mockResolvedValue([{ name: 'Estudiante' }]);
    vi.spyOn(userService, 'uploadBatch').mockResolvedValue({
      message: 'Importación parcial: 1 creados, 0 actualizados y 1 rechazados.',
      totalRecords: 2,
      createdCount: 1,
      updatedCount: 0,
      failureCount: 1,
      errors: ['Fila 3 (bad@ubb.cl): Rol no encontrado en el sistema: Invalido'],
    });

    const { container } = render(
      <MemoryRouter>
        <CargaMasiva />
      </MemoryRouter>
    );

    expect(screen.getByText(/Nombre, Correo, Contraseña, Rol/i)).toBeInTheDocument();
    const input = container.querySelector('input[type="file"]');
    const file = new File(['Nombre,Correo,Contraseña,Rol\nA,a@ubb.cl,123,Estudiante\n'], 'usuarios.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });
    fireEvent.click(screen.getByRole('button', { name: /Procesar Usuarios/i }));

    await waitFor(() => {
      expect(screen.getByText(/Importación parcial/i)).toBeInTheDocument();
      expect(screen.getByText(/Fila 3 \(bad@ubb\.cl\)/i)).toBeInTheDocument();
    });
  });

  it('F10: MisEstudiantes deshabilita evaluación cuando asignatura no tiene pautas profesionales y conserva histórica', async () => {
    vi.spyOn(assignmentService, 'getByTutor').mockResolvedValue([
      {
        id: 101,
        student: { fullName: 'Alumno Sin Pautas', email: 'sin@ubb.cl' },
        subject: { name: 'Práctica Intermedia', professionalRubrics: false },
        center: { name: 'Escuela 1' },
      },
      {
        id: 102,
        student: { fullName: 'Alumno Con Histórica', email: 'con@ubb.cl' },
        subject: { name: 'Práctica Intermedia', professionalRubrics: false },
        center: { name: 'Escuela 2' },
      },
    ]);
    vi.spyOn(evaluationService, 'getByAssignment').mockImplementation(async (id) => {
      if (id === 102) {
        return [{ id: 50, type: 'TUTOR_ESP_1', isFinalized: true, score: 6.5 }];
      }
      return [];
    });
    vi.spyOn(evaluationService, 'getRubrics').mockResolvedValue([]);

    render(
      <AuthContext.Provider value={{ user: { id: 9, email: 'tutor@ubb.cl' }, userRole: 'Tutor' }}>
        <MemoryRouter>
          <MisEstudiantes />
        </MemoryRouter>
      </AuthContext.Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Alumno Sin Pautas')).toBeInTheDocument();
    });

    const disabledBtn = screen.getByRole('button', { name: /Sin pautas configuradas/i });
    expect(disabledBtn).toBeDisabled();

    const historicalBtn = screen.getByRole('button', { name: /Ver Oficial/i });
    expect(historicalBtn).toBeEnabled();
  });

  it('F11: NuevaPlanificacion asocia etiquetas accesibles a los seis campos de planificación', () => {
    render(
      <MemoryRouter>
        <NuevaPlanificacion />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/Objetivo de Aprendizaje Basal \(OA\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Objetivo de la Clase \(Específico\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Objetivo Actitudinal \(OAT\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Inicio/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Desarrollo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Cierre/i)).toBeInTheDocument();
  });

  it('F12: formatEvaluationTimestamp convierte UTC con sufijo Z a hora de America/Santiago', () => {
    const formatted = formatEvaluationTimestamp('2026-11-24T22:48:10.473535Z');
    expect(formatted).toContain('19:48');
    expect(formatted).not.toContain('22:48');
  });
});
