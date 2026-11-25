import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import GestionAsignaturas from '../pages/GestionAsignaturas';
import subjectService from '../services/subjectService';

vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));
vi.mock('../services/subjectService', () => ({ default: {
  getSubjects: vi.fn(), getPeriods: vi.fn(), createSubject: vi.fn(), updateSubject: vi.fn(),
  createPeriod: vi.fn(), updatePeriod: vi.fn(),
} }));
vi.mock('../services/userService', () => ({ default: { getUsersByRole: vi.fn().mockResolvedValue([]) } }));

const period = { id: 7, name: '2026-2', year: 2026, semesterNumber: 2, status: 'ACTIVO', startDate: '2026-08-01', endDate: '2026-12-15' };

beforeEach(() => {
  vi.clearAllMocks();
  subjectService.getSubjects.mockResolvedValue([{ id: 1, name: 'Práctica profesional', cycle: 'AVANZADO' }]);
  subjectService.getPeriods.mockResolvedValue([period]);
  subjectService.createPeriod.mockResolvedValue({ id: 8 });
  subjectService.updatePeriod.mockResolvedValue(period);
});

async function openPeriods() {
  render(<GestionAsignaturas />);
  fireEvent.click(screen.getByRole('button', { name: 'Periodos / Semestres Académicos' }));
  await screen.findByText('2026-08-01 → 2026-12-15');
}

describe('Regresiones de ciclos y periodos', () => {
  it('permite únicamente Inicial, Intermedio y Avanzado al crear y editar asignaturas', async () => {
    render(<GestionAsignaturas />);
    const cycle = screen.getByRole('combobox', { name: /^Ciclo/ });
    expect(within(cycle).getAllByRole('option').map(option => option.value).filter(Boolean)).toEqual(['INICIAL', 'INTERMEDIO', 'AVANZADO']);
    await screen.findByText('Práctica profesional');
    fireEvent.click(screen.getByRole('button', { name: 'Editar', exact: true }));
    const dialog = screen.getByRole('dialog');
    const editCycle = within(dialog).getByRole('combobox', { name: /^Ciclo/ });
    expect(within(editCycle).getAllByRole('option').map(option => option.value).filter(Boolean)).toEqual(['INICIAL', 'INTERMEDIO', 'AVANZADO']);
    expect(editCycle).toHaveValue('AVANZADO');
  });

  it('no envía un periodo nuevo con fechas invertidas, incluso al enviar el formulario directamente', async () => {
    await openPeriods();
    const start = screen.getByLabelText('Fecha Inicio');
    const end = screen.getByLabelText('Fecha Término');
    fireEvent.change(start, { target: { value: '2027-07-01' } });
    fireEvent.change(end, { target: { value: '2027-03-01' } });
    expect(end).toHaveAttribute('min', '2027-07-01');
    expect(start).toHaveAttribute('max', '2027-03-01');
    expect(end).toHaveAttribute('aria-invalid', 'true');
    fireEvent.submit(end.closest('form'));
    expect(subjectService.createPeriod).not.toHaveBeenCalled();
    expect(subjectService.updatePeriod).not.toHaveBeenCalled();
    expect(screen.getByText(/La fecha de término no puede ser anterior/)).toBeVisible();
  });

  it('rechaza las fechas invertidas al editar y permite guardar una corrección con fechas iguales', async () => {
    await openPeriods();
    fireEvent.click(screen.getByRole('button', { name: 'Editar', exact: true }));
    const end = screen.getByLabelText('Fecha Término');
    fireEvent.change(end, { target: { value: '2026-01-01' } });
    fireEvent.submit(end.closest('form'));
    expect(subjectService.updatePeriod).not.toHaveBeenCalled();
    fireEvent.change(end, { target: { value: '2026-08-01' } });
    fireEvent.submit(end.closest('form'));
    await waitFor(() => expect(subjectService.updatePeriod).toHaveBeenCalledWith(7, expect.objectContaining({ startDate: '2026-08-01', endDate: '2026-08-01' })));
    expect(subjectService.createPeriod).not.toHaveBeenCalled();
  });
});
