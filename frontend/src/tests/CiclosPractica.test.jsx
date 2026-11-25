import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import toast from 'react-hot-toast';
import CiclosPractica, { formatSemesters } from '../pages/CiclosPractica';
import cycleService from '../services/cycleService';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
  toast: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('../services/cycleService', () => ({
  default: { getCycles: vi.fn(), getCycle: vi.fn(), updateCycle: vi.fn() },
}));

const CYCLES = [
  {
    id: 1, code: 'INICIAL', name: 'Ciclo Inicial', description: 'Simulaciones en la universidad',
    displayOrder: 1, startSemester: 4, endSemester: 5, subjectCount: 1,
    subjects: [{ id: 10, name: 'Aproximación al sistema escolar', code: 'PEM-401' }],
  },
  {
    id: 2, code: 'INTERMEDIO', name: 'Ciclo Intermedio', description: 'Duplas en el aula',
    displayOrder: 2, startSemester: 6, endSemester: 7, subjectCount: 0, subjects: [],
  },
  {
    id: 3, code: 'AVANZADO', name: 'Ciclo Avanzado', description: null,
    displayOrder: 3, startSemester: null, endSemester: null, subjectCount: 0, subjects: [],
  },
];

describe('HU5 - CiclosPractica', () => {
  beforeEach(() => {
    vi.mocked(cycleService.getCycles).mockResolvedValue(CYCLES);
    vi.mocked(cycleService.updateCycle).mockReset();
    vi.mocked(toast.success).mockClear();
    vi.mocked(toast.error).mockClear();
  });

  it('formatSemesters muestra rangos en números romanos', () => {
    expect(formatSemesters(4, 5)).toBe('Semestres IV a V');
    expect(formatSemesters(8, 8)).toBe('Semestre VIII');
    expect(formatSemesters(null, null)).toBe('Semestres sin definir');
  });

  it('muestra los tres ciclos con sus asignaturas', async () => {
    render(<CiclosPractica />);

    const inicial = await screen.findByTestId('cycle-card-INICIAL');
    expect(within(inicial).getByText('Ciclo Inicial')).toBeInTheDocument();
    expect(within(inicial).getByText('Semestres IV a V')).toBeInTheDocument();
    expect(within(inicial).getByText('Aproximación al sistema escolar')).toBeInTheDocument();

    expect(screen.getByTestId('cycle-card-INTERMEDIO')).toBeInTheDocument();
    const avanzado = screen.getByTestId('cycle-card-AVANZADO');
    expect(within(avanzado).getByText('Semestres sin definir')).toBeInTheDocument();
    expect(within(avanzado).getByText('Sin descripción')).toBeInTheDocument();
  });

  it('edita un ciclo y actualiza la tarjeta', async () => {
    const user = userEvent.setup();
    vi.mocked(cycleService.updateCycle).mockResolvedValue({
      ...CYCLES[2], name: 'Ciclo Avanzado PEM', startSemester: 8, endSemester: 9,
    });

    render(<CiclosPractica />);
    const avanzado = await screen.findByTestId('cycle-card-AVANZADO');
    await user.click(within(avanzado).getByRole('button', { name: 'Editar ciclo' }));

    const nombre = screen.getByLabelText(/Nombre/);
    await user.clear(nombre);
    await user.type(nombre, 'Ciclo Avanzado PEM');
    await user.type(screen.getByLabelText('Semestre de inicio'), '8');
    await user.type(screen.getByLabelText('Semestre de término'), '9');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(cycleService.updateCycle).toHaveBeenCalledWith('AVANZADO', expect.objectContaining({
      name: 'Ciclo Avanzado PEM', startSemester: '8', endSemester: '9',
    })));
    expect(toast.success).toHaveBeenCalled();
    expect(await screen.findByText('Ciclo Avanzado PEM')).toBeInTheDocument();
    expect(screen.getByText('Semestres VIII a IX')).toBeInTheDocument();
  });

  it('valida en el cliente que el semestre de inicio no supere al de término', async () => {
    const user = userEvent.setup();
    render(<CiclosPractica />);
    const intermedio = await screen.findByTestId('cycle-card-INTERMEDIO');
    await user.click(within(intermedio).getByRole('button', { name: 'Editar ciclo' }));

    const inicio = screen.getByLabelText('Semestre de inicio');
    await user.clear(inicio);
    await user.type(inicio, '9');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Debe ser mayor o igual al semestre de inicio')).toBeInTheDocument();
    expect(cycleService.updateCycle).not.toHaveBeenCalled();
  });

  it('muestra un error si falla la carga', async () => {
    vi.mocked(cycleService.getCycles).mockRejectedValue(new Error('Servidor caído'));
    render(<CiclosPractica />);
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith('Servidor caído'));
    expect(screen.getByText('No hay ciclos registrados.')).toBeInTheDocument();
  });
});
