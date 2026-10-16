/**
 * Accessible Spinner Component
 * WAI-ARIA status indicator with customizable size and color
 */
export default function Spinner({
  size = 'md',
  color = 'orange',
  className = '',
  label = 'Cargando...',
}) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-10 h-10',
    xl: 'w-16 h-16',
  }[size] || 'w-6 h-6';

  const colorClasses = {
    orange: 'text-orange-600',
    blue: 'text-blue-600',
    white: 'text-white',
    gray: 'text-gray-500',
  }[color] || 'text-orange-600';

  return (
    <div role="status" className={`inline-flex items-center justify-center ${className}`}>
      <svg
        className={`animate-spin ${sizeClasses} ${colorClasses}`}
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        ></circle>
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        ></path>
      </svg>
      <span className="sr-only">{label}</span>
    </div>
  );
}
