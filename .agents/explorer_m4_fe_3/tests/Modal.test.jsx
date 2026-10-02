import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Modal from '../../../frontend/src/components/ui/Modal';

describe('Modal UI Component (FE-18 Accessible Dialog)', () => {
  beforeEach(() => {
    document.body.style.overflow = '';
  });

  afterEach(() => {
    document.body.style.overflow = '';
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <Modal isOpen={false} onClose={() => {}} title="Título Modal">
        <p>Contenido del modal</p>
      </Modal>
    );

    expect(container.firstChild).toBeNull();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renders dialog with WAI-ARIA role and aria-modal when isOpen is true', () => {
    render(
      <Modal isOpen={true} onClose={() => {}} title="Nueva Asignación">
        <p>Formulario de práctica</p>
      </Modal>
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeInTheDocument();
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText('Nueva Asignación')).toBeInTheDocument();
    expect(screen.getByText('Formulario de práctica')).toBeInTheDocument();
  });

  it('binds aria-labelledby to title heading id', () => {
    render(
      <Modal isOpen={true} onClose={() => {}} title="Confirmación de Portafolio">
        <p>Contenido</p>
      </Modal>
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-labelledby', 'modal-dialog-title');
    const heading = screen.getByRole('heading', { level: 2 });
    expect(heading).toHaveAttribute('id', 'modal-dialog-title');
    expect(heading).toHaveTextContent('Confirmación de Portafolio');
  });

  it('binds aria-describedby to description text id when description prop is provided', () => {
    render(
      <Modal
        isOpen={true}
        onClose={() => {}}
        title="Subir Archivo"
        description="Selecciona un archivo PDF o ZIP no mayor a 10MB"
      >
        <p>Cuerpo</p>
      </Modal>
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-describedby', 'modal-dialog-description');
    expect(screen.getByText(/selecciona un archivo pdf o zip/i)).toHaveAttribute('id', 'modal-dialog-description');
  });

  describe('Dismissal & Keyboard Accessibility (FE-18)', () => {
    it('calls onClose when close button ("X") is clicked', async () => {
      const user = userEvent.setup();
      const handleClose = vi.fn();

      render(
        <Modal isOpen={true} onClose={handleClose} title="Cerrar con X">
          <p>Cuerpo</p>
        </Modal>
      );

      const closeButton = screen.getByRole('button', { name: /cerrar modal/i });
      await user.click(closeButton);

      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('hides close button when showCloseButton={false}', () => {
      render(
        <Modal isOpen={true} onClose={() => {}} title="Sin Botón X" showCloseButton={false}>
          <p>Cuerpo</p>
        </Modal>
      );

      expect(screen.queryByRole('button', { name: /cerrar modal/i })).not.toBeInTheDocument();
    });

    it('calls onClose when Escape key is pressed', () => {
      const handleClose = vi.fn();

      render(
        <Modal isOpen={true} onClose={handleClose} title="Escape Test">
          <p>Cuerpo</p>
        </Modal>
      );

      fireEvent.keyDown(document, { key: 'Escape', code: 'Escape' });
      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when clicking outside on the backdrop overlay', () => {
      const handleClose = vi.fn();

      render(
        <Modal isOpen={true} onClose={handleClose} title="Backdrop Test">
          <p>Cuerpo</p>
        </Modal>
      );

      const backdrop = screen.getByRole('dialog');
      // Fire click where target is the outer container itself
      fireEvent.click(backdrop);

      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('does NOT call onClose when clicking inside the modal content box', () => {
      const handleClose = vi.fn();

      render(
        <Modal isOpen={true} onClose={handleClose} title="Inner Click Test">
          <div data-testid="modal-inner">Contenido Interior</div>
        </Modal>
      );

      const innerContent = screen.getByTestId('modal-inner');
      fireEvent.click(innerContent);

      expect(handleClose).not.toHaveBeenCalled();
    });
  });

  describe('Body Scroll Lock', () => {
    it('sets document.body.style.overflow = "hidden" while open and restores on unmount', () => {
      document.body.style.overflow = 'auto';

      const { unmount } = render(
        <Modal isOpen={true} onClose={() => {}} title="Scroll Lock Test">
          <p>Cuerpo</p>
        </Modal>
      );

      expect(document.body.style.overflow).toBe('hidden');

      unmount();
      expect(document.body.style.overflow).toBe('auto');
    });
  });

  describe('Size Variants', () => {
    it('applies corresponding size classes (sm, lg, full)', () => {
      const { rerender } = render(
        <Modal isOpen={true} onClose={() => {}} title="Size SM" size="sm">
          <p>Contenido</p>
        </Modal>
      );

      expect(screen.getByRole('dialog').firstElementChild.className).toContain('max-w-md');

      rerender(
        <Modal isOpen={true} onClose={() => {}} title="Size LG" size="lg">
          <p>Contenido</p>
        </Modal>
      );
      expect(screen.getByRole('dialog').firstElementChild.className).toContain('max-w-2xl');

      rerender(
        <Modal isOpen={true} onClose={() => {}} title="Size Full" size="full">
          <p>Contenido</p>
        </Modal>
      );
      expect(screen.getByRole('dialog').firstElementChild.className).toContain('max-w-6xl');
    });
  });
});
