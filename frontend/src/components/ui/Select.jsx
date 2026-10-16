import { useId } from 'react';

/**
 * Accessible Select Component
 */
export default function Select({
  label,
  error,
  helperText,
  id,
  name,
  options = [],
  placeholder = 'Seleccione una opción',
  required = false,
  disabled = false,
  className = '',
  selectClassName = '',
  value,
  onChange,
  children,
  ...props
}) {
  const generatedId = useId();
  const selectId = id || generatedId;
  const errorId = `${selectId}-error`;
  const helperId = `${selectId}-helper`;

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label htmlFor={selectId} className="block text-sm font-medium text-gray-700 mb-1.5">
          {label} {required && <span className="text-red-500" aria-hidden="true">*</span>}
        </label>
      )}
      <select
        id={selectId}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        aria-invalid={!!error}
        aria-describedby={error ? errorId : helperText ? helperId : undefined}
        className={`w-full border rounded-xl p-3 outline-none transition-all text-gray-900 bg-white disabled:bg-gray-100 disabled:text-gray-500 disabled:cursor-not-allowed ${
          error
            ? 'border-red-500 focus:ring-2 focus:ring-red-200 focus:border-red-500'
            : 'border-gray-300 focus:ring-2 focus:ring-orange-500 focus:border-orange-500'
        } ${selectClassName}`}
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.length > 0
          ? options.map((opt) => {
              const val = typeof opt === 'object' ? opt.value : opt;
              const lbl = typeof opt === 'object' ? opt.label : opt;
              const dis = typeof opt === 'object' ? opt.disabled : false;
              return (
                <option key={val} value={val} disabled={dis}>
                  {lbl}
                </option>
              );
            })
          : children}
      </select>
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
