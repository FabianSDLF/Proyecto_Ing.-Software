import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Button from '../../../frontend/src/components/ui/Button';

describe('Button UI Component', () => {
  it('renders button with children text', () => {
    render(<Button>Guardar Planificación</Button>);
    const button = screen.getByRole('button', { name: /guardar planificación/i });
    expect(button).toBeInTheDocument();
    expect(button).toHaveAttribute('type', 'button');
  });

  it('handles custom button type (e.g., submit)', () => {
    render(<Button type="submit">Iniciar Sesión</Button>);
    const button = screen.getByRole('button', { name: /iniciar sesión/i });
    expect(button).toHaveAttribute('type', 'submit');
  });

  describe('Click Handling', () => {
    it('fires onClick callback when clicked', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      render(<Button onClick={handleClick}>Hacer Clic</Button>);

      const button = screen.getByRole('button', { name: /hacer clic/i });
      await user.click(button);

      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('does not trigger onClick when disabled={true}', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      render(<Button disabled onClick={handleClick}>Botón Deshabilitado</Button>);

      const button = screen.getByRole('button', { name: /botón deshabilitado/i });
      expect(button).toBeDisabled();

      await user.click(button);
      expect(handleClick).not.toHaveBeenCalled();
    });

    it('does not trigger onClick when isLoading={true}', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();
      render(<Button isLoading onClick={handleClick}>Cargando</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
      expect(button).toHaveAttribute('aria-busy', 'true');

      await user.click(button);
      expect(handleClick).not.toHaveBeenCalled();
    });
  });

  describe('Variants & Styling', () => {
    it('applies primary variant classes by default', () => {
      render(<Button>Primario</Button>);
      const button = screen.getByRole('button', { name: /primario/i });
      expect(button.className).toContain('bg-orange-600');
    });

    it('applies secondary variant classes', () => {
      render(<Button variant="secondary">Secundario</Button>);
      const button = screen.getByRole('button', { name: /secundario/i });
      expect(button.className).toContain('bg-blue-600');
    });

    it('applies danger variant classes', () => {
      render(<Button variant="danger">Eliminar</Button>);
      const button = screen.getByRole('button', { name: /eliminar/i });
      expect(button.className).toContain('bg-red-600');
    });

    it('applies success variant classes', () => {
      render(<Button variant="success">Aprobar</Button>);
      const button = screen.getByRole('button', { name: /aprobar/i });
      expect(button.className).toContain('bg-green-600');
    });

    it('applies outline variant classes', () => {
      render(<Button variant="outline">Cancelar</Button>);
      const button = screen.getByRole('button', { name: /cancelar/i });
      expect(button.className).toContain('border-gray-300');
    });

    it('applies ghost variant classes', () => {
      render(<Button variant="ghost">Fantasma</Button>);
      const button = screen.getByRole('button', { name: /fantasma/i });
      expect(button.className).toContain('shadow-none');
    });
  });

  describe('Sizes & Dimensions', () => {
    it('applies sm size classes', () => {
      render(<Button size="sm">Pequeño</Button>);
      const button = screen.getByRole('button', { name: /pequeño/i });
      expect(button.className).toContain('text-xs');
      expect(button.className).toContain('px-3');
    });

    it('applies md size classes by default', () => {
      render(<Button>Mediano</Button>);
      const button = screen.getByRole('button', { name: /mediano/i });
      expect(button.className).toContain('text-sm');
      expect(button.className).toContain('px-4');
    });

    it('applies lg size classes', () => {
      render(<Button size="lg">Grande</Button>);
      const button = screen.getByRole('button', { name: /grande/i });
      expect(button.className).toContain('text-base');
      expect(button.className).toContain('px-6');
    });

    it('applies fullWidth class (w-full) when fullWidth={true}', () => {
      render(<Button fullWidth>Ancho Completo</Button>);
      const button = screen.getByRole('button', { name: /ancho completo/i });
      expect(button.className).toContain('w-full');
    });
  });

  describe('Loading State and Icons', () => {
    it('renders spinner and custom loadingText when isLoading={true}', () => {
      render(<Button isLoading loadingText="Procesando entrega...">Subir</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
      expect(button).toHaveTextContent('Procesando entrega...');
      expect(button.querySelector('svg')).toBeInTheDocument(); // Spinner svg
    });

    it('renders spinner and original children if loadingText is not provided', () => {
      render(<Button isLoading>Guardar</Button>);
      const button = screen.getByRole('button');
      expect(button).toHaveTextContent('Guardar');
      expect(button.querySelector('svg')).toBeInTheDocument();
    });

    it('renders left and right icons when not loading', () => {
      render(
        <Button
          leftIcon={<span data-testid="left-icon">Left</span>}
          rightIcon={<span data-testid="right-icon">Right</span>}
        >
          Con Iconos
        </Button>
      );

      expect(screen.getByTestId('left-icon')).toBeInTheDocument();
      expect(screen.getByTestId('right-icon')).toBeInTheDocument();
      expect(screen.getByText('Con Iconos')).toBeInTheDocument();
    });

    it('hides left and right icons while isLoading is true', () => {
      render(
        <Button
          isLoading
          leftIcon={<span data-testid="left-icon">Left</span>}
          rightIcon={<span data-testid="right-icon">Right</span>}
        >
          Cargando
        </Button>
      );

      expect(screen.queryByTestId('left-icon')).not.toBeInTheDocument();
      expect(screen.queryByTestId('right-icon')).not.toBeInTheDocument();
    });
  });
});
