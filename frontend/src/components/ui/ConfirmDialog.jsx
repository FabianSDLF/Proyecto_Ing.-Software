import Modal from './Modal';
import Button from './Button';

/**
 * Non-blocking Confirm Dialog
 * Resolves FE-19: Replaces window.confirm with an accessible, styled modal
 */
export default function ConfirmDialog({
  isOpen,
  onConfirm,
  onCancel,
  title = '¿Confirmar acción?',
  message = '¿Estás seguro de que deseas continuar? Esta acción no se puede deshacer.',
  confirmText = 'Confirmar',
  cancelText = 'Cancelar',
  variant = 'danger',
  isLoading = false,
}) {
  return (
    <Modal isOpen={isOpen} onClose={onCancel} title={title} size="sm" showCloseButton={!isLoading}>
      <div className="space-y-6">
        <p className="text-gray-600 text-sm leading-relaxed">{message}</p>

        <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
          <Button variant="outline" onClick={onCancel} disabled={isLoading}>
            {cancelText}
          </Button>
          <Button variant={variant} onClick={onConfirm} isLoading={isLoading}>
            {confirmText}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
