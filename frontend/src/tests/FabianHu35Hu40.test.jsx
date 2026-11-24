import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import StatisticsPanel from '../components/StatisticsPanel';
import Ayuda from '../pages/Ayuda';
import Modal from '../components/ui/Modal';
import apiClient from '../services/apiClient';

vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ userRole: 'Tutor' }) }));
vi.mock('../services/apiClient', () => ({ default: { get: vi.fn(), download: vi.fn() } }));
const report = { students: 1, assignments: 2, centersUsed: 1, publishedEvaluations: 0, configuredInstruments: 8, pendingEvaluations: 8, periods: [], subjects: [], gradesByInstrument: [], series: [], latestPlanStates: {}, detail: [] };

describe('HU35 y HU40', () => {
  beforeEach(() => vi.resetAllMocks());
  it('exports exactly the selected filters and handles an empty panel', async () => {
    apiClient.get.mockResolvedValue(report);
    render(<MemoryRouter><StatisticsPanel /></MemoryRouter>);
    expect(await screen.findByText('Sin notas publicadas.')).toBeVisible();
    fireEvent.change(screen.getByLabelText('Periodo'), { target: { value: 'TODOS' } });
    fireEvent.change(screen.getByLabelText('Ciclo'), { target: { value: 'AVANZADO' } });
    await waitFor(() => expect(apiClient.get).toHaveBeenLastCalledWith('/api/reportes/estadisticas', expect.objectContaining({ params: { history: true, cycle: 'AVANZADO' } })));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Descargar CSV' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Descargar CSV' }));
    expect(apiClient.download).toHaveBeenCalledWith('/api/portafolio/export-csv', 'estadisticas_practicas.csv', { params: { history: true, cycle: 'AVANZADO' } });
  });
  it('shows a load error and allows retrying', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('Sin conexión')).mockResolvedValue(report);
    render(<MemoryRouter><StatisticsPanel /></MemoryRouter>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Sin conexión');
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Sin notas publicadas.')).toBeVisible();
  });
  it('keeps role tutorials available when no coordinator is configured', async () => {
    apiClient.get.mockResolvedValue({ configured: false });
    render(<MemoryRouter><Ayuda /></MemoryRouter>);
    expect(await screen.findByText(/aún no está configurado/)).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Registrar y firmar una evaluación' })).toBeVisible();
    expect(screen.queryByRole('link', { name: /@/ })).not.toBeInTheDocument();
  });
  it('shows only the explicitly configured contact', async () => {
    apiClient.get.mockResolvedValue({ configured: true, name: 'Coordinación', email: 'coord@example.test', phone: '' });
    render(<MemoryRouter><Ayuda /></MemoryRouter>);
    expect(await screen.findByRole('link', { name: 'coord@example.test' })).toHaveAttribute('href', 'mailto:coord@example.test');
  });
  it('traps focus and Escape affects only the topmost modal', () => {
    const closeOuter=vi.fn(), closeInner=vi.fn();
    const { unmount }=render(<><Modal isOpen title="Exterior" onClose={closeOuter}><button>Primero</button></Modal><Modal isOpen title="Interior" onClose={closeInner}><button>Último</button></Modal></>);
    fireEvent.keyDown(window,{key:'Tab'});
    expect(screen.getAllByRole('button', { name: 'Cerrar modal' })[1]).toHaveFocus();
    fireEvent.keyDown(window,{key:'Tab',shiftKey:true});
    expect(screen.getByRole('button', { name: 'Último' })).toHaveFocus();
    fireEvent.keyDown(window,{key:'Escape'});
    expect(closeInner).toHaveBeenCalledTimes(1); expect(closeOuter).not.toHaveBeenCalled();
    unmount();
  });
});
