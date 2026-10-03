import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import assignmentService from '../services/assignmentService';
import { DataTable, Badge } from '../components/ui';

export default function MisEstudiantes() {
    const { user, userRole } = useAuth();
    const [asignaciones, setAsignaciones] = useState([]);
    const [loading, setLoading] = useState(true);

    const cargarEstudiantes = useCallback(async (signal) => {
        if (!user) return;
        setLoading(true);

        try {
            let data = [];
            if (userRole === 'Profesor de Asignatura') {
                data = await assignmentService.getByProfessor(user.id, { signal });
            } else if (userRole === 'Profesor Colaborador') {
                data = await assignmentService.getByCollaborator(user.id, { signal });
            } else if (userRole === 'Tutor') {
                data = await assignmentService.getByTutor(user.id, { signal });
            }

            setAsignaciones(Array.isArray(data) ? data : []);
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error('Error al cargar estudiantes:', error);
                toast.error(error.message || 'Error al cargar estudiantes');
            }
        } finally {
            setLoading(false);
        }
    }, [user, userRole]);

    useEffect(() => {
        const controller = new AbortController();
        cargarEstudiantes(controller.signal);
        return () => controller.abort();
    }, [cargarEstudiantes]);

    const columns = useMemo(() => [
        {
            header: 'Estudiante',
            render: (row) => (
                <div>
                    <p className="font-medium text-gray-900">{row.student?.fullName || 'No asignado'}</p>
                    <p className="text-xs text-gray-500">{row.student?.email || ''}</p>
                </div>
            ),
        },
        {
            header: 'Asignatura',
            accessor: 'subject.name',
            className: 'text-gray-700',
        },
        {
            header: 'Centro de Práctica',
            render: (row) => row.center?.name || <span className="text-gray-400 italic">Sin centro</span>,
        },
        {
            header: 'Estado',
            align: 'center',
            render: (row) => (
                <Badge variant={row.status === 'ACTIVA' ? 'success' : 'gray'}>
                    {row.status || 'PENDING'}
                </Badge>
            ),
        },
    ], []);
    const handleDescargarZip = async () => {
        try {
            const token = localStorage.getItem('token');
            const respuesta = await fetch('http://localhost:8080/api/portafolio/download-zip', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (respuesta.ok) {
                const blob = await respuesta.blob();
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', 'informes_finales.zip');
                document.body.appendChild(link);
                link.click();
                link.parentNode.removeChild(link);
            } else {
                alert("Error al descargar el ZIP.");
            }
        } catch (error) {
            console.error("Error al descargar:", error);
        }
    };

    return (
        <div className="p-8 max-w-6xl mx-auto space-y-8">
            <div>
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">Mis Estudiantes</h1>
                    <button
                        onClick={handleDescargarZip}
                        className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow flex items-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                        Descargar Todos los Informes (ZIP)
                    </button>
                </div>
                <p className="text-gray-500 mt-1">Lleva el control y seguimiento de los estudiantes asignados a tu tutela.</p>
            </div>

            <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                <DataTable
                    columns={columns}
                    data={asignaciones}
                    isLoading={loading}
                    emptyMessage="No tienes estudiantes asignados actualmente."
                    keyField="id"
                />
            </div>
        </div>
    );
}
