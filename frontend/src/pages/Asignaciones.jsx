import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import assignmentService from '../services/assignmentService';
import userService from '../services/userService';
import subjectService from '../services/subjectService';
import centerService from '../services/centerService';
import { DataTable, Modal, ConfirmDialog, Select, Input, Button, Badge } from '../components/ui';

const DEPENDENCY_OPTIONS = [
    { value: 'PUBLICO_SLEP', label: 'Público / SLEP / Municipal' },
    { value: 'SUBVENCIONADO', label: 'Particular Subvencionado' },
    { value: 'PRIVADO', label: 'Particular Pagado' },
    { value: 'EDUCACION_ESPECIAL', label: 'Educación Especial' },
    { value: 'ADULTOS', label: 'Educación de Adultos' },
    { value: 'PENITENCIARIO', label: 'Contexto Penitenciario' },
];

export default function Asignaciones() {
    // Pestaña activa ('ASIGNACIONES' | 'DUPLAS' | 'CENTROS')
    const [activeTab, setActiveTab] = useState('ASIGNACIONES');

    // Datos generales de asignaciones individuales
    const [asignaciones, setAsignaciones] = useState([]);
    const [estudiantes, setEstudiantes] = useState([]);
    const [asignaturas, setAsignaturas] = useState([]);
    const [centros, setCentros] = useState([]);
    const [profesores, setProfesores] = useState([]);
    const [tutores, setTutores] = useState([]);
    const [periodos, setPeriodos] = useState([]);

    // Datos de duplas (HU9)
    const [duplas, setDuplas] = useState([]);
    const [estudiantesDisponiblesDupla, setEstudiantesDisponiblesDupla] = useState([]);
    const [isDuplaModalOpen, setIsDuplaModalOpen] = useState(false);
    const [filtroDupla, setFiltroDupla] = useState('');
    const [loadingCrearDupla, setLoadingCrearDupla] = useState(false);
    const [duplaFormData, setDuplaFormData] = useState({
        assignment1Id: '',
        assignment2Id: '',
    });

    // Estados de modales y cargas para Asignaciones
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [loadingData, setLoadingData] = useState(true);
    const [loadingDuplas, setLoadingDuplas] = useState(false);

    const [assignmentToDelete, setAssignmentToDelete] = useState(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const [formData, setFormData] = useState({
        studentId: '',
        subjectId: '',
        centerId: '',
        collaboratorId: '',
        tutorId: '',
        periodId: '',
    });

    // Estados para Gestión de Centros de Práctica (Fase 1)
    const [isCenterModalOpen, setIsCenterModalOpen] = useState(false);
    const [editingCenter, setEditingCenter] = useState(null);
    const [savingCenter, setSavingCenter] = useState(false);
    const [centerToDelete, setCenterToDelete] = useState(null);
    const [deletingCenter, setDeletingCenter] = useState(false);
    const [centerForm, setCenterForm] = useState({
        name: '',
        dependency: 'PUBLICO_SLEP',
        address: '',
        district: '',
        city: 'Chillán',
        phone: '',
        contactName: '',
        contactEmail: '',
    });

    // Cargar datos del sistema para asignaciones individuales y centros
    const cargarDatos = useCallback(async (signal) => {
        setLoadingData(true);
        try {
            const [
                asigRes,
                estudRes,
                asigListRes,
                centrosRes,
                profesRes,
                tutoresRes,
                periodosRes,
            ] = await Promise.allSettled([
                assignmentService.getAssignments({ signal }),
                userService.getUsersByRole('Estudiante', { signal }),
                subjectService.getSubjects({ signal }),
                centerService.getCenters({ signal }),
                userService.getUsersByRole('Profesor Colaborador', { signal }),
                userService.getUsersByRole('Tutor', { signal }),
                subjectService.getPeriods ? subjectService.getPeriods({ signal }) : Promise.resolve([]),
            ]);

            if (asigRes.status === 'fulfilled') setAsignaciones(Array.isArray(asigRes.value) ? asigRes.value : []);
            if (estudRes.status === 'fulfilled') setEstudiantes(Array.isArray(estudRes.value) ? estudRes.value : []);
            if (asigListRes.status === 'fulfilled') setAsignaturas(Array.isArray(asigListRes.value) ? asigListRes.value : []);
            if (centrosRes.status === 'fulfilled') setCentros(Array.isArray(centrosRes.value) ? centrosRes.value : []);
            if (profesRes.status === 'fulfilled') setProfesores(Array.isArray(profesRes.value) ? profesRes.value : []);
            if (tutoresRes.status === 'fulfilled') setTutores(Array.isArray(tutoresRes.value) ? tutoresRes.value : []);
            if (periodosRes.status === 'fulfilled') setPeriodos(Array.isArray(periodosRes.value) ? periodosRes.value : []);
        } catch (error) {
            if (error.name !== 'AbortError') {
                toast.error('Error al cargar datos del sistema');
                console.error(error);
            }
        } finally {
            setLoadingData(false);
        }
    }, []);

    // Cargar datos de duplas y estudiantes disponibles de ciclo intermedio (HU9)
    const cargarDatosDuplas = useCallback(async (signal) => {
        setLoadingDuplas(true);
        try {
            const [duplasRes, dispRes] = await Promise.allSettled([
                assignmentService.getDuplas(undefined, { signal }),
                assignmentService.getEstudiantesDisponiblesDupla(undefined, { signal }),
            ]);

            if (duplasRes.status === 'fulfilled') {
                setDuplas(Array.isArray(duplasRes.value) ? duplasRes.value : []);
            }
            if (dispRes.status === 'fulfilled') {
                setEstudiantesDisponiblesDupla(Array.isArray(dispRes.value) ? dispRes.value : []);
            }
        } catch (error) {
            if (error.name !== 'AbortError') {
                toast.error('Error al cargar información de duplas');
                console.error(error);
            }
        } finally {
            setLoadingDuplas(false);
        }
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        cargarDatos(controller.signal);
        cargarDatosDuplas(controller.signal);
        return () => controller.abort();
    }, [cargarDatos, cargarDatosDuplas]);

    useEffect(() => {
        if (activeTab === 'DUPLAS') {
            cargarDatosDuplas();
        }
    }, [activeTab, cargarDatosDuplas]);

    // Manejo de formulario Asignación Individual
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
                periodId: formData.periodId || null,
                status: 'ACTIVA',
            });

            toast.success('Asignación creada exitosamente');
            setIsModalOpen(false);
            setFormData({ studentId: '', subjectId: '', centerId: '', collaboratorId: '', tutorId: '', periodId: '' });
            cargarDatos();
            cargarDatosDuplas();
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
            cargarDatosDuplas();
        } catch (error) {
            console.error('Error al eliminar asignación:', error);
            toast.error(error.message || 'Error al eliminar asignación');
        } finally {
            setIsDeleting(false);
        }
    };

    // Handlers de Centros de Práctica (Fase 1)
    const handleOpenNewCenter = () => {
        setEditingCenter(null);
        setCenterForm({
            name: '',
            dependency: 'PUBLICO_SLEP',
            address: '',
            district: '',
            city: 'Chillán',
            phone: '',
            contactName: '',
            contactEmail: '',
        });
        setIsCenterModalOpen(true);
    };

    const handleOpenEditCenter = (center) => {
        setEditingCenter(center);
        setCenterForm({
            name: center.name || '',
            dependency: center.dependency || 'PUBLICO_SLEP',
            address: center.address || '',
            district: center.district || '',
            city: center.city || '',
            phone: center.phone || '',
            contactName: center.contactName || '',
            contactEmail: center.contactEmail || '',
        });
        setIsCenterModalOpen(true);
    };

    const handleSaveCenter = async (e) => {
        e.preventDefault();
        if (!centerForm.name.trim()) {
            toast.error('El nombre del establecimiento es obligatorio.');
            return;
        }
        setSavingCenter(true);
        try {
            if (editingCenter) {
                await centerService.updateCenter(editingCenter.id, centerForm);
                toast.success('Centro de práctica actualizado exitosamente');
            } else {
                await centerService.createCenter(centerForm);
                toast.success('Centro de práctica registrado exitosamente');
            }
            setIsCenterModalOpen(false);
            setEditingCenter(null);
            cargarDatos();
        } catch (error) {
            toast.error(error.message || 'Error al guardar el centro de práctica');
        } finally {
            setSavingCenter(false);
        }
    };

    const confirmDeleteCenter = async () => {
        if (!centerToDelete) return;
        setDeletingCenter(true);
        try {
            await centerService.deleteCenter(centerToDelete.id);
            toast.success('Centro de práctica eliminado exitosamente');
            setCenterToDelete(null);
            cargarDatos();
        } catch (error) {
            toast.error(error.message || 'No se pudo eliminar el centro de práctica');
        } finally {
            setDeletingCenter(false);
        }
    };

    // Manejo de creación de Duplas (HU9)
    const handleDuplaInputChange = (e) => {
        const { name, value } = e.target;
        setDuplaFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleOpenCrearDuplaModal = () => {
        setDuplaFormData({ assignment1Id: '', assignment2Id: '' });
        setFiltroDupla('');
        setIsDuplaModalOpen(true);
        cargarDatosDuplas();
    };

    const handleSubmitDupla = async (e) => {
        if (e && e.preventDefault) e.preventDefault();

        const { assignment1Id, assignment2Id } = duplaFormData;

        if (!assignment1Id || !assignment2Id) {
            toast.error('Debe seleccionar ambos estudiantes para conformar la dupla.');
            return;
        }

        if (String(assignment1Id) === String(assignment2Id)) {
            toast.error('No se puede crear una dupla con el mismo estudiante.');
            return;
        }

        const asig1 = estudiantesDisponiblesDupla.find(a => String(a.id) === String(assignment1Id));
        const periodId = asig1?.period?.id || asig1?.periodId || 1;

        setLoadingCrearDupla(true);
        try {
            await assignmentService.crearDupla({
                assignment1Id: Number(assignment1Id),
                assignment2Id: Number(assignment2Id),
                periodId: Number(periodId),
            });

            toast.success('Dupla creada exitosamente');
            setIsDuplaModalOpen(false);
            setDuplaFormData({ assignment1Id: '', assignment2Id: '' });
            setFiltroDupla('');
            cargarDatosDuplas();
        } catch (error) {
            console.error('Error al crear dupla:', error);
            toast.error(error.message || 'Error al conformar la dupla de estudiantes');
        } finally {
            setLoadingCrearDupla(false);
        }
    };

    // Opciones para asignaciones individuales
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

    const periodOptions = useMemo(() => (
        periodos.map(p => ({ value: p.id, label: `${p.name} (${p.status})` }))
    ), [periodos]);

    // Opciones y filtrado para Modal de Duplas (HU9)
    const estudiantesDuplaFiltrados = useMemo(() => {
        if (!filtroDupla.trim()) return estudiantesDisponiblesDupla;
        const term = filtroDupla.toLowerCase();
        return estudiantesDisponiblesDupla.filter(item => {
            const name = item.student?.fullName || item.studentName || item.name || '';
            const email = item.student?.email || item.studentEmail || item.email || '';
            return name.toLowerCase().includes(term) || email.toLowerCase().includes(term);
        });
    }, [estudiantesDisponiblesDupla, filtroDupla]);

    const opcionesEstudiante1 = useMemo(() => {
        return estudiantesDisponiblesDupla
            .filter(item => {
                const itemIdStr = String(item.id);
                if (itemIdStr === String(duplaFormData.assignment2Id)) return false;
                if (itemIdStr === String(duplaFormData.assignment1Id)) return true;
                if (!filtroDupla.trim()) return true;
                const term = filtroDupla.toLowerCase();
                const name = item.student?.fullName || item.studentName || item.name || '';
                const email = item.student?.email || item.studentEmail || item.email || '';
                return name.toLowerCase().includes(term) || email.toLowerCase().includes(term);
            })
            .map(item => {
                const name = item.student?.fullName || item.studentName || 'Estudiante';
                const email = item.student?.email || item.studentEmail || '';
                const subject = item.subject?.name || item.subjectName || 'Ciclo Intermedio';
                return {
                    value: item.id,
                    label: `${name} (${email}) - ${subject}`,
                };
            });
    }, [estudiantesDisponiblesDupla, duplaFormData.assignment1Id, duplaFormData.assignment2Id, filtroDupla]);

    const opcionesEstudiante2 = useMemo(() => {
        return estudiantesDisponiblesDupla
            .filter(item => {
                const itemIdStr = String(item.id);
                if (itemIdStr === String(duplaFormData.assignment1Id)) return false;
                if (itemIdStr === String(duplaFormData.assignment2Id)) return true;
                if (!filtroDupla.trim()) return true;
                const term = filtroDupla.toLowerCase();
                const name = item.student?.fullName || item.studentName || item.name || '';
                const email = item.student?.email || item.studentEmail || '';
                return name.toLowerCase().includes(term) || email.toLowerCase().includes(term);
            })
            .map(item => {
                const name = item.student?.fullName || item.studentName || 'Estudiante';
                const email = item.student?.email || item.studentEmail || '';
                const subject = item.subject?.name || item.subjectName || 'Ciclo Intermedio';
                return {
                    value: item.id,
                    label: `${name} (${email}) - ${subject}`,
                };
            });
    }, [estudiantesDisponiblesDupla, duplaFormData.assignment1Id, duplaFormData.assignment2Id, filtroDupla]);

    const isConfirmDuplaDisabled = useMemo(() => {
        const { assignment1Id, assignment2Id } = duplaFormData;
        if (!assignment1Id || !assignment2Id) return true;
        if (String(assignment1Id) === String(assignment2Id)) return true;
        if (loadingCrearDupla) return true;
        return false;
    }, [duplaFormData, loadingCrearDupla]);

    // Columnas de Asignaciones Individuales
    const columnsAsignaciones = useMemo(() => [
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
            header: 'Asignatura / Periodo',
            render: (row) => (
                <div>
                    <p className="text-gray-800 font-medium">{row.subject?.name || 'N/A'}</p>
                    {row.period?.name && (
                        <span className="inline-block text-[11px] font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded mt-0.5">
                            Semestre {row.period.name}
                        </span>
                    )}
                </div>
            ),
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

    // Columnas de Duplas (HU9: estrictamente SIN botones de editar ni eliminar)
    const columnsDuplas = useMemo(() => [
        {
            header: 'Estudiante 1',
            render: (row) => {
                const st = row.assignment1?.student || row.student1;
                const name = st?.fullName || row.student1Name || 'N/A';
                const email = st?.email || row.student1Email || '';
                return (
                    <div>
                        <p className="font-semibold text-gray-900">{name}</p>
                        <p className="text-xs text-gray-500">{email}</p>
                    </div>
                );
            },
        },
        {
            header: 'Estudiante 2',
            render: (row) => {
                const st = row.assignment2?.student || row.student2;
                const name = st?.fullName || row.student2Name || 'N/A';
                const email = st?.email || row.student2Email || '';
                return (
                    <div>
                        <p className="font-semibold text-gray-900">{name}</p>
                        <p className="text-xs text-gray-500">{email}</p>
                    </div>
                );
            },
        },
        {
            header: 'Asignatura / Ciclo',
            render: (row) => {
                const subjectName = row.assignment1?.subject?.name || row.subjectName || 'Práctica Intermedia';
                const cycle = row.assignment1?.subject?.cycle || row.cycle || 'INTERMEDIO';
                return (
                    <div>
                        <p className="font-medium text-gray-800">{subjectName}</p>
                        <span className="inline-block text-[11px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded mt-0.5">
                            Ciclo {cycle}
                        </span>
                    </div>
                );
            },
        },
        {
            header: 'Centro de Práctica',
            render: (row) => {
                const centerName = row.assignment1?.center?.name || row.centerName || row.assignment2?.center?.name;
                return centerName ? (
                    <span className="text-gray-700">{centerName}</span>
                ) : (
                    <span className="text-gray-400 italic">No asignado</span>
                );
            },
        },
        {
            header: 'Fecha',
            render: (row) => {
                const dateVal = row.createdAt;
                return (
                    <span className="text-sm text-gray-600">
                        {dateVal ? new Date(dateVal).toLocaleDateString('es-CL') : 'N/A'}
                    </span>
                );
            },
        },
        {
            header: 'Estado',
            align: 'center',
            render: () => (
                <Badge variant="info">
                    Definitiva
                </Badge>
            ),
        },
    ], []);

    // Columnas de Centros de Práctica (Fase 1)
    const columnsCentros = useMemo(() => [
        {
            header: 'Establecimiento',
            render: (row) => (
                <div>
                    <p className="font-semibold text-gray-900">{row.name}</p>
                    <p className="text-xs text-gray-500">{row.address || 'Sin dirección'}</p>
                </div>
            ),
        },
        {
            header: 'Dependencia',
            render: (row) => (
                <Badge variant="info">
                    {row.dependency || 'PUBLICO_SLEP'}
                </Badge>
            ),
        },
        {
            header: 'Ubicación',
            render: (row) => (
                <span className="text-sm text-gray-700">
                    {[row.district, row.city].filter(Boolean).join(', ') || 'Sin especificar'}
                </span>
            ),
        },
        {
            header: 'Contacto Directivo / UTP',
            render: (row) => (
                <div className="text-xs text-gray-600">
                    <p className="font-medium text-gray-800">{row.contactName || 'Sin contacto'}</p>
                    {row.contactEmail && <p>{row.contactEmail}</p>}
                    {row.phone && <p>{row.phone}</p>}
                </div>
            ),
        },
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => (
                <div className="flex items-center justify-end gap-2">
                    <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleOpenEditCenter(row)}
                    >
                        Editar
                    </Button>
                    <Button
                        variant="danger"
                        size="sm"
                        onClick={() => setCenterToDelete(row)}
                    >
                        Eliminar
                    </Button>
                </div>
            ),
        },
    ], []);

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-6">
            {/* Encabezado */}
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Gestión de Asignaciones y Centros</h1>
                    <p className="text-gray-500 mt-1">Vincula estudiantes con asignaturas, duplas pedagógicas, centros de práctica y supervisores.</p>
                </div>
                {activeTab === 'ASIGNACIONES' && (
                    <Button
                        variant="primary"
                        onClick={() => setIsModalOpen(true)}
                    >
                        + Nueva Asignación
                    </Button>
                )}
                {activeTab === 'DUPLAS' && (
                    <Button
                        variant="primary"
                        onClick={handleOpenCrearDuplaModal}
                    >
                        + Nueva Dupla
                    </Button>
                )}
                {activeTab === 'CENTROS' && (
                    <Button
                        variant="primary"
                        onClick={handleOpenNewCenter}
                    >
                        + Nuevo Centro de Práctica
                    </Button>
                )}
            </div>

            {/* Pestañas de Navegación */}
            <div className="flex border-b border-gray-200 gap-4" role="tablist">
                <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'ASIGNACIONES'}
                    onClick={() => setActiveTab('ASIGNACIONES')}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors cursor-pointer ${
                        activeTab === 'ASIGNACIONES'
                            ? 'border-orange-600 text-orange-600'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    Asignaciones Individuales
                </button>
                <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'DUPLAS'}
                    onClick={() => setActiveTab('DUPLAS')}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors cursor-pointer ${
                        activeTab === 'DUPLAS'
                            ? 'border-orange-600 text-orange-600'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    Duplas (Ciclo Intermedio)
                </button>
                <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'CENTROS'}
                    onClick={() => setActiveTab('CENTROS')}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors cursor-pointer ${
                        activeTab === 'CENTROS'
                            ? 'border-orange-600 text-orange-600'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    Centros de Práctica ({centros.length})
                </button>
            </div>

            {/* Pestaña: Asignaciones Individuales */}
            {activeTab === 'ASIGNACIONES' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                    <DataTable
                        columns={columnsAsignaciones}
                        data={asignaciones}
                        isLoading={loadingData}
                        emptyMessage="No hay asignaciones registradas en el sistema."
                        keyField="id"
                    />
                </div>
            )}

            {/* Pestaña: Duplas de Ciclo Intermedio (HU9) */}
            {activeTab === 'DUPLAS' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                    <DataTable
                        columns={columnsDuplas}
                        data={duplas}
                        isLoading={loadingDuplas}
                        emptyMessage="No hay duplas de estudiantes registradas en el ciclo intermedio."
                        keyField="id"
                    />
                </div>
            )}

            {/* Pestaña: Centros de Práctica (Fase 1) */}
            {activeTab === 'CENTROS' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                    <DataTable
                        columns={columnsCentros}
                        data={centros}
                        isLoading={loadingData}
                        emptyMessage="No hay centros de práctica registrados."
                        keyField="id"
                    />
                </div>
            )}

            {/* Modal Nueva Asignación Individual */}
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

                    {periodOptions.length > 0 && (
                        <Select
                            label="Periodo Académico (Semestre)"
                            name="periodId"
                            options={periodOptions}
                            value={formData.periodId}
                            onChange={handleInputChange}
                            placeholder="-- Automático (Periodo Activo) --"
                        />
                    )}

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

            {/* Modal Crear / Editar Centro de Práctica (Fase 1) */}
            <Modal
                isOpen={isCenterModalOpen}
                onClose={() => setIsCenterModalOpen(false)}
                title={editingCenter ? `Editar Centro: ${editingCenter.name}` : 'Registrar Nuevo Centro de Práctica'}
                size="lg"
            >
                <form onSubmit={handleSaveCenter} className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <Input
                            label="Nombre del Establecimiento"
                            value={centerForm.name}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, name: e.target.value }))}
                            placeholder="Ej: Liceo Bicentenario Marta Brunet"
                            required
                        />
                        <Select
                            label="Dependencia"
                            options={DEPENDENCY_OPTIONS}
                            value={centerForm.dependency}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, dependency: e.target.value }))}
                            placeholder={null}
                        />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <Input
                            label="Dirección"
                            value={centerForm.address}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, address: e.target.value }))}
                            placeholder="Ej: Av. Libertad 850"
                        />
                        <Input
                            label="Comuna"
                            value={centerForm.district}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, district: e.target.value }))}
                            placeholder="Ej: Chillán"
                        />
                        <Input
                            label="Ciudad"
                            value={centerForm.city}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, city: e.target.value }))}
                            placeholder="Ej: Chillán"
                        />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <Input
                            label="Nombre Contacto / Jefe UTP"
                            value={centerForm.contactName}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, contactName: e.target.value }))}
                            placeholder="Ej: María González (UTP)"
                        />
                        <Input
                            label="Correo de Contacto"
                            type="email"
                            value={centerForm.contactEmail}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, contactEmail: e.target.value }))}
                            placeholder="utp@colegio.cl"
                        />
                        <Input
                            label="Teléfono"
                            value={centerForm.phone}
                            onChange={(e) => setCenterForm(prev => ({ ...prev, phone: e.target.value }))}
                            placeholder="+56 42 2223344"
                        />
                    </div>

                    <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
                        <Button type="button" variant="secondary" onClick={() => setIsCenterModalOpen(false)}>
                            Cancelar
                        </Button>
                        <Button type="submit" variant="primary" isLoading={savingCenter}>
                            {editingCenter ? 'Guardar Cambios' : 'Registrar Centro'}
                        </Button>
                    </div>
                </form>
            </Modal>

            {/* Modal Crear Dupla (HU9) */}
            <Modal
                isOpen={isDuplaModalOpen}
                onClose={() => setIsDuplaModalOpen(false)}
                title="Crear Dupla de Práctica (Ciclo Intermedio)"
                size="lg"
                footer={
                    <div className="flex justify-end space-x-3">
                        <Button
                            variant="secondary"
                            onClick={() => setIsDuplaModalOpen(false)}
                            disabled={loadingCrearDupla}
                        >
                            Cancelar
                        </Button>
                        <Button
                            variant="primary"
                            onClick={handleSubmitDupla}
                            isLoading={loadingCrearDupla}
                            disabled={isConfirmDuplaDisabled}
                        >
                            Crear Dupla
                        </Button>
                    </div>
                }
            >
                <div className="space-y-5">
                    <div
                        role="alert"
                        className="p-4 bg-amber-50 border-l-4 border-amber-500 rounded-xl text-amber-900 text-sm flex items-start gap-3"
                    >
                        <svg className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                        <div>
                            <p className="font-bold text-amber-900">Advertencia Importante</p>
                            <p className="mt-0.5 text-xs text-amber-800 font-medium">
                                Las duplas de práctica son definitivas durante el semestre y no podrán modificarse ni desvincularse.
                            </p>
                        </div>
                    </div>

                    <div>
                        <Input
                            label="Buscar Estudiante"
                            name="filtroDupla"
                            placeholder="Buscar por nombre o correo electrónico..."
                            value={filtroDupla}
                            onChange={(e) => setFiltroDupla(e.target.value)}
                            helperText={
                                filtroDupla.trim()
                                    ? `Resultados filtrados: ${estudiantesDuplaFiltrados.length} estudiante(s) encontrado(s)`
                                    : 'Filtra la lista de opciones para ambos selectores'
                            }
                        />
                    </div>

                    <div>
                        <Select
                            label="Estudiante 1 (Ciclo Intermedio)"
                            name="assignment1Id"
                            value={duplaFormData.assignment1Id}
                            onChange={handleDuplaInputChange}
                            options={opcionesEstudiante1}
                            placeholder="-- Seleccione Primer Estudiante --"
                            required
                        />
                    </div>

                    <div>
                        <Select
                            label="Estudiante 2 (Ciclo Intermedio)"
                            name="assignment2Id"
                            value={duplaFormData.assignment2Id}
                            onChange={handleDuplaInputChange}
                            options={opcionesEstudiante2}
                            placeholder="-- Seleccione Segundo Estudiante --"
                            required
                        />
                    </div>

                    {estudiantesDisponiblesDupla.length === 0 && !loadingDuplas && (
                        <p className="text-xs text-gray-500 italic bg-gray-50 p-3 rounded-lg border border-gray-200">
                            No hay estudiantes disponibles en Ciclo Intermedio sin pareja para este periodo.
                        </p>
                    )}
                </div>
            </Modal>

            {/* Diálogo de Confirmación para Asignación Individual */}
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

            {/* Diálogo de Confirmación para Eliminar Centro de Práctica */}
            <ConfirmDialog
                isOpen={Boolean(centerToDelete)}
                onClose={() => setCenterToDelete(null)}
                onConfirm={confirmDeleteCenter}
                title="Eliminar Centro de Práctica"
                message={`¿Está seguro de que desea eliminar el establecimiento "${centerToDelete?.name}"?`}
                confirmText="Eliminar Centro"
                cancelText="Cancelar"
                variant="danger"
                isLoading={deletingCenter}
            />
        </div>
    );
}
