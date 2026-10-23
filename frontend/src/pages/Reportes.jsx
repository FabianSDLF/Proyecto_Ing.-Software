import { useState, useEffect } from 'react';
import { BASE_URL } from '../services/apiClient';

export default function Reportes() {
    const [asignaciones, setAsignaciones] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [filtroPeriodo, setFiltroPeriodo] = useState('TODOS');

    useEffect(() => {
        const fetchDatos = async () => {
            try {
                const token = localStorage.getItem('token');
                const respuesta = await fetch(`${BASE_URL}/api/asignaciones`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (respuesta.ok) {
                    const datos = await respuesta.json();
                    setAsignaciones(datos);
                }
            } catch (error) {
                console.error("Error al cargar reporte", error);
            } finally {
                setCargando(false);
            }
        };
        fetchDatos();
    }, []);

    const handleExportarExcel = async () => {
        try {
            const token = localStorage.getItem('token');
            const respuesta = await fetch(`${BASE_URL}/api/portafolio/export-csv`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (respuesta.ok) {
                const blob = await respuesta.blob();
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', 'reporte_institucional.csv');
                document.body.appendChild(link);
                link.click();
                link.parentNode.removeChild(link);
            } else {
                alert("Error al exportar los datos.");
            }
        } catch (error) {
            console.error("Error al exportar:", error);
        }
    };

    const periodosUnicos = [...new Set(asignaciones.map(a => a.period?.name).filter(Boolean))];
    const asignacionesFiltradas = filtroPeriodo === 'TODOS'
        ? asignaciones
        : asignaciones.filter(a => a.period?.name === filtroPeriodo);

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Reporte Institucional</h1>
                    <p className="text-gray-500 mt-2">Visualiza el estado de las prácticas y exporta la data centralizada.</p>
                </div>
                <button
                    onClick={handleExportarExcel}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-lg shadow-lg flex items-center gap-2"
                >
                    📊 Descargar CSV / Excel
                </button>
            </div>

            <div className="bg-white rounded-2xl shadow border border-gray-100 overflow-hidden">
                <div className="flex items-center space-x-3 px-6 pt-5 pb-4 border-b border-gray-100 bg-gray-50">
                    <label className="font-semibold text-gray-700">Filtrar por Periodo:</label>
                    <select
                        className="border border-gray-300 bg-white text-gray-800 rounded-lg py-1.5 px-3 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer w-auto shadow-sm"
                        value={filtroPeriodo}
                        onChange={(e) => setFiltroPeriodo(e.target.value)}
                    >
                        <option value="TODOS">Todos los periodos</option>
                        {periodosUnicos.map(periodo => (
                            <option key={periodo} value={periodo}>{periodo}</option>
                        ))}
                    </select>
                </div>

                <div className="overflow-x-auto">
                    {cargando ? <p className="p-6 text-center text-gray-500">Cargando datos...</p> : (
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-100">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase">Estudiante</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase">Asignatura</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase">Centro (Colegio)</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase">Tutor / Colaborador</th>
                                <th className="px-6 py-3 text-center text-xs font-medium text-gray-600 uppercase">Estado</th>
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {asignacionesFiltradas.length > 0 ? (
                                asignacionesFiltradas.map(fila => (
                                    <tr key={fila.id} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 text-sm font-medium text-gray-900">{fila.student?.fullName || 'N/A'}</td>
                                        <td className="px-6 py-4 text-sm text-gray-500">
                                            {fila.subject?.name || 'N/A'}<br/>
                                            <span className="text-xs text-gray-400">{fila.period?.name || ''}</span>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-gray-500">{fila.center?.name || 'N/A'}</td>
                                        <td className="px-6 py-4 text-sm text-gray-500">
                                            <strong>T:</strong> {fila.tutor?.fullName || 'N/A'}<br/>
                                            <strong>C:</strong> {fila.collaborator?.fullName || 'N/A'}
                                        </td>
                                        <td className="px-6 py-4 text-sm text-center">
                                                <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                                                    {fila.status || 'N/A'}
                                                </span>
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                                        No hay datos para el periodo seleccionado.
                                    </td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}