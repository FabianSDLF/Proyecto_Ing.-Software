import { useState, useMemo } from 'react';
import Spinner from './Spinner';

/**
 * Reusable DataTable with sorting, pagination, and empty state
 */
export default function DataTable({
  columns = [],
  data = [],
  keyExtractor = (item, index) => item?.id ?? index,
  isLoading = false,
  emptyMessage = 'No se encontraron registros.',
  pageSize = 10,
  pagination = true,
  className = '',
}) {
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });
  const [currentPage, setCurrentPage] = useState(1);

  // Sorting
  const sortedData = useMemo(() => {
    if (!sortConfig.key) return data;
    const sorted = [...data].sort((a, b) => {
      const aVal = sortConfig.key.split('.').reduce((acc, part) => acc && acc[part], a);
      const bVal = sortConfig.key.split('.').reduce((acc, part) => acc && acc[part], b);
      if (aVal === bVal) return 0;
      if (aVal === null || aVal === undefined) return 1;
      if (bVal === null || bVal === undefined) return -1;
      if (typeof aVal === 'string') {
        return sortConfig.direction === 'asc'
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal);
      }
      return sortConfig.direction === 'asc' ? aVal - bVal : bVal - aVal;
    });
    return sorted;
  }, [data, sortConfig]);

  // Pagination
  const totalPages = Math.ceil(sortedData.length / pageSize) || 1;
  const paginatedData = useMemo(() => {
    if (!pagination) return sortedData;
    const start = (currentPage - 1) * pageSize;
    return sortedData.slice(start, start + pageSize);
  }, [sortedData, currentPage, pageSize, pagination]);

  const handleSort = (columnKey, isSortable) => {
    if (!isSortable) return;
    setSortConfig((prev) => {
      if (prev.key === columnKey) {
        return {
          key: columnKey,
          direction: prev.direction === 'asc' ? 'desc' : 'asc',
        };
      }
      return { key: columnKey, direction: 'asc' };
    });
  };

  return (
    <div className={`bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden ${className}`}>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200 text-left">
          <thead className="bg-gray-50">
            <tr>
              {columns.map((col) => {
                const isSortable = !!col.sortable;
                const colKey = col.accessor || col.key || col.header;
                const isSorted = sortConfig.key === colKey;
                return (
                  <th
                    key={colKey}
                    scope="col"
                    onClick={() => handleSort(colKey, isSortable)}
                    className={`px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider ${
                      col.align === 'center' ? 'text-center' : col.align === 'right' ? 'text-right' : 'text-left'
                    } ${isSortable ? 'cursor-pointer select-none hover:bg-gray-100 transition-colors' : ''} ${
                      col.className || ''
                    }`}
                  >
                    <div className={`inline-flex items-center gap-1 ${col.align === 'right' ? 'justify-end' : ''}`}>
                      <span>{col.header}</span>
                      {isSortable && (
                        <span className="text-gray-400">
                          {isSorted ? (sortConfig.direction === 'asc' ? ' ↑' : ' ↓') : ' ↕'}
                        </span>
                      )}
                    </div>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-100">
            {isLoading ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-12 text-center text-gray-500">
                  <Spinner size="md" />
                  <p className="mt-2 text-sm">Cargando información...</p>
                </td>
              </tr>
            ) : paginatedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-12 text-center text-gray-500 text-sm">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              paginatedData.map((row, index) => (
                <tr key={keyExtractor(row, index)} className="hover:bg-gray-50 transition-colors">
                  {columns.map((col) => {
                    const colKey = col.accessor || col.key;
                    const nestedValue = colKey ? colKey.split('.').reduce((acc, part) => acc && acc[part], row) : undefined;
                    return (
                      <td
                        key={colKey || col.header}
                        className={`px-6 py-4 whitespace-nowrap text-sm ${
                          col.align === 'center' ? 'text-center' : col.align === 'right' ? 'text-right' : 'text-left'
                        } ${col.cellClassName || 'text-gray-700'}`}
                      >
                        {col.render ? col.render(row) : nestedValue}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      {pagination && !isLoading && sortedData.length > pageSize && (
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 bg-gray-50 text-sm text-gray-600">
          <div>
            Mostrando <span className="font-semibold">{(currentPage - 1) * pageSize + 1}</span> a{' '}
            <span className="font-semibold">
              {Math.min(currentPage * pageSize, sortedData.length)}
            </span>{' '}
            de <span className="font-semibold">{sortedData.length}</span> resultados
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
              disabled={currentPage === 1}
              className="px-3 py-1.5 rounded-lg border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer"
            >
              Anterior
            </button>
            <span className="px-2 font-medium">
              Página {currentPage} de {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
              disabled={currentPage === totalPages}
              className="px-3 py-1.5 rounded-lg border border-gray-300 bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
