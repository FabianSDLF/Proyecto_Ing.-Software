import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { BASE_URL } from '../services/apiClient';
import assignmentService from '../services/assignmentService';
import evaluationService, { calculatePreliminaryGrade } from '../services/evaluationService';
import EvaluationForm from '../components/EvaluationForm';
import FinalizeEvaluationDialog from '../components/FinalizeEvaluationDialog';
import { DataTable, Badge, Modal, Button, Input, ConfirmDialog } from '../components/ui';

function getDefaultEvaluationTypeForRole(role) {
    if (role === 'Tutor') return 'TUTOR_ESP_1';
    if (role === 'Profesor Colaborador') return 'COLAB_ESP_1';
    if (role === 'Profesor de Asignatura') return 'RUBRICA_INFORME';
    return 'COLAB_ESP_1';
}

export default function MisEstudiantes() {
    const { user, userRole } = useAuth();
    const navigate = useNavigate();
    const [asignaciones, setAsignaciones] = useState([]);
    const [todasEvaluacionesPorAsignacion, setTodasEvaluacionesPorAsignacion] = useState({});
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [isEvaluationModalOpen, setIsEvaluationModalOpen] = useState(false);
    const [isFinalizeDialogOpen, setIsFinalizeDialogOpen] = useState(false);
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    const [existingEvaluation, setExistingEvaluation] = useState(null);
    const [conflictMessage, setConflictMessage] = useState(null);

    const [evaluationData, setEvaluationData] = useState({
        score: '',
        comments: '',
    });
    const [indicatorPoints, setIndicatorPoints] = useState({});

    const [rubrics, setRubrics] = useState([]);
    const [selectedRubric, setSelectedRubric] = useState(() => getDefaultEvaluationTypeForRole(userRole));
    const [history, setHistory] = useState(false);
    const [savedForm, setSavedForm] = useState('');
    const [discardOpen, setDiscardOpen] = useState(false);
    const dirty = isEvaluationModalOpen && !existingEvaluation?.isFinalized && savedForm !== JSON.stringify({ evaluationData, indicatorPoints });
    useEffect(() => {
        if (!dirty) return;
        const warn = e => { e.preventDefault(); e.returnValue = ''; };
        window.addEventListener('beforeunload', warn);
        return () => window.removeEventListener('beforeunload', warn);
    }, [dirty]);

    const usesProfessionalRubrics = userRole === 'Profesor Colaborador' || userRole === 'Tutor';

    useEffect(() => {
        const defaultType = getDefaultEvaluationTypeForRole(userRole);
        setSelectedRubric(defaultType);

        if (userRole === 'Profesor Colaborador') {
            evaluationService
                .getCollaboratorRubrics()
                .then((data) => {
                    const list = Array.isArray(data) ? data : [];
                    setRubrics(list);
                    if (list.length > 0 && !list.some((r) => r.type === defaultType)) {
                        setSelectedRubric(list[0].type);
                    }
                })
                .catch((err) => toast.error(err.message || 'Error al cargar pautas del colaborador'));
        } else if (userRole === 'Tutor') {
            evaluationService
                .getTutorRubrics()
                .then((data) => {
                    const list = Array.isArray(data) ? data : [];
                    setRubrics(list);
                    if (list.length > 0 && !list.some((r) => r.type === defaultType)) {
                        setSelectedRubric(list[0].type);
                    }
                })
                .catch((err) => toast.error(err.message || 'Error al cargar pautas del tutor'));
        } else {
            setRubrics([]);
        }
    }, [userRole]);

    const targetEvaluationType = usesProfessionalRubrics
        ? selectedRubric
        : getDefaultEvaluationTypeForRole(userRole);

    const activeRubric = useMemo(
        () => rubrics.find((r) => r.type === targetEvaluationType) || null,
        [rubrics, targetEvaluationType]
    );

    const cargarEvaluacionesAsignaciones = useCallback(async (listaAsignaciones, signal) => {
        const mapa = {};
        await Promise.all(
            listaAsignaciones.map(async (asig) => {
                try {
                    const evals = await evaluationService.getByAssignment(asig.id, { signal });
                    mapa[asig.id] = Array.isArray(evals) ? evals : [];
                } catch (err) {
                    if (err.name !== 'AbortError') {
                        console.error(`Error al cargar evaluación de asignación ${asig.id}:`, err);
                    }
                }
            })
        );
        setTodasEvaluacionesPorAsignacion(mapa);
    }, []);

    const evaluacionesPorAsignacion = useMemo(() => {
        const result = {};
        Object.entries(todasEvaluacionesPorAsignacion).forEach(([asigId, lista]) => {
            const match = (lista || []).find((ev) => ev.type === targetEvaluationType) || null;
            if (match) {
                result[asigId] = match;
            }
        });
        return result;
    }, [todasEvaluacionesPorAsignacion, targetEvaluationType]);

    const hasHistoricalLegacyRecords = useMemo(() => {
        const legacyType =
            userRole === 'Tutor' ? 'PAUTA_OBSERVACION_TUTOR' : 'PAUTA_OBSERVACION_COLABORADOR';
        return Object.values(todasEvaluacionesPorAsignacion).some((lista) =>
            (lista || []).some((ev) => ev.type === legacyType)
        );
    }, [todasEvaluacionesPorAsignacion, userRole]);

    const cargarEstudiantes = useCallback(async (signal) => {
        if (!user) return;
        setLoading(true);

        try {
            let data = [];
            if (userRole === 'Profesor de Asignatura') {
                data = await assignmentService.getByProfessor(user.id, { signal, params: { history } });
            } else if (userRole === 'Profesor Colaborador') {
                data = await assignmentService.getByCollaborator(user.id, { signal, params: { history } });
            } else if (userRole === 'Tutor') {
                data = await assignmentService.getByTutor(user.id, { signal, params: { history } });
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
    }, [user, userRole, cargarEvaluacionesAsignaciones, history]);

    useEffect(() => {
        const controller = new AbortController();
        cargarEstudiantes(controller.signal);
        return () => controller.abort();
    }, [cargarEstudiantes]);

    const hydrateFormFromEvaluation = useCallback((evalRecord) => {
        if (evalRecord) {
            setEvaluationData({
                score:
                    evalRecord.score !== undefined && evalRecord.score !== null
                        ? String(evalRecord.score)
                        : '',
                comments: evalRecord.qualitativeComments || '',
            });
            const pointsMap = {};
            if (Array.isArray(evalRecord.criteria)) {
                evalRecord.criteria.forEach((c) => {
                    const code = c.criterionCode || c.criteriaName;
                    if (code && c.points != null) {
                        pointsMap[code] = Number(c.points);
                    }
                });
            }
            setIndicatorPoints(pointsMap);
            setSavedForm(JSON.stringify({ evaluationData: { score: evalRecord.score != null ? String(evalRecord.score) : '', comments: evalRecord.qualitativeComments || '' }, indicatorPoints: pointsMap }));
        } else {
            setEvaluationData({
                score: '',
                comments: '',
            });
            setIndicatorPoints({});
            setSavedForm(JSON.stringify({ evaluationData: { score: '', comments: '' }, indicatorPoints: {} }));
        }
    }, []);

    const handleOpenEvaluation = useCallback((assignment) => {
        if (usesProfessionalRubrics && assignment.subject?.professionalRubrics === false && !evaluacionesPorAsignacion[assignment.id]) {
            toast.error("Esta asignatura no tiene configuradas pautas profesionales. Consulta a coordinación.");
            return;
        }
        setSelectedAssignment(assignment);
        setConflictMessage(null);
        const currentEval = evaluacionesPorAsignacion[assignment.id] || null;
        setExistingEvaluation(currentEval);
        hydrateFormFromEvaluation(currentEval);
        setIsEvaluationModalOpen(true);
    }, [evaluacionesPorAsignacion, hydrateFormFromEvaluation, usesProfessionalRubrics]);

    const handleCloseEvaluation = (discard = false) => {
        if (submitting) return;
        if (dirty && discard !== true) { setDiscardOpen(true); return; }
        setDiscardOpen(false);
        setIsFinalizeDialogOpen(false);
        setIsEvaluationModalOpen(false);
        setSelectedAssignment(null);
        setExistingEvaluation(null);
        setConflictMessage(null);
    };

    const reloadCurrentEvaluationAfterConflict = useCallback(async (assignmentId) => {
        try {
            const evals = await evaluationService.getByAssignment(assignmentId);
            const lista = Array.isArray(evals) ? evals : [];
            setTodasEvaluacionesPorAsignacion((prev) => ({
                ...prev,
                [assignmentId]: lista,
            }));
            const current = lista.find((ev) => ev.type === targetEvaluationType) || null;
            setExistingEvaluation(current);
            hydrateFormFromEvaluation(current);
            return current;
        } catch (err) {
            console.error('Error al recuperar versión actual de la pauta:', err);
            return null;
        }
    }, [targetEvaluationType, hydrateFormFromEvaluation]);

    const isLocked = Boolean(existingEvaluation?.isFinalized);
    const isCurrentRubricType = Boolean(activeRubric);

    const displayedIndicators = useMemo(() => {
        if (isLocked && Array.isArray(existingEvaluation?.criteria) && existingEvaluation.criteria.length > 0) {
            return existingEvaluation.criteria.map((c, idx) => ({
                id: c.criterionCode || c.criteriaName || `IND-${idx + 1}`,
                criterionCode: c.criterionCode || c.criteriaName || `IND-${idx + 1}`,
                dimension: c.dimension || 'General',
                order: c.order || idx + 1,
                text: c.text || c.criteriaName || '',
                points: c.points,
            }));
        }
        return activeRubric?.indicators || [];
    }, [isLocked, existingEvaluation, activeRubric]);

    const rubricProgress = useMemo(() => {
        if (!isCurrentRubricType) return null;
        const indicators = activeRubric?.indicators || [];
        const totalCount = indicators.length;
        let observedCount = 0;
        let sumPoints = 0;
        indicators.forEach((ind) => {
            const code = ind.criterionCode || ind.id;
            const val = indicatorPoints[code];
            if (val !== undefined && val !== null && val !== '') {
                observedCount += 1;
                sumPoints += Number(val);
            }
        });
        const maxPoints = activeRubric?.maxPoints || totalCount * 7;
        const isComplete = totalCount > 0 && observedCount === totalCount;
        const preliminaryScore = isComplete
            ? calculatePreliminaryGrade(sumPoints, maxPoints)
            : null;
        return {
            observedCount,
            totalCount,
            sumPoints,
            maxPoints,
            isComplete,
            preliminaryScore,
        };
    }, [isCurrentRubricType, activeRubric, indicatorPoints]);

    const handleSetIndicatorPoint = (code, value) => {
        if (isLocked) return;
        setIndicatorPoints((prev) => {
            const next = { ...prev };
            if (value === '' || value === null || value === undefined) {
                delete next[code];
            } else {
                next[code] = Number(value);
            }
            return next;
        });
    };

    const handleSetAllIndicators = (pointValue) => {
        if (isLocked || !activeRubric?.indicators) return;
        const next = {};
        activeRubric.indicators.forEach((ind) => {
            const code = ind.criterionCode || ind.id;
            next[code] = Number(pointValue);
        });
        setIndicatorPoints(next);
    };

    const buildDraftPayload = () => {
        const criteriaList = [];
        if (isCurrentRubricType && activeRubric?.indicators) {
            activeRubric.indicators.forEach((ind) => {
                const code = ind.criterionCode || ind.id;
                const pts = indicatorPoints[code];
                if (pts !== undefined && pts !== null && pts !== '') {
                    criteriaList.push({
                        criterionCode: code,
                        points: Number(pts),
                    });
                }
            });
        }
        const numericScore = !isCurrentRubricType && evaluationData.score !== ''
            ? parseFloat(evaluationData.score)
            : null;

        return {
            practiceAssignmentId: selectedAssignment.id,
            type: targetEvaluationType,
            rubricVersion: activeRubric?.rubricVersion || '2025-2-v1',
            expectedRevision: existingEvaluation?.revision ?? null,
            score: numericScore,
            qualitativeComments: evaluationData.comments,
            isFinalized: false,
            criteria: criteriaList,
        };
    };

    const updateSavedEvaluationInState = (saved) => {
        setExistingEvaluation(saved);
        hydrateFormFromEvaluation(saved);
        setTodasEvaluacionesPorAsignacion((prev) => {
            const asigId = selectedAssignment.id;
            const currentList = prev[asigId] || [];
            const filtered = currentList.filter((ev) => ev.type !== saved.type);
            return {
                ...prev,
                [asigId]: [...filtered, saved],
            };
        });
    };

    const handleSaveDraft = async (e) => {
        if (e) e.preventDefault();
        if (!selectedAssignment || isLocked) return;

        if (!isCurrentRubricType) {
            const numericScore = parseFloat(evaluationData.score);
            if (Number.isNaN(numericScore) || numericScore < 1.0 || numericScore > 7.0) {
                toast.error('La calificación debe estar entre 1.0 y 7.0.');
                return;
            }
        }

        setSubmitting(true);
        setConflictMessage(null);
        try {
            const payload = buildDraftPayload();
            const saved = await evaluationService.createOrUpdate(payload);
            updateSavedEvaluationInState(saved);
            toast.success('Borrador de pauta de evaluación guardado con éxito.');
        } catch (error) {
            console.error(error);
            if (error?.status === 409) {
                const msg =
                    'La pauta cambió en otra sesión o ya fue firmada oficialmente. Se ha recuperado el estado actual.';
                setConflictMessage(msg);
                toast.error(msg);
                await reloadCurrentEvaluationAfterConflict(selectedAssignment.id);
            } else {
                toast.error(error.message || 'Error al guardar el borrador');
            }
        } finally {
            setSubmitting(false);
        }
    };

    const handlePrepareFinalize = async () => {
        if (!selectedAssignment || isLocked) return;

        if (isCurrentRubricType) {
            if (!rubricProgress?.isComplete) {
                toast.error(
                    `Debe calificar todos los indicadores oficiales (${rubricProgress?.observedCount ?? 0} de ${rubricProgress?.totalCount ?? 0}) con puntajes entre 2 y 7 antes de firmar.`
                );
                return;
            }
        } else {
            const numericScore = parseFloat(evaluationData.score);
            if (Number.isNaN(numericScore) || numericScore < 1.0 || numericScore > 7.0) {
                toast.error('La calificación debe estar entre 1.0 y 7.0 para poder firmar.');
                return;
            }
        }

        if (!evaluationData.comments || !evaluationData.comments.trim()) {
            toast.error('Ingrese comentarios cualitativos y retroalimentación antes de firmar la evaluación.');
            return;
        }

        setSubmitting(true);
        setConflictMessage(null);
        try {
            const payload = buildDraftPayload();
            const saved = await evaluationService.createOrUpdate(payload);
            updateSavedEvaluationInState(saved);
            setIsFinalizeDialogOpen(true);
        } catch (error) {
            console.error(error);
            if (error?.status === 409) {
                const msg =
                    'La pauta cambió en otra sesión o ya fue firmada oficialmente. Se ha recuperado el estado actual.';
                setConflictMessage(msg);
                toast.error(msg);
                await reloadCurrentEvaluationAfterConflict(selectedAssignment.id);
            } else {
                toast.error(error.message || 'Error al validar la pauta antes de firmar');
            }
        } finally {
            setSubmitting(false);
        }
    };

    const handleConfirmFinalize = async (evaluationToClose) => {
        if (!evaluationToClose?.id) return;
        setSubmitting(true);
        setConflictMessage(null);
        try {
            const closed = await evaluationService.finalize(evaluationToClose.id, {
                expectedRevision: evaluationToClose.revision ?? 1,
                confirmed: true,
            });
            updateSavedEvaluationInState(closed);
            setIsFinalizeDialogOpen(false);
            toast.success(
                'Evaluación finalizada, firmada digitalmente y publicada como calificación oficial e inamovible.'
            );
        } catch (error) {
            console.error(error);
            if (error?.status === 409) {
                const msg =
                    'Conflicto (409): La pauta fue modificada o ya había sido finalizada. Se ha recuperado su estado oficial actual.';
                setConflictMessage(msg);
                toast.error(msg);
                const latest = await reloadCurrentEvaluationAfterConflict(selectedAssignment.id);
                if (latest?.isFinalized) {
                    setIsFinalizeDialogOpen(false);
                }
            } else {
                toast.error(error.message || 'Error al firmar y finalizar la evaluación');
            }
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
            header: 'Calificación (Instrumento Activo)',
            align: 'center',
            render: (row) => {
                const ev = evaluacionesPorAsignacion[row.id];
                if (!ev) {
                    return <Badge variant="gray">Sin calificar</Badge>;
                }
                if (ev.isFinalized) {
                    return (
                        <div className="flex flex-col items-center gap-1">
                            <Badge variant="success">
                                Oficial: {ev.score != null ? Number(ev.score).toFixed(1) : '—'}
                            </Badge>
                            <span className="text-[11px] text-green-700 font-medium">
                                Firmada (Publicada)
                            </span>
                        </div>
                    );
                }
                return (
                    <div className="flex flex-col items-center gap-1">
                        <Badge variant="warning">
                            {ev.score != null
                                ? `Borrador: ${Number(ev.score).toFixed(1)}`
                                : 'Borrador en progreso'}
                        </Badge>
                        {ev.totalPoints != null && ev.maxPoints != null && (
                            <span className="text-[11px] text-amber-800">
                                {ev.totalPoints} / {ev.maxPoints} pts
                            </span>
                        )}
                    </div>
                );
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
    ], [canEvaluate, evaluacionesPorAsignacion, handleOpenEvaluation, navigate]);

    const handleDescargarZip = async () => {
        try {
            const token = localStorage.getItem('token');
            const respuesta = await fetch(`${BASE_URL}/api/portafolio/download-zip?history=${history}`, {
                headers: {
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
                },
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

    const legacyHistoricalType =
        userRole === 'Tutor' ? 'PAUTA_OBSERVACION_TUTOR' : 'PAUTA_OBSERVACION_COLABORADOR';

    return (
        <div className="p-2 sm:p-6 max-w-6xl mx-auto space-y-8">
            <div>
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-4">
                    <div>
                        <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Mis Estudiantes</h1>
                        <p className="text-gray-500 mt-1">
                            Lleva el control, seguimiento y evaluación estandarizada de los estudiantes asignados a tu supervisión.
                        </p>
                    </div>
                    <button
                        hidden={userRole !== 'Profesor de Asignatura'}
                        onClick={handleDescargarZip}
                        className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded-lg shadow-sm flex items-center gap-2 transition-colors cursor-pointer"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path>
                        </svg>
                        Descargar Todos los Informes (ZIP)
                    </button>
                </div>
            </div>

            <div className="bg-white p-3 sm:p-6 rounded-2xl shadow-sm border border-gray-100">
                <div className="flex flex-wrap items-center gap-4 mb-4">
                    <label className="text-sm font-medium text-gray-700">
                        Mostrar
                        <select
                            value={history ? 'historial' : 'vigente'}
                            onChange={(e) => setHistory(e.target.value === 'historial')}
                            className="border border-gray-300 rounded-lg p-2 ml-2 bg-white max-w-full"
                        >
                            <option value="vigente">Prácticas vigentes</option>
                            <option value="historial">Todo el historial</option>
                        </select>
                    </label>

                    {usesProfessionalRubrics && (
                        <label className="text-sm font-medium text-gray-700 min-w-0 max-w-full">
                            Pauta
                            <select
                                aria-label="Pauta de evaluación"
                                className="border border-gray-300 rounded-lg p-2 bg-white max-w-full w-full sm:w-auto"
                                value={selectedRubric}
                                onChange={(e) => setSelectedRubric(e.target.value)}
                            >
                                {rubrics.map((r) => (
                                    <option key={r.type} value={r.type}>
                                        {r.name} ({r.area} • Visita {r.visit})
                                    </option>
                                ))}
                                {hasHistoricalLegacyRecords && (
                                    <option value={legacyHistoricalType}>
                                        Pauta Histórica ({legacyHistoricalType})
                                    </option>
                                )}
                            </select>
                        </label>
                    )}
                </div>

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
                onClose={() => handleCloseEvaluation()}
                title={isLocked ? 'Calificación Oficial Registrada (Modo Lectura)' : 'Pauta de Evaluación Digital'}
                size="lg"
            >
                {selectedAssignment && (
                    <div className="space-y-4 pr-1">
                        <div className="bg-blue-50 p-4 rounded-xl border border-blue-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                            <div>
                                <p className="font-bold text-blue-900">
                                    Estudiante: {selectedAssignment.student?.fullName}
                                </p>
                                <p className="text-sm text-blue-700">
                                    Asignatura: {selectedAssignment.subject?.name}
                                </p>
                                {activeRubric && (
                                    <p className="text-xs text-blue-800 font-medium mt-1">
                                        Instrumento: {activeRubric.name} • Área: {activeRubric.area} • Visita {activeRubric.visit} • Versión {existingEvaluation?.rubricVersion || activeRubric.rubricVersion || '2025-2-v1'}
                                    </p>
                                )}
                            </div>
                            <div className="flex flex-col items-end gap-1">
                                <Badge variant={isLocked ? 'success' : 'warning'}>
                                    {isLocked ? 'Firmada y Publicada' : 'Borrador Privado'}
                                </Badge>
                                {existingEvaluation?.revision != null && (
                                    <span className="text-xs text-gray-500">
                                        Revisión #{existingEvaluation.revision}
                                    </span>
                                )}
                            </div>
                        </div>

                        {conflictMessage && (
                            <div role="alert" className="bg-red-50 border border-red-300 rounded-xl p-3 text-xs text-red-800 font-medium">
                                {conflictMessage}
                            </div>
                        )}

                        {isLocked && (
                            <div className="bg-green-50 border border-green-300 rounded-xl p-4 space-y-1.5">
                                <p className="text-sm font-bold text-green-900">
                                    Calificación Oficial e Inamovible (Firmada Digitalmente y Publicada)
                                </p>
                                <p className="text-xs text-green-800">
                                    Firmado por:{' '}
                                    <span className="font-semibold">
                                        {existingEvaluation?.finalizedByName || existingEvaluation?.digitalSignature || 'Evaluador Oficial'}
                                    </span>
                                    {existingEvaluation?.finalizedByEmail
                                        ? ` <${existingEvaluation.finalizedByEmail}>`
                                        : ''}
                                    {existingEvaluation?.finalizedByRole
                                        ? ` (${existingEvaluation.finalizedByRole})`
                                        : ''}
                                </p>
                                {(existingEvaluation?.finalizedAt || existingEvaluation?.evaluationDate) && (
                                    <p className="text-xs text-green-700">
                                        Fecha de cierre y publicación:{' '}
                                        {new Date(
                                            existingEvaluation.finalizedAt || existingEvaluation.evaluationDate
                                        ).toLocaleString('es-CL')}
                                    </p>
                                )}
                                {existingEvaluation?.totalPoints != null && existingEvaluation?.maxPoints != null && (
                                    <p className="text-xs text-green-800 font-semibold">
                                        Puntaje Oficial: {existingEvaluation.totalPoints} / {existingEvaluation.maxPoints} pts • Nota Oficial:{' '}
                                        {existingEvaluation.score != null ? Number(existingEvaluation.score).toFixed(1) : '—'}
                                    </p>
                                )}
                                {existingEvaluation?.snapshotHash && (
                                    <p className="text-[11px] text-green-700 font-mono break-all">
                                        SHA-256: {existingEvaluation.snapshotHash}
                                    </p>
                                )}
                                <p className="text-xs text-green-700 italic pt-1">
                                    Esta evaluación ha sido cerrada definitivamente y no admite modificaciones.
                                </p>
                            </div>
                        )}

                        {(isCurrentRubricType || (isLocked && displayedIndicators.length > 0)) && (
                            <EvaluationForm existingEvaluation={existingEvaluation} activeRubric={activeRubric} isLocked={isLocked} rubricProgress={rubricProgress} handleSetAllIndicators={handleSetAllIndicators} displayedIndicators={displayedIndicators} indicatorPoints={indicatorPoints} handleSetIndicatorPoint={handleSetIndicatorPoint} />
                        )}

                        <form onSubmit={handleSaveDraft} className="space-y-4">
                            {!isCurrentRubricType && (
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Calificación Final (1.0 - 7.0)
                                    </label>
                                    <Input
                                        type="number"
                                        step="0.1"
                                        min="1.0"
                                        max="7.0"
                                        required
                                        disabled={isLocked}
                                        value={evaluationData.score}
                                        onChange={(e) => setEvaluationData({ ...evaluationData, score: e.target.value })}
                                        placeholder="Ej: 6.5"
                                    />
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Comentarios Cualitativos y Retroalimentación
                                </label>
                                <textarea
                                    className="w-full p-2.5 border border-gray-300 rounded-xl focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:text-gray-600 text-sm"
                                    rows="4"
                                    maxLength={1000}
                                    aria-label="Comentarios cualitativos"
                                    disabled={isLocked}
                                    value={evaluationData.comments}
                                    onChange={(e) => setEvaluationData({ ...evaluationData, comments: e.target.value })}
                                    placeholder="Escriba sus observaciones sobre el desempeño del estudiante en el aula..."
                                ></textarea>
                            </div>

                            <div className="flex flex-wrap justify-end gap-3 pt-4 border-t border-gray-200">
                                <Button type="button" variant="outline" onClick={() => handleCloseEvaluation()}>
                                    {isLocked ? 'Cerrar' : 'Cancelar'}
                                </Button>
                                {!isLocked && (
                                    <>
                                        <Button
                                            type="submit"
                                            variant="secondary"
                                            disabled={submitting}
                                        >
                                            {submitting ? 'Guardando...' : 'Guardar Borrador'}
                                        </Button>
                                        <Button
                                            type="button"
                                            variant="primary"
                                            disabled={submitting}
                                            onClick={handlePrepareFinalize}
                                        >
                                            {submitting ? 'Procesando...' : 'Revisar y Firmar Oficialmente'}
                                        </Button>
                                    </>
                                )}
                            </div>
                        </form>
                    </div>
                )}
            </Modal>

            <ConfirmDialog isOpen={discardOpen} title="Cambios sin guardar" message="Hay cambios en la pauta que aún no has guardado. ¿Quieres descartarlos?" confirmText="Descartar cambios" cancelText="Seguir editando" onConfirm={() => handleCloseEvaluation(true)} onCancel={() => setDiscardOpen(false)} />
            <FinalizeEvaluationDialog
                isOpen={isFinalizeDialogOpen}
                onClose={() => setIsFinalizeDialogOpen(false)}
                onConfirm={handleConfirmFinalize}
                evaluation={existingEvaluation}
                assignment={selectedAssignment}
                rubric={activeRubric}
                user={user}
                userRole={userRole}
                submitting={submitting}
                conflictError={conflictMessage}
            />
        </div>
    );
}
