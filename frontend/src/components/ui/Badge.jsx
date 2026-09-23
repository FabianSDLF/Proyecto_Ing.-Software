/**
 * Badge / Tag Component
 */
export default function Badge({
  children,
  variant = 'neutral',
  size = 'md',
  className = '',
}) {
  const variantClasses = {
    neutral: 'bg-gray-100 text-gray-800',
    primary: 'bg-orange-100 text-orange-800',
    info: 'bg-blue-100 text-blue-800',
    success: 'bg-green-100 text-green-800',
    warning: 'bg-yellow-100 text-yellow-800',
    danger: 'bg-red-100 text-red-800',
    purple: 'bg-purple-100 text-purple-800',
  }[variant] || 'bg-gray-100 text-gray-800';

  const sizeClasses = {
    sm: 'text-[11px] px-2 py-0.5',
    md: 'text-xs px-2.5 py-1',
  }[size] || 'text-xs px-2.5 py-1';

  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full tracking-wide uppercase ${variantClasses} ${sizeClasses} ${className}`}
    >
      {children}
    </span>
  );
}
