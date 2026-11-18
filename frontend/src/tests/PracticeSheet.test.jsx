import { render, screen, fireEvent } from '@testing-library/react';
import { describe, test, expect, vi, beforeEach } from 'vitest';
import PracticeSheet from '../components/PracticeSheet';
import apiClient from '../services/apiClient';

vi.mock('../services/apiClient', () => ({ default: { get: vi.fn() } }));

describe('Ficha de práctica y semestres archivados', () => {
  beforeEach(() => vi.clearAllMocks());

  test('shows fullName and complete centre details and keeps archived practices in history', async () => {
    apiClient.get.mockResolvedValue([
      { id: 1, status: 'ACTIVA', period: { name: '2026-2', status: 'ACTIVO' }, subject: { name: 'Profesional', professor: { fullName: 'Profesor UBB', email: 'p@example.test' } },
        collaborator: { fullName: 'Colaboradora Real', email: 'c@example.test' }, center: { name: 'Colegio Actual', rbd: '123', district: 'Chillán', address: 'Calle 1' } },
      { id: 2, status: 'ACTIVA', period: { name: '2025-2', status: 'CERRADO' }, center: { name: 'Colegio Histórico' } },
    ]);
    render(<PracticeSheet />);
    expect(await screen.findByText('Colegio Actual')).toBeInTheDocument();
    expect(screen.getByText('Profesor Colaborador: Colaboradora Real')).toBeInTheDocument();
    expect(screen.getByText('RBD: 123')).toBeInTheDocument();
    expect(screen.queryByText('Colegio Histórico')).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'historial' } });
    expect(screen.getByText('Colegio Histórico')).toBeInTheDocument();
  });

  test('uses the protected assignment endpoint for supervised portfolios', async () => {
    apiClient.get.mockResolvedValue({ id: 7, status: 'ACTIVA', center: { name: 'Centro supervisado' } });
    render(<PracticeSheet assignmentId="7" />);
    expect(await screen.findByText('Centro supervisado')).toBeInTheDocument();
    expect(apiClient.get).toHaveBeenCalledWith('/api/asignaciones/7/ficha', expect.any(Object));
  });
});
