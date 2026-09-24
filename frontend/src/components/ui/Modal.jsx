import { useEffect, useRef } from 'react';

/**
 * Accessible Modal Dialog Component
 * Resolves FE-18:
 * - WAI-ARIA role="dialog", aria-modal="true"
 * - Escape key dismiss handler
 * - Body scroll lock on open
 * - Backdrop click to close
 */
export default function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer,
  size = 'md',
  showCloseButton = true,
  className = '',
}) {
  const dialogRef = useRef(null);

  // Keyboard navigation: Escape key closes modal
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Lock body scroll when modal is active
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
    full: 'max-w-6xl',
  }[size] || 'max-w-lg';

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm overflow-y-auto"
      onClick={handleBackdropClick}
      aria-modal="true"
      role="dialog"
      aria-labelledby={title ? 'modal-dialog-title' : undefined}
      aria-describedby={description ? 'modal-dialog-description' : undefined}
    >
      <div
        ref={dialogRef}
        className={`relative w-full ${sizeClasses} bg-white rounded-2xl shadow-2xl border border-gray-100 p-6 md:p-8 my-8 transform transition-all animate-in fade-in zoom-in-95 ${className}`}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5 border-b pb-4 border-gray-100">
          <div>
            {title && (
              <h2 id="modal-dialog-title" className="text-2xl font-bold text-gray-900 tracking-tight">
                {title}
              </h2>
            )}
            {description && (
              <p id="modal-dialog-description" className="text-sm text-gray-500 mt-1">
                {description}
              </p>
            )}
          </div>
          {showCloseButton && (
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors ml-4 cursor-pointer"
              aria-label="Cerrar modal"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          )}
        </div>

        {/* Content */}
        <div>{children}</div>

        {/* Footer */}
        {footer && (
          <div className="mt-6 pt-5 border-t border-gray-100">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
