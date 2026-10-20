import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { BASE_URL } from '../services/apiClient';
import assignmentService from '../services/assignmentService';
import { DataTable, Badge, Modal, Button, Input } from '../components/ui';

export default function MisEstudiantes() {
    const { user, userRole } = useAuth();
    const [asignaciones, setAsignaciones] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isEvaluationModalOpen, setIsEvaluationModalOpen] = useState(false);
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    const [evaluationData, setEvaluationData] = useState({ score: '', comments: '', isFinalized: false, digitalSignature: '' });

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

    const handleOpenEvaluation = (assignment) => {
        setSelectedAssignment(assignment);
        setEvaluationData({ score: '', comments: '', isFinalized: false, digitalSignature: '' });
        setIsEvaluationModalOpen(true);
    };

    const handleCloseEvaluation = () => {
        setIsEvaluationModalOpen(false);
        setSelectedAssignment(null);
    };

    const handleSubmitEvaluation = async (e) => {
        e.preventDefault();
        try {
            const token = localStorage.getItem('token');
            const payload = {
                practiceAssignmentId: selectedAssignment.id,
                type: 'PAUTA_OBSERVACION_COLABORADOR',
                score: parseFloat(evaluationData.score),
                qualitativeComments: evaluationData.comments,
                isFinalized: evaluationData.isFinalized,
                digitalSignature: evaluationData.isFinalized ? evaluationData.digitalSignature : null,
                criteria: [
                    { criteriaName: 'Responsabilidad y Puntualidad', achievementLevel: 'LOGRO_ALTO', points: 25.0, justification: 'Evaluado general' },
                    { criteriaName: 'Dominio del Grupo', achievementLevel: 'LOGRO_ADECUADO', points: 20.0, justification: 'Evaluado general' }
                ]
            };
            const res = await fetch(`${BASE_URL}/api/evaluaciones`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });
            
            if (!res.ok) throw new Error('Error al guardar la evaluación');
            toast.success('Pauta de evaluación guardada con éxito');
            handleCloseEvaluation();
        } catch (error) {
            console.error(error);
            toast.error(error.message);
        }
    };

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
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => (
                userRole === 'Profesor Colaborador' && (
                    <Button variant="primary" size="sm" onClick={() => handleOpenEvaluation(row)}>
                        Evaluar (Pauta)
                    </Button>
                )
            ),
        },
    ], [userRole]);

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

            <Modal isOpen={isEvaluationModalOpen} onClose={handleCloseEvaluation} title="Pauta de Evaluación Digital">
                {selectedAssignment && (
                    <form onSubmit={handleSubmitEvaluation} className="space-y-4">
                        <div className="bg-blue-50 p-4 rounded-lg mb-4">
                            <p className="font-bold text-blue-900">Estudiante: {selectedAssignment.student?.fullName}</p>
                            <p className="text-sm text-blue-700">Asignatura: {selectedAssignment.subject?.name}</p>
                        </div>
                        
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Calificación Final (1.0 - 7.0)</label>
                            <Input 
                                type="number" 
                                step="0.1" 
                                min="1.0" 
                                max="7.0" 
                                required 
                                value={evaluationData.score}
                                onChange={(e) => setEvaluationData({...evaluationData, score: e.target.value})}
                                placeholder="Ej: 6.5"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Comentarios Cualitativos y Retroalimentación</label>
                            <textarea 
                                className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                                rows="4"
                                required
                                value={evaluationData.comments}
                                onChange={(e) => setEvaluationData({...evaluationData, comments: e.target.value})}
                                placeholder="Escriba sus observaciones sobre el desempeño del estudiante en el aula..."
                            ></textarea>
                        </div>

                        <div className="bg-yellow-50 p-4 rounded-lg border border-yellow-200">
                            <label className="flex items-center space-x-3 mb-3">
                                <input 
                                    type="checkbox" 
                                    className="form-checkbox h-5 w-5 text-blue-600 rounded"
                                    checked={evaluationData.isFinalized}
                                    onChange={(e) => setEvaluationData({...evaluationData, isFinalized: e.target.checked})}
                                />
                                <span className="text-gray-800 font-medium">Finalizar evaluación definitivamente</span>
                            </label>
                            
                            {evaluationData.isFinalized && (
                                <div className="mt-3">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Firma Digital (Escriba su nombre completo)</label>
                                    <Input 
                                        type="text" 
                                        required={evaluationData.isFinalized}
                                        value={evaluationData.digitalSignature}
                                        onChange={(e) => setEvaluationData({...evaluationData, digitalSignature: e.target.value})}
                                        placeholder="Firma electrónica"
                                    />
                                    <p className="text-xs text-red-600 mt-1">Atención: Una vez enviada con firma digital, esta evaluación será inamovible y se registrará como la calificación oficial.</p>
                                </div>
                            )}
                        </div>

                        <div className="flex justify-end gap-3 pt-4 border-t">
                            <Button type="button" variant="outline" onClick={handleCloseEvaluation}>Cancelar</Button>
                            <Button type="submit" variant="primary">
                                {evaluationData.isFinalized ? 'Firmar y Enviar' : 'Guardar Pauta'}
                            </Button>
                        </div>
                    </form>
                )}
            </Modal>
        </div>
    );
}
