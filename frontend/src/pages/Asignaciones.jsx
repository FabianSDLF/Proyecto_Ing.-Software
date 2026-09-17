import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

export default function Asignaciones() {
    const [asignaciones, setAsignaciones] = useState([]);
    const [estudiantes, setEstudiantes] = useState([]);
    const [asignaturas, setAsignaturas] = useState([]);
    const [centros, setCentros] = useState([]);
    const [profesores, setProfesores] = useState([]);
    const [tutores, setTutores] = useState([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    
    const [formData, setFormData] = useState({
        studentId: '',
        subjectId: '',
        centerId: '',
        collaboratorId: '',
        tutorId: ''
    });

    const navigate = useNavigate();

    const cargarDatos = async () => {
        try {
            const [
                asigRes, 
                estudRes, 
                asigListRes, 
                centrosRes, 
                profesRes, 
                tutoresRes
            ] = await Promise.all([
                fetch('http://localhost:8080/api/asignaciones'),
                fetch('http://localhost:8080/api/usuarios/rol/Estudiante'),
                fetch('http://localhost:8080/api/asignaturas'),
                fetch('http://localhost:8080/api/centros'),
                fetch('http://localhost:8080/api/usuarios/rol/Profesor Colaborador'),
                fetch('http://localhost:8080/api/usuarios/rol/Tutor')
            ]);
            
            if (asigRes.ok) setAsignaciones(await asigRes.json());
            if (estudRes.ok) setEstudiantes(await estudRes.json());
            if (asigListRes.ok) setAsignaturas(await asigListRes.json());
            if (centrosRes.ok) setCentros(await centrosRes.json());
            if (profesRes.ok) setProfesores(await profesRes.json());
            if (tutoresRes.ok) setTutores(await tutoresRes.json());
            
        } catch (error) {
            toast.error('Error al cargar datos del sistema');
            console.error(error);
        }
    };

    useEffect(() => {
        const userString = localStorage.getItem('usuarioActivo');
        if (userString) {
            try {
                const user = JSON.parse(userString);
                if (!user.role || user.role.name !== 'Administrador') {
                    toast.error("Acceso denegado: Solo administradores");
                    navigate('/dashboard');
                    return;
                }
            } catch {
                navigate('/');
                return;
            }
        } else {
            navigate('/');
            return;
        }

        cargarDatos();
    }, [navigate]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const payload = {
                student: formData.studentId ? { id: formData.studentId } : null,
                subject: formData.subjectId ? { id: formData.subjectId } : null,
                center: formData.centerId ? { id: formData.centerId } : null,
                collaborator: formData.collaboratorId ? { id: formData.collaboratorId } : null,
                tutor: formData.tutorId ? { id: formData.tutorId } : null,
                status: 'ACTIVA'
            };
            
            const res = await fetch('http://localhost:8080/api/asignaciones', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                toast.success('Asignación creada exitosamente');
                setIsModalOpen(false);
                setFormData({ studentId: '', subjectId: '', centerId: '', collaboratorId: '', tutorId: '' });
                cargarDatos();
            } else {
                const errorMsg = await res.text();
                toast.error(errorMsg || 'Error al guardar asignación');
            }
        } catch (error) {
            toast.error('Error de conexión con el servidor');
        } finally {
            setLoading(false);
        }
    };

    const eliminarAsignacion = async (id) => {
        if (window.confirm('¿Está seguro de eliminar esta asignación?')) {
            try {
                const res = await fetch(`http://localhost:8080/api/asignaciones/${id}`, {
                    method: 'DELETE'
                });
                
                if (res.ok) {
                    toast.success('Asignación eliminada');
                    cargarDatos();
                } else {
                    toast.error('Error al eliminar asignación');
                }
            } catch (error) {
                toast.error('Error de conexión con el servidor');
            }
        }
    };

    return (
        <div className="p-8 max-w-7xl mx-auto">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-gray-800">Gestión de Asignaciones</h1>
                <button 
                    onClick={() => setIsModalOpen(true)}
                    className="bg-blue-600 text-white px-4 py-2 rounded shadow hover:bg-blue-700 font-bold transition"
                >
                    Nueva Asignación
                </button>
            </div>

            <div className="bg-white rounded-lg shadow-md overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="min-w-full text-left">
                        <thead className="bg-gray-50 border-b">
                            <tr>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Estudiante</th>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Asignatura</th>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Centro Práctica</th>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Prof. Colaborador</th>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Tutor</th>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Estado</th>
                                <th className="px-6 py-4 font-semibold text-gray-600 uppercase text-xs">Acciones</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {asignaciones.map((asig) => (
                                <tr key={asig.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap">{asig.student?.name} {asig.student?.lastName}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{asig.subject?.name}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{asig.center?.name || 'No asignado'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{asig.collaborator ? `${asig.collaborator.name} ${asig.collaborator.lastName}` : 'No asignado'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{asig.tutor ? `${asig.tutor.name} ${asig.tutor.lastName}` : 'No asignado'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${asig.status === 'ACTIVA' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                            {asig.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <button 
                                            onClick={() => eliminarAsignacion(asig.id)}
                                            className="text-red-500 hover:text-red-700 font-medium"
                                        >
                                            Eliminar
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            {asignaciones.length === 0 && (
                                <tr>
                                    <td colSpan="7" className="px-6 py-8 text-center text-gray-500">
                                        No hay asignaciones registradas en el sistema.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Modal */}
            {isModalOpen && (
                <div className="fixed inset-0 bg-gray-900 bg-opacity-50 overflow-y-auto h-full w-full flex justify-center items-center z-50">
                    <div className="bg-white p-8 rounded-lg shadow-xl w-full max-w-2xl">
                        <h2 className="text-2xl font-bold mb-6 text-gray-800">Nueva Asignación de Práctica</h2>
                        <form onSubmit={handleSubmit} className="space-y-5">
                            
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Estudiante *</label>
                                <select name="studentId" value={formData.studentId} onChange={handleInputChange} required className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-3 border focus:ring-blue-500 focus:border-blue-500">
                                    <option value="">Seleccione estudiante</option>
                                    {estudiantes.map(e => <option key={e.id} value={e.id}>{e.name} {e.lastName} ({e.email})</option>)}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Asignatura *</label>
                                <select name="subjectId" value={formData.subjectId} onChange={handleInputChange} required className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-3 border focus:ring-blue-500 focus:border-blue-500">
                                    <option value="">Seleccione asignatura</option>
                                    {asignaturas.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Centro de Práctica</label>
                                <select name="centerId" value={formData.centerId} onChange={handleInputChange} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-3 border focus:ring-blue-500 focus:border-blue-500">
                                    <option value="">(Opcional) Seleccione centro</option>
                                    {centros.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                                </select>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Profesor Colaborador</label>
                                    <select name="collaboratorId" value={formData.collaboratorId} onChange={handleInputChange} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-3 border focus:ring-blue-500 focus:border-blue-500">
                                        <option value="">(Opcional) Seleccione colaborador</option>
                                        {profesores.map(p => <option key={p.id} value={p.id}>{p.name} {p.lastName}</option>)}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Tutor de Universidad</label>
                                    <select name="tutorId" value={formData.tutorId} onChange={handleInputChange} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm p-3 border focus:ring-blue-500 focus:border-blue-500">
                                        <option value="">(Opcional) Seleccione tutor</option>
                                        {tutores.map(t => <option key={t.id} value={t.id}>{t.name} {t.lastName}</option>)}
                                    </select>
                                </div>
                            </div>

                            <div className="flex justify-end space-x-3 mt-8 pt-4 border-t">
                                <button type="button" onClick={() => setIsModalOpen(false)} className="px-4 py-2 bg-gray-100 text-gray-800 rounded font-medium hover:bg-gray-200 transition">
                                    Cancelar
                                </button>
                                <button type="submit" disabled={loading} className={`px-4 py-2 bg-blue-600 text-white rounded font-medium hover:bg-blue-700 transition ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}>
                                    {loading ? 'Guardando...' : 'Guardar Asignación'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
