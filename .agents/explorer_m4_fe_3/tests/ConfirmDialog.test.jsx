import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ConfirmDialog from '../../../frontend/src/components/ui/ConfirmDialog';

describe('ConfirmDialog UI Component (FE-19 Non-blocking Dialog)', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <ConfirmDialog
        isOpen={false}
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    );

    expect(container.firstChild).toBeNull();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renders default title and message when isOpen is true', () => {
    render(
      <ConfirmDialog
        isOpen={true}
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    );

    expect(screen.getByText('¿Confirmar acción?')).toBeInTheDocument();
    expect(
      screen.getByText('¿Estás seguro de que deseas continuar? Esta acción no se puede deshacer.')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeInTheDocument();
  });

  it('renders custom title, message, confirmText, and cancelText', () => {
    render(
      <ConfirmDialog
        isOpen={true}
        onConfirm={() => {}}
        onCancel={() => {}}
        title="Dar de Baja a Estudiante"
        message="¿Desea desactivar la cuenta del estudiante seleccionado?"
        confirmText="Desactivar"
        cancelText="Volver"
      />
    );

    expect(screen.getByText('Dar de Baja a Estudiante')).toBeInTheDocument();
    expect(screen.getByText('¿Desea desactivar la cuenta del estudiante seleccionado?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Desactivar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Volver' })).toBeInTheDocument();
  });

  describe('Callbacks & User Actions', () => {
    it('triggers onConfirm when clicking confirm button', async () => {
      const user = userEvent.setup();
      const handleConfirm = vi.fn();
      const handleCancel = vi.fn();

      render(
        <ConfirmDialog
          isOpen={true}
          onConfirm={handleConfirm}
          onCancel={handleCancel}
          confirmText="Sí, eliminar"
        />
      );

      const confirmBtn = screen.getByRole('button', { name: 'Sí, eliminar' });
      await user.click(confirmBtn);

      expect(handleConfirm).toHaveBeenCalledTimes(1);
      expect(handleCancel).not.toHaveBeenCalled();
    });

    it('triggers onCancel when clicking cancel button', async () => {
      const user = userEvent.setup();
      const handleConfirm = vi.fn();
      const handleCancel = vi.fn();

      render(
        <ConfirmDialog
          isOpen={true}
          onConfirm={handleConfirm}
          onCancel={handleCancel}
          cancelText="No, cancelar"
        />
      );

      const cancelBtn = screen.getByRole('button', { name: 'No, cancelar' });
      await user.click(cancelBtn);

      expect(handleCancel).toHaveBeenCalledTimes(1);
      expect(handleConfirm).not.toHaveBeenCalled();
    });
  });

  describe('Variants & Styling', () => {
    it('applies danger variant to confirm button by default', () => {
      render(
        <ConfirmDialog
          isOpen={true}
          onConfirm={() => {}}
          onCancel={() => {}}
          confirmText="Eliminar"
        />
      );

      const confirmBtn = screen.getByRole('button', { name: 'Eliminar' });
      expect(confirmBtn.className).toContain('bg-red-600');
    });

    it('supports custom variant like primary or secondary', () => {
      render(
        <ConfirmDialog
          isOpen={true}
          onConfirm={() => {}}
          onCancel={() => {}}
          confirmText="Aprobar Planificación"
          variant="success"
        />
      );

      const confirmBtn = screen.getByRole('button', { name: 'Aprobar Planificación' });
      expect(confirmBtn.className).toContain('bg-green-600');
    });
  });

  describe('Loading State Behavior', () => {
    it('disables cancel button, sets loading on confirm button, and hides close "X" button', () => {
      render(
        <ConfirmDialog
          isOpen={true}
          isLoading={true}
          onConfirm={() => {}}
          onCancel={() => {}}
          confirmText="Borrando..."
          cancelText="Cancelar"
        />
      );

      const cancelBtn = screen.getByRole('button', { name: 'Cancelar' });
      expect(cancelBtn).toBeDisabled();

      const confirmBtn = screen.getByRole('button', { name: 'Borrando...' });
      expect(confirmBtn).toBeDisabled();
      expect(confirmBtn).toHaveAttribute('aria-busy', 'true');

      // The modal close 'X' button should be hidden during loading
      expect(screen.queryByRole('button', { name: /cerrar modal/i })).not.toBeInTheDocument();
    });
  });
});
