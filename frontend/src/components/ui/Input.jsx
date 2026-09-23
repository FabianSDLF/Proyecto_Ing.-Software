import { useId } from 'react';

/**
 * Accessible Input Component
 * Integrates label, helper text, error messages, and WAI-ARIA descriptions
 */
export default function Input({
  label,
  error,
  helperText,
  id,
  name,
  type = 'text',
  required = false,
  disabled = false,
  className = '',
  inputClassName = '',
  icon,
  ...props
}) {
  const generatedId = useId();
  const inputId = id || generatedId;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1.5">
          {label} {required && <span className="text-red-500" aria-hidden="true">*</span>}
        </label>
      )}
      <div className="relative">
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
            {icon}
          </div>
        )}
        <input
          id={inputId}
          name={name}
          type={type}
          required={required}
          disabled={disabled}
          aria-invalid={!!error}
          aria-describedby={error ? errorId : helperText ? helperId : undefined}
          className={`w-full border rounded-xl p-3 outline-none transition-all text-gray-900 bg-white disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed ${
            icon ? 'pl-10' : ''
          } ${
            error
              ? 'border-red-500 focus:ring-2 focus:ring-red-200 focus:border-red-500'
              : 'border-gray-300 focus:ring-2 focus:ring-orange-500 focus:border-orange-500'
          } ${inputClassName}`}
          {...props}
        />
      </div>
      {error && (
        <p id={errorId} className="mt-1.5 text-xs font-semibold text-red-600">
          {error}
        </p>
      )}
      {!error && helperText && (
        <p id={helperId} className="mt-1 text-xs text-gray-500">
          {helperText}
        </p>
      )}
    </div>
  );
}
