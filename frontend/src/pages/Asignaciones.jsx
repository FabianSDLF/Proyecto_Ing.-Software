import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import assignmentService from '../services/assignmentService';
import userService from '../services/userService';
import subjectService from '../services/subjectService';
import centerService from '../services/centerService';
import { DataTable, Modal, ConfirmDialog, Select, Button, Badge } from '../components/ui';

export default function Asignaciones() {
    const [asignaciones, setAsignaciones] = useState([]);
    const [estudiantes, setEstudiantes] = useState([]);
    const [asignaturas, setAsignaturas] = useState([]);
    const [centros, setCentros] = useState([]);
    const [profesores, setProfesores] = useState([]);
    const [tutores, setTutores] = useState([]);
    
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [loadingData, setLoadingData] = useState(true);

    const [assignmentToDelete, setAssignmentToDelete] = useState(null);
    const [isDeleting, setIsDeleting] = useState(false);
    
    const [formData, setFormData] = useState({
        studentId: '',
        subjectId: '',
        centerId: '',
        collaboratorId: '',
        tutorId: ''
    });

    const cargarDatos = useCallback(async (signal) => {
        setLoadingData(true);
        try {
            const [
                asigRes, 
                estudRes, 
                asigListRes, 
                centrosRes, 
                profesRes, 
                tutoresRes
            ] = await Promise.allSettled([
                assignmentService.getAssignments({ signal }),
                userService.getUsersByRole('Estudiante', { signal }),
                subjectService.getSubjects({ signal }),
                centerService.getCenters({ signal }),
                userService.getUsersByRole('Profesor Colaborador', { signal }),
                userService.getUsersByRole('Tutor', { signal })
            ]);
            
            if (asigRes.status === 'fulfilled') setAsignaciones(Array.isArray(asigRes.value) ? asigRes.value : []);
            if (estudRes.status === 'fulfilled') setEstudiantes(Array.isArray(estudRes.value) ? estudRes.value : []);
            if (asigListRes.status === 'fulfilled') setAsignaturas(Array.isArray(asigListRes.value) ? asigListRes.value : []);
            if (centrosRes.status === 'fulfilled') setCentros(Array.isArray(centrosRes.value) ? centrosRes.value : []);
            if (profesRes.status === 'fulfilled') setProfesores(Array.isArray(profesRes.value) ? profesRes.value : []);
            if (tutoresRes.status === 'fulfilled') setTutores(Array.isArray(tutoresRes.value) ? tutoresRes.value : []);
            
        } catch (error) {
            if (error.name !== 'AbortError') {
                toast.error('Error al cargar datos del sistema');
                console.error(error);
            }
        } finally {
            setLoadingData(false);
        }
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        cargarDatos(controller.signal);
        return () => controller.abort();
    }, [cargarDatos]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.studentId || !formData.subjectId) {
            toast.error('Estudiante y Asignatura son obligatorios.');
            return;
        }

        setLoading(true);
        try {
            await assignmentService.createAssignment({
                studentId: formData.studentId,
                subjectId: formData.subjectId,
                centerId: formData.centerId || null,
                collaboratorId: formData.collaboratorId || null,
                tutorId: formData.tutorId || null,
                status: 'ACTIVA'
            });

            toast.success('Asignación creada exitosamente');
            setIsModalOpen(false);
            setFormData({ studentId: '', subjectId: '', centerId: '', collaboratorId: '', tutorId: '' });
            cargarDatos();
        } catch (error) {
            console.error('Error al guardar asignación:', error);
            toast.error(error.message || 'Error al guardar asignación');
        } finally {
            setLoading(false);
        }
    };

    const confirmDelete = async () => {
        if (!assignmentToDelete) return;

        setIsDeleting(true);
        try {
            await assignmentService.deleteAssignment(assignmentToDelete.id);
            toast.success('Asignación eliminada exitosamente');
            setAssignmentToDelete(null);
            cargarDatos();
        } catch (error) {
            console.error('Error al eliminar asignación:', error);
            toast.error(error.message || 'Error al eliminar asignación');
        } finally {
            setIsDeleting(false);
        }
    };

    const studentOptions = useMemo(() => (
        estudiantes.map(e => ({ value: e.id, label: `${e.fullName} (${e.email})` }))
    ), [estudiantes]);

    const subjectOptions = useMemo(() => (
        asignaturas.map(a => ({ value: a.id, label: `${a.name} (${a.cycle || 'Sin ciclo'})` }))
    ), [asignaturas]);

    const centerOptions = useMemo(() => (
        centros.map(c => ({ value: c.id, label: `${c.name} - ${c.city || ''}` }))
    ), [centros]);

    const collaboratorOptions = useMemo(() => (
        profesores.map(p => ({ value: p.id, label: p.fullName }))
    ), [profesores]);

    const tutorOptions = useMemo(() => (
        tutores.map(t => ({ value: t.id, label: t.fullName }))
    ), [tutores]);

    const columns = useMemo(() => [
        {
            header: 'Estudiante',
            render: (row) => (
                <div>
                    <p className="font-medium text-gray-900">{row.student?.fullName || 'N/A'}</p>
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
            header: 'Centro Práctica',
            render: (row) => row.center?.name || <span className="text-gray-400 italic">No asignado</span>,
        },
        {
            header: 'Prof. Colaborador',
            render: (row) => row.collaborator?.fullName || <span className="text-gray-400 italic">No asignado</span>,
        },
        {
            header: 'Tutor',
            render: (row) => row.tutor?.fullName || <span className="text-gray-400 italic">No asignado</span>,
        },
        {
            header: 'Estado',
            align: 'center',
            render: (row) => (
                <Badge variant={row.status === 'ACTIVA' ? 'success' : 'gray'}>
                    {row.status || 'N/A'}
                </Badge>
            ),
        },
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => (
                <Button 
                    variant="danger"
                    size="sm"
                    onClick={() => setAssignmentToDelete(row)}
                >
                    Eliminar
                </Button>
            ),
        },
    ], []);

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Gestión de Asignaciones</h1>
                    <p className="text-gray-500 mt-1">Vincula estudiantes con asignaturas, centros de práctica y docentes supervisores.</p>
                </div>
                <Button 
                    variant="primary"
                    onClick={() => setIsModalOpen(true)}
                >
                    + Nueva Asignación
                </Button>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                <DataTable
                    columns={columns}
                    data={asignaciones}
                    isLoading={loadingData}
                    emptyMessage="No hay asignaciones registradas en el sistema."
                    keyField="id"
                />
            </div>

            {/* Modal Nueva Asignación (FE-18) */}
            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title="Nueva Asignación de Práctica"
                size="lg"
                footer={
                    <div className="flex justify-end space-x-3">
                        <Button
                            variant="secondary"
                            onClick={() => setIsModalOpen(false)}
                            disabled={loading}
                        >
                            Cancelar
                        </Button>
                        <Button
                            variant="primary"
                            onClick={handleSubmit}
                            isLoading={loading}
                        >
                            Guardar Asignación
                        </Button>
                    </div>
                }
            >
                <form onSubmit={handleSubmit} className="space-y-4">
                    <Select
                        label="Estudiante"
                        name="studentId"
                        options={studentOptions}
                        value={formData.studentId}
                        onChange={handleInputChange}
                        placeholder="-- Seleccione Estudiante --"
                        required
                    />

                    <Select
                        label="Asignatura (Ciclo)"
                        name="subjectId"
                        options={subjectOptions}
                        value={formData.subjectId}
                        onChange={handleInputChange}
                        placeholder="-- Seleccione Asignatura --"
                        required
                    />

                    <Select
                        label="Centro de Práctica (Opcional)"
                        name="centerId"
                        options={centerOptions}
                        value={formData.centerId}
                        onChange={handleInputChange}
                        placeholder="-- Seleccione Centro --"
                    />

                    <Select
                        label="Profesor Colaborador (Opcional)"
                        name="collaboratorId"
                        options={collaboratorOptions}
                        value={formData.collaboratorId}
                        onChange={handleInputChange}
                        placeholder="-- Seleccione Profesor Colaborador --"
                    />

                    <Select
                        label="Tutor UBB (Opcional)"
                        name="tutorId"
                        options={tutorOptions}
                        value={formData.tutorId}
                        onChange={handleInputChange}
                        placeholder="-- Seleccione Tutor --"
                    />
                </form>
            </Modal>

            {/* Diálogo de Confirmación Accesible (FE-19) */}
            <ConfirmDialog
                isOpen={Boolean(assignmentToDelete)}
                onClose={() => setAssignmentToDelete(null)}
                onConfirm={confirmDelete}
                title="Eliminar Asignación"
                message={`¿Está seguro de que desea eliminar la asignación del estudiante "${assignmentToDelete?.student?.fullName}"? Esta acción no se puede deshacer.`}
                confirmText="Eliminar Asignación"
                cancelText="Cancelar"
                variant="danger"
                isLoading={isDeleting}
            />
        </div>
    );
}
