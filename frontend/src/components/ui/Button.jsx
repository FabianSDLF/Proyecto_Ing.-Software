import Spinner from './Spinner';

/**
 * Reusable Button Component
 * Supports variants, sizes, loading state with spinner, and accessible attributes
 */
export default function Button({
  children,
  type = 'button',
  variant = 'primary',
  size = 'md',
  isLoading = false,
  loadingText,
  disabled = false,
  fullWidth = false,
  leftIcon,
  rightIcon,
  className = '',
  onClick,
  ...props
}) {
  const baseClasses =
    'inline-flex items-center justify-center font-bold rounded-xl transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 disabled:shadow-none';

  const variantClasses = {
    primary: 'bg-orange-600 text-white hover:bg-orange-700 hover:shadow-md focus:ring-orange-500',
    secondary: 'bg-blue-600 text-white hover:bg-blue-700 hover:shadow-md focus:ring-blue-500',
    danger: 'bg-red-600 text-white hover:bg-red-700 hover:shadow-md focus:ring-red-500',
    success: 'bg-green-600 text-white hover:bg-green-700 hover:shadow-md focus:ring-green-500',
    outline: 'border border-gray-300 text-gray-700 bg-white hover:bg-gray-50 focus:ring-orange-500',
    ghost: 'text-gray-700 hover:bg-gray-100 shadow-none focus:ring-gray-400',
  }[variant] || 'bg-orange-600 text-white hover:bg-orange-700 focus:ring-orange-500';

  const sizeClasses = {
    sm: 'text-xs px-3 py-1.5 gap-1.5',
    md: 'text-sm px-4 py-2.5 gap-2',
    lg: 'text-base px-6 py-3 gap-2.5',
  }[size] || 'text-sm px-4 py-2.5 gap-2';

  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      onClick={onClick}
      className={`${baseClasses} ${variantClasses} ${sizeClasses} ${widthClass} ${className}`}
      {...props}
    >
      {isLoading ? (
        <>
          <Spinner size="sm" color={variant === 'outline' || variant === 'ghost' ? 'gray' : 'white'} />
          <span>{loadingText || children}</span>
        </>
      ) : (
        <>
          {leftIcon && <span className="inline-flex shrink-0">{leftIcon}</span>}
          <span>{children}</span>
          {rightIcon && <span className="inline-flex shrink-0">{rightIcon}</span>}
        </>
      )}
    </button>
  );
}
