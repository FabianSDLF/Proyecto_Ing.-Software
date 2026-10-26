import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { BASE_URL } from '../services/apiClient';
import assignmentService from '../services/assignmentService';
import evaluationService from '../services/evaluationService';
import { DataTable, Badge, Modal, Button, Input } from '../components/ui';

function getEvaluationTypeForRole(role) {
    if (role === 'Tutor') return 'PAUTA_OBSERVACION_TUTOR';
    if (role === 'Profesor de Asignatura') return 'RUBRICA_INFORME';
    return 'PAUTA_OBSERVACION_COLABORADOR';
}

export default function MisEstudiantes() {
    const { user, userRole } = useAuth();
    const navigate = useNavigate();
    const [asignaciones, setAsignaciones] = useState([]);
    const [evaluacionesPorAsignacion, setEvaluacionesPorAsignacion] = useState({});
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [isEvaluationModalOpen, setIsEvaluationModalOpen] = useState(false);
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    const [existingEvaluation, setExistingEvaluation] = useState(null);
    const [evaluationData, setEvaluationData] = useState({
        score: '',
        comments: '',
        isFinalized: false,
        digitalSignature: '',
    });

    const targetEvaluationType = useMemo(() => getEvaluationTypeForRole(userRole), [userRole]);

    const cargarEvaluacionesAsignaciones = useCallback(async (listaAsignaciones, signal) => {
        const mapa = {};
        await Promise.all(
            listaAsignaciones.map(async (asig) => {
                try {
                    const evals = await evaluationService.getByAssignment(asig.id, { signal });
                    const lista = Array.isArray(evals) ? evals : [];
                    const match =
                        lista.find((ev) => ev.type === targetEvaluationType) ||
                        lista.find((ev) => ev.isFinalized) ||
                        lista[0] ||
                        null;
                    if (match) {
                        mapa[asig.id] = match;
                    }
                } catch (err) {
                    if (err.name !== 'AbortError') {
                        console.error(`Error al cargar evaluación de asignación ${asig.id}:`, err);
                    }
                }
            })
        );
        setEvaluacionesPorAsignacion(mapa);
    }, [targetEvaluationType]);

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

            const lista = Array.isArray(data) ? data : [];
            setAsignaciones(lista);
            await cargarEvaluacionesAsignaciones(lista, signal);
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error('Error al cargar estudiantes:', error);
                toast.error(error.message || 'Error al cargar estudiantes');
            }
        } finally {
            setLoading(false);
        }
    }, [user, userRole, cargarEvaluacionesAsignaciones]);

    useEffect(() => {
        const controller = new AbortController();
        cargarEstudiantes(controller.signal);
        return () => controller.abort();
    }, [cargarEstudiantes]);

    const handleOpenEvaluation = useCallback((assignment) => {
        setSelectedAssignment(assignment);
        const currentEval = evaluacionesPorAsignacion[assignment.id] || null;
        setExistingEvaluation(currentEval);

        if (currentEval) {
            setEvaluationData({
                score: currentEval.score !== undefined && currentEval.score !== null ? String(currentEval.score) : '',
                comments: currentEval.qualitativeComments || '',
                isFinalized: Boolean(currentEval.isFinalized),
                digitalSignature: currentEval.digitalSignature || '',
            });
        } else {
            setEvaluationData({
                score: '',
                comments: '',
                isFinalized: false,
                digitalSignature: '',
            });
        }
        setIsEvaluationModalOpen(true);
    }, [evaluacionesPorAsignacion]);

    const handleCloseEvaluation = () => {
        setIsEvaluationModalOpen(false);
        setSelectedAssignment(null);
        setExistingEvaluation(null);
    };

    const isLocked = Boolean(existingEvaluation?.isFinalized);

    const handleSubmitEvaluation = async (e) => {
        e.preventDefault();
        if (!selectedAssignment || isLocked) return;

        const numericScore = parseFloat(evaluationData.score);
        if (Number.isNaN(numericScore) || numericScore < 1.0 || numericScore > 7.0) {
            toast.error('La calificación debe estar entre 1.0 y 7.0.');
            return;
        }

        if (evaluationData.isFinalized && !evaluationData.digitalSignature.trim()) {
            toast.error('Debe ingresar su firma digital para finalizar y registrar oficialmente la evaluación.');
            return;
        }

        setSubmitting(true);
        try {
            const payload = {
                practiceAssignmentId: selectedAssignment.id,
                type: targetEvaluationType,
                score: numericScore,
                qualitativeComments: evaluationData.comments,
                isFinalized: evaluationData.isFinalized,
                digitalSignature: evaluationData.isFinalized ? evaluationData.digitalSignature.trim() : null,
                criteria: [
                    { criteriaName: 'Responsabilidad y Puntualidad', achievementLevel: 'LOGRO_ALTO', points: 25.0, justification: 'Evaluado general' },
                    { criteriaName: 'Dominio del Grupo', achievementLevel: 'LOGRO_ADECUADO', points: 20.0, justification: 'Evaluado general' }
                ]
            };
const saved = await evaluationService.createOrUpdate(payload);
            setEvaluacionesPorAsignacion((prev) => ({
                ...prev,
                [selectedAssignment.id]: saved,
            }));

            if (evaluationData.isFinalized) {
                toast.success('Evaluación finalizada y firmada digitalmente como calificación oficial e inamovible.');
            } else {
                toast.success('Borrador de pauta de evaluación guardado con éxito.');
            }
            handleCloseEvaluation();
        } catch (error) {
            console.error(error);
            toast.error(error.message || 'Error al guardar la evaluación');
        } finally {
            setSubmitting(false);
        }
    };

    const canEvaluate =
        userRole === 'Profesor Colaborador' ||
        userRole === 'Tutor' ||
        userRole === 'Profesor de Asignatura';

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
            header: 'Estado Práctica',
            align: 'center',
            render: (row) => (
                <Badge variant={row.status === 'ACTIVA' ? 'success' : 'gray'}>
                    {row.status || 'PENDIENTE'}
                </Badge>
            ),
        },
        {
            header: 'Calificación',
            align: 'center',
            render: (row) => {
                const ev = evaluacionesPorAsignacion[row.id];
                if (!ev) {
                    return <Badge variant="gray">Sin calificar</Badge>;
                }
                if (ev.isFinalized) {
                    return (
                        <div className="flex flex-col items-center gap-1">
                            <Badge variant="success">Oficial: {Number(ev.score).toFixed(1)}</Badge>
                            <span className="text-[11px] text-green-700 font-medium">Firmada (Inamovible)</span>
                        </div>
                    );
                }
                return <Badge variant="warning">Borrador: {Number(ev.score).toFixed(1)}</Badge>;
            },
        },
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => {
                if (!canEvaluate) return null;
                const ev = evaluacionesPorAsignacion[row.id];
                const finalized = Boolean(ev?.isFinalized);
                return (
                    <div className="flex items-center justify-end gap-2">
                        <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => navigate(`/portafolio/estudiante/${row.id}`)}
                        >
                            Ver Portafolio
                        </Button>
                        <Button
                            variant={finalized ? 'outline' : 'primary'}
                            size="sm"
                            onClick={() => handleOpenEvaluation(row)}
                        >
                            {finalized ? 'Ver Oficial' : ev ? 'Continuar Pauta' : 'Evaluar (Pauta)'}
                        </Button>
                    </div>
                );
            },
        },
    ], [canEvaluate, evaluacionesPorAsignacion, handleOpenEvaluation]);
    const handleDescargarZip = async () => {
        try {
            const token = localStorage.getItem('token');
            const respuesta = await fetch(`${BASE_URL}/api/portafolio/download-zip`, {
                headers: {
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                }
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
                toast.error('Error al descargar el ZIP de informes.');
            }
        } catch (error) {
            console.error('Error al descargar:', error);
            toast.error('Error al descargar el archivo ZIP');
        }
    };


    return (
        <div className="p-8 max-w-6xl mx-auto space-y-8">
            <div>
<div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-4">
                    <div>
                        <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Mis Estudiantes</h1>
                        <p className="text-gray-500 mt-1">Lleva el control, seguimiento y evaluación oficial de los estudiantes asignados a tu tutela.</p>
                    </div>
                    <button
                        onClick={handleDescargarZip}
                        className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded-lg shadow-sm flex items-center gap-2 transition-colors cursor-pointer"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
                        Descargar Todos los Informes (ZIP)
                    </button>
                </div>
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

            <Modal
                isOpen={isEvaluationModalOpen}
                onClose={handleCloseEvaluation}
                title={isLocked ? 'Calificación Oficial Registrada' : 'Pauta de Evaluación Digital'}
            >
                {selectedAssignment && (
                    <form onSubmit={handleSubmitEvaluation} className="space-y-4">
                        <div className="bg-blue-50 p-4 rounded-lg mb-4">
                            <p className="font-bold text-blue-900">Estudiante: {selectedAssignment.student?.fullName}</p>
                            <p className="text-sm text-blue-700">Asignatura: {selectedAssignment.subject?.name}</p>
                        </div>

                        {isLocked && (
                            <div className="bg-green-50 border border-green-300 rounded-lg p-4 space-y-1">
                                <p className="text-sm font-bold text-green-900">
                                    Calificación Oficial e Inamovible (Firmada Digitalmente)
                                </p>
                                <p className="text-xs text-green-800">
                                    Firmado por: <span className="font-semibold">{existingEvaluation?.digitalSignature}</span>
                                </p>
                                {existingEvaluation?.evaluationDate && (
                                    <p className="text-xs text-green-700">
                                        Fecha de registro oficial: {new Date(existingEvaluation.evaluationDate).toLocaleString('es-CL')}
                                    </p>
                                )}
                                <p className="text-xs text-green-700 italic pt-1">
                                    Esta evaluación ha sido cerrada definitivamente y no admite modificaciones.
                                </p>
                            </div>
                        )}
                        
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Calificación Final (1.0 - 7.0)</label>
                            <Input 
                                type="number" 
                                step="0.1" 
                                min="1.0" 
                                max="7.0" 
                                required 
                                disabled={isLocked}
                                value={evaluationData.score}
                                onChange={(e) => setEvaluationData({...evaluationData, score: e.target.value})}
                                placeholder="Ej: 6.5"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Comentarios Cualitativos y Retroalimentación</label>
                            <textarea 
                                className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:text-gray-600"
                                rows="4"
                                required
                                disabled={isLocked}
                                value={evaluationData.comments}
                                onChange={(e) => setEvaluationData({...evaluationData, comments: e.target.value})}
                                placeholder="Escriba sus observaciones sobre el desempeño del estudiante en el aula..."
                            ></textarea>
                        </div>

                        {!isLocked && (
                            <div className="bg-yellow-50 p-4 rounded-lg border border-yellow-200">
                                <label className="flex items-center space-x-3 mb-2 cursor-pointer">
                                    <input 
                                        type="checkbox" 
                                        className="form-checkbox h-5 w-5 text-blue-600 rounded"
                                        checked={evaluationData.isFinalized}
                                        onChange={(e) => {
                                            const checked = e.target.checked;
                                            setEvaluationData((prev) => ({
                                                ...prev,
                                                isFinalized: checked,
                                                digitalSignature: checked && !prev.digitalSignature && user?.fullName
                                                    ? user.fullName
                                                    : prev.digitalSignature,
                                            }));
                                        }}
                                    />
                                    <span className="text-gray-800 font-medium">Finalizar evaluación definitivamente y firmar digitalmente</span>
                                </label>
                                
                                {evaluationData.isFinalized && (
                                    <div className="mt-3">
                                        <label className="block text-sm font-medium text-gray-700 mb-1">
                                            Firma Digital del Evaluador (Escriba su nombre completo)
                                        </label>
                                        <Input 
                                            type="text" 
                                            required={evaluationData.isFinalized}
                                            value={evaluationData.digitalSignature}
                                            onChange={(e) => setEvaluationData({...evaluationData, digitalSignature: e.target.value})}
                                            placeholder="Nombre completo como firma electrónica"
                                        />
                                        <p className="text-xs text-red-600 mt-1">
                                            Atención: Una vez enviada con firma digital, esta evaluación quedará registrada como calificación oficial e inamovible en el sistema.
                                        </p>
                                    </div>
                                )}
                            </div>
                        )}

                        <div className="flex justify-end gap-3 pt-4 border-t">
                            <Button type="button" variant="outline" onClick={handleCloseEvaluation}>
                                {isLocked ? 'Cerrar' : 'Cancelar'}
                            </Button>
                            {!isLocked && (
                                <Button type="submit" variant="primary" disabled={submitting}>
                                    {submitting
                                        ? 'Procesando...'
                                        : evaluationData.isFinalized
                                        ? 'Firmar y Enviar'
                                        : 'Guardar Borrador'}
                                </Button>
                            )}
                        </div>
                    </form>
                )}
            </Modal>
        </div>
    );
}
