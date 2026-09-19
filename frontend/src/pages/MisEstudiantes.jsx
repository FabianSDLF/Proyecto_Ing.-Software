import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

export default function MisEstudiantes() {
    const [asignaciones, setAsignaciones] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        cargarEstudiantes();
    }, []);

    const cargarEstudiantes = async () => {
        setLoading(true);
        try {
            const usuarioGuardado = localStorage.getItem('usuarioActivo');
            if (usuarioGuardado) {
                const usuario = JSON.parse(usuarioGuardado);
                if (!['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(usuario?.role?.name)) {
                    toast.error("No tienes permiso para ver esta pǭgina.");
                    navigate('/dashboard');
                    return;
                }
                const res = await fetch(`http://localhost:8080/api/profesor/${usuario.id}/estudiantes`, { headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') } });
                if (res.ok) {
                    const data = await res.json();
                    setAsignaciones(data);
                } else {
                    toast.error("Error al cargar estudiantes");
                }
            }
        } catch (error) {
            toast.error("Error de conexión");
        }
        setLoading(false);
    };

    return (
        <div className="p-8 max-w-6xl mx-auto space-y-8">
            <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Mis Estudiantes</h1>

            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100">
                <div className="flex items-center mb-6">
                    <div className="bg-blue-100 p-3 rounded-full mr-4">
                        <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-gray-800">Estudiantes Inscritos en mis Asignaturas</h2>
                        <p className="text-gray-500 mt-1">Lleva el control y seguimiento de tus estudiantes.</p>
                    </div>
                </div>

                <div className="overflow-x-auto">
                    {loading ? (
                        <p className="text-center text-gray-500">Cargando información...</p>
                    ) : (
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Estudiante</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Asignatura</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Centro de Práctica</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Estado</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {asignaciones.map(asignacion => (
                                    <tr key={asignacion.id}>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                            {asignacion.student ? asignacion.student.fullName : 'No asignado'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {asignacion.subject ? asignacion.subject.name : 'No asignada'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {asignacion.center ? asignacion.center.name : 'Sin centro'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                                                {asignacion.status ? asignacion.status : 'PENDING'}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                                {asignaciones.length === 0 && (
                                    <tr>
                                        <td colSpan="4" className="px-6 py-4 text-center text-sm text-gray-500">
                                            No tienes estudiantes inscritos.
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
