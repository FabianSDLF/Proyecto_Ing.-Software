import { useEffect, useLayoutEffect, useRef, useId } from 'react';

const openDialogs = [];

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

  const titleId = useId();
  const descriptionId = useId();
  const closeRef = useRef(onClose);
  useLayoutEffect(() => { closeRef.current = onClose; }, [onClose]);
  useEffect(() => {
    if (!isOpen) return;
    const opener = document.activeElement;
    const token = {};
    openDialogs.push(token);
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialogRef.current?.focus();
    const handleKeyDown = e => {
      if (openDialogs.at(-1) !== token) return;
      if (e.key === 'Escape') { e.stopPropagation(); closeRef.current(); }
      if (e.key === 'Tab') {
        const nodes = [...dialogRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]')].filter(n => !n.hidden);
        const first = nodes[0], last = nodes.at(-1);
        if (!first) { e.preventDefault(); dialogRef.current.focus(); return; }
        if (e.shiftKey && (document.activeElement === first || document.activeElement === dialogRef.current)) { e.preventDefault(); last.focus(); }
        if (!e.shiftKey && (document.activeElement === last || document.activeElement === dialogRef.current)) { e.preventDefault(); first.focus(); }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      openDialogs.splice(openDialogs.indexOf(token), 1);
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = originalOverflow;
      if (opener?.isConnected) opener.focus();
    };
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
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm overflow-hidden"
      onClick={handleBackdropClick}
      aria-modal="true"
      role="dialog"
      aria-labelledby={title ? titleId : undefined}
      aria-describedby={description ? descriptionId : undefined}
    >
      <div
        ref={dialogRef}
        tabIndex={-1}
        className={`relative min-w-0 w-full ${sizeClasses} bg-white rounded-2xl shadow-2xl border border-gray-100 p-4 sm:p-6 md:p-8 my-2 max-h-[calc(100dvh-2rem)] overflow-y-auto ${className}`}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5 border-b pb-4 border-gray-100">
          <div>
            {title && (
              <h2 id={titleId} className="text-2xl font-bold text-gray-900 tracking-tight">
                {title}
              </h2>
            )}
            {description && (
              <p id={descriptionId} className="text-sm text-gray-500 mt-1">
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
