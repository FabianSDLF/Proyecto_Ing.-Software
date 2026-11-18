import React, { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import apiClient from '../services/apiClient';
import evaluationService from '../services/evaluationService';
import { isCurrentPractice } from './PracticeSheet';
import { Badge, Button, Modal } from './ui';

const PROFESSIONAL_EXPECTED_INSTRUMENTS = [
    { type: 'COLAB_ESP_1', name: 'Primera observación de Especialidad (Colaborador)', role: 'Profesor Colaborador', area: 'Especialidad', visit: 1 },
    { type: 'COLAB_ESP_2', name: 'Segunda observación de Especialidad (Colaborador)', role: 'Profesor Colaborador', area: 'Especialidad', visit: 2 },
    { type: 'COLAB_ORI_1', name: 'Primera observación de Orientación (Colaborador)', role: 'Profesor Colaborador', area: 'Orientación', visit: 1 },
    { type: 'COLAB_ORI_2', name: 'Segunda observación de Orientación (Colaborador)', role: 'Profesor Colaborador', area: 'Orientación', visit: 2 },
    { type: 'TUTOR_ESP_1', name: 'Primera observación de Especialidad (Tutor)', role: 'Tutor', area: 'Especialidad', visit: 1 },
    { type: 'TUTOR_ESP_2', name: 'Segunda observación de Especialidad (Tutor)', role: 'Tutor', area: 'Especialidad', visit: 2 },
    { type: 'TUTOR_ORI_1', name: 'Primera observación de Orientación (Tutor)', role: 'Tutor', area: 'Orientación', visit: 1 },
    { type: 'TUTOR_ORI_2', name: 'Segunda observación de Orientación (Tutor)', role: 'Tutor', area: 'Orientación', visit: 2 },
];

function getInstrumentMeta(ev) {
    const match = PROFESSIONAL_EXPECTED_INSTRUMENTS.find((i) => i.type === ev?.type);
    return {
        name: ev?.instrumentName || match?.name || ev?.type || 'Evaluación de Aula',
        area: ev?.area || match?.area || null,
        visit: ev?.visit ?? match?.visit ?? null,
        role:
            ev?.finalizedByRole ||
            ev?.evaluator?.role?.name ||
            match?.role ||
            'Evaluador',
    };
}

export default function StudentEvaluationsPanel({
    assignmentId = null,
    history = false,
    onHistoryChange = null,
}) {
    const [assignments, setAssignments] = useState([]);
    const [evaluationsByAssignment, setEvaluationsByAssignment] = useState({});
    const [selectedPracticeFilter, setSelectedPracticeFilter] = useState('ALL');
    const [selectedPeriodFilter, setSelectedPeriodFilter] = useState('ALL');
    const [loading, setLoading] = useState(true);
    const [detailModalEval, setDetailModalEval] = useState(null);
    const [loadingDetail, setLoadingDetail] = useState(false);

    const loadData = useCallback(async (signal) => {
        setLoading(true);
        try {
            const rawAssignments = assignmentId
                ? await apiClient.get(`/api/asignaciones/${assignmentId}/ficha`, { signal }).then((a) => (a ? [a] : []))
                : await apiClient.get('/api/asignaciones/visibles', { signal, params: { history: true } });

            const list = Array.isArray(rawAssignments) ? rawAssignments : [];
            setAssignments(list);

            const evalMap = {};
            await Promise.all(
                list.map(async (asig) => {
                    try {
                        const evals = await evaluationService.getByAssignment(asig.id, { signal });
                        // Solo evaluaciones publicadas/finalizadas (defensa adicional en cliente)
                        evalMap[asig.id] = (Array.isArray(evals) ? evals : []).filter((e) => Boolean(e?.isFinalized));
                    } catch (err) {
                        if (err.name !== 'AbortError') {
                            evalMap[asig.id] = [];
                        }
                    }
                })
            );
            setEvaluationsByAssignment(evalMap);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error al cargar evaluaciones publicadas:', err);
                toast.error(err.message || 'No se pudieron cargar las evaluaciones');
            }
        } finally {
            setLoading(false);
        }
    }, [assignmentId]);

    useEffect(() => {
        const controller = new AbortController();
        loadData(controller.signal);
        return () => controller.abort();
    }, [loadData]);

    const periods = useMemo(() => {
        const map = new Map();
        assignments.forEach((a) => {
            if (a.period?.id) {
                map.set(String(a.period.id), a.period.name || `Periodo ${a.period.id}`);
            }
        });
        return Array.from(map.entries());
    }, [assignments]);

    const filteredAssignments = useMemo(() => {
        return assignments.filter((a) => {
            if (!assignmentId && !history && !isCurrentPractice(a)) {
                return false;
            }
            if (selectedPracticeFilter !== 'ALL' && String(a.id) !== String(selectedPracticeFilter)) {
                return false;
            }
            if (selectedPeriodFilter !== 'ALL' && String(a.period?.id || '') !== String(selectedPeriodFilter)) {
                return false;
            }
            return true;
        });
    }, [assignments, assignmentId, history, selectedPracticeFilter, selectedPeriodFilter]);

    const handleOpenBreakdown = async (ev) => {
        setDetailModalEval(ev);
        if (!ev?.id) return;
        setLoadingDetail(true);
        try {
            const fullDetail = await evaluationService.getById(ev.id);
            if (fullDetail && fullDetail.isFinalized) {
                setDetailModalEval(fullDetail);
            }
        } catch (err) {
            toast.error(err.message || 'No se pudo cargar el desglose de la evaluación.');
        } finally {
            setLoadingDetail(false);
        }
    };

    const groupedCriteriaByDimension = useMemo(() => {
        if (!detailModalEval?.criteria || !Array.isArray(detailModalEval.criteria)) return [];
        const groups = new Map();
        detailModalEval.criteria.forEach((c, index) => {
            const dim = c.dimension || 'General';
            if (!groups.has(dim)) {
                groups.set(dim, []);
            }
            groups.get(dim).push({
                ...c,
                code: c.criterionCode || `IND-${index + 1}`,
                label: c.text || c.criteriaName || 'Indicador evaluado',
            });
        });
        return Array.from(groups.entries());
    }, [detailModalEval]);

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h2 className="text-xl font-bold text-gray-800">
                        Mis Evaluaciones y Retroalimentación de Aula
                    </h2>
                    <p className="text-sm text-gray-500 mt-1">
                        Consulta tus calificaciones oficiales, puntajes por indicador y observaciones publicadas por tu Tutor y Profesor Colaborador.
                    </p>
                </div>

                {!assignmentId && (
                    <div className="flex flex-wrap items-center gap-3 text-sm">
                        {onHistoryChange && (
                            <label className="font-medium text-gray-700">
                                Alcance
                                <select
                                    aria-label="Filtro de vigencia de evaluaciones"
                                    className="border border-gray-300 rounded-lg p-2 ml-2 bg-white"
                                    value={history ? 'historial' : 'vigente'}
                                    onChange={(e) => onHistoryChange(e.target.value === 'historial')}
                                >
                                    <option value="vigente">Prácticas vigentes</option>
                                    <option value="historial">Todo el historial</option>
                                </select>
                            </label>
                        )}

                        {periods.length > 0 && (
                            <label className="font-medium text-gray-700">
                                Periodo
                                <select
                                    aria-label="Filtrar por periodo académico"
                                    className="border border-gray-300 rounded-lg p-2 ml-2 bg-white"
                                    value={selectedPeriodFilter}
                                    onChange={(e) => setSelectedPeriodFilter(e.target.value)}
                                >
                                    <option value="ALL">Todos los periodos</option>
                                    {periods.map(([id, name]) => (
                                        <option key={id} value={id}>
                                            {name}
                                        </option>
                                    ))}
                                </select>
                            </label>
                        )}

                        {assignments.length > 1 && (
                            <label className="font-medium text-gray-700">
                                Práctica
                                <select
                                    aria-label="Filtrar por práctica"
                                    className="border border-gray-300 rounded-lg p-2 ml-2 bg-white"
                                    value={selectedPracticeFilter}
                                    onChange={(e) => setSelectedPracticeFilter(e.target.value)}
                                >
                                    <option value="ALL">Todas las prácticas</option>
                                    {assignments.map((a) => (
                                        <option key={a.id} value={a.id}>
                                            {a.subject?.name || `Práctica #${a.id}`} ({a.period?.name || 'Sin periodo'})
                                        </option>
                                    ))}
                                </select>
                            </label>
                        )}
                    </div>
                )}
            </div>

            {loading ? (
                <div className="py-10 text-center text-gray-500">
                    Cargando evaluaciones publicadas...
                </div>
            ) : filteredAssignments.length === 0 ? (
                <div className="py-10 text-center text-gray-500 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                    No se encontraron prácticas para los filtros seleccionados.
                </div>
            ) : (
                <div className="space-y-6">
                    {filteredAssignments.map((asig) => {
                        const publishedList = evaluationsByAssignment[asig.id] || [];
                        const publishedTypes = new Set(publishedList.map((e) => e.type));
                        const pendingInstruments = PROFESSIONAL_EXPECTED_INSTRUMENTS.filter(
                            (inst) => !publishedTypes.has(inst.type)
                        );

                        return (
                            <section
                                key={asig.id}
                                className="border border-gray-200 rounded-2xl p-5 space-y-4 bg-gray-50/40"
                            >
                                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-200 pb-3">
                                    <div>
                                        <h3 className="text-lg font-bold text-blue-950">
                                            {asig.subject?.name || 'Práctica Pedagógica'}
                                        </h3>
                                        <p className="text-xs text-gray-600">
                                            Periodo: {asig.period?.name || 'Sin periodo'} • Centro:{' '}
                                            {asig.center?.name || 'Sin centro asignado'}
                                        </p>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <Badge variant={isCurrentPractice(asig) ? 'success' : 'gray'}>
                                            {isCurrentPractice(asig) ? 'Práctica Vigente' : 'Histórica'}
                                        </Badge>
                                        <Badge variant="info">
                                            {publishedList.length} publicada(s)
                                        </Badge>
                                    </div>
                                </div>

                                {publishedList.length === 0 ? (
                                    <div className="p-4 bg-white rounded-xl border border-gray-200 text-sm text-gray-600">
                                        Aún no hay evaluaciones publicadas en esta práctica. Las pautas aparecerán aquí automáticamente una vez que el evaluador las firme digitalmente.
                                    </div>
                                ) : (
                                    <div className="grid grid-cols-1 gap-4">
                                        {publishedList.map((ev) => {
                                            const meta = getInstrumentMeta(ev);
                                            const evaluatorName =
                                                ev.finalizedByName ||
                                                ev.evaluator?.fullName ||
                                                ev.digitalSignature ||
                                                'Evaluador Oficial';
                                            const publishedDate = ev.finalizedAt || ev.evaluationDate || ev.createdAt;

                                            return (
                                                <article
                                                    key={ev.id || ev.type}
                                                    className="bg-white border border-gray-200 rounded-xl p-5 shadow-xs space-y-3"
                                                >
                                                    <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                                                        <div className="space-y-1">
                                                            <div className="flex flex-wrap items-center gap-2">
                                                                <Badge variant="success">Publicada Oficial</Badge>
                                                                {meta.area && (
                                                                    <Badge variant="info">Área: {meta.area}</Badge>
                                                                )}
                                                                {meta.visit && (
                                                                    <Badge variant="gray">Visita {meta.visit}</Badge>
                                                                )}
                                                                {ev.rubricVersion && (
                                                                    <span className="text-xs text-gray-500">
                                                                        Versión {ev.rubricVersion}
                                                                    </span>
                                                                )}
                                                            </div>
                                                            <h4 className="text-base font-bold text-gray-900">
                                                                {meta.name}
                                                            </h4>
                                                            <p className="text-xs text-gray-600">
                                                                Evaluador:{' '}
                                                                <span className="font-semibold text-gray-800">
                                                                    {evaluatorName}
                                                                </span>{' '}
                                                                ({meta.role})
                                                                {publishedDate && (
                                                                    <>
                                                                        {' '}
                                                                        • Publicada el{' '}
                                                                        {new Date(publishedDate).toLocaleString('es-CL')}
                                                                    </>
                                                                )}
                                                            </p>
                                                        </div>

                                                        <div className="flex items-center gap-4 shrink-0">
                                                            {ev.totalPoints != null && ev.maxPoints != null && (
                                                                <div className="text-right">
                                                                    <p className="text-[11px] uppercase text-gray-500 font-semibold">
                                                                        Puntaje
                                                                    </p>
                                                                    <p className="text-sm font-bold text-gray-800">
                                                                        {ev.totalPoints} / {ev.maxPoints} pts
                                                                    </p>
                                                                </div>
                                                            )}
                                                            <div className="bg-emerald-50 border border-emerald-300 rounded-xl px-3.5 py-2 text-center">
                                                                <p className="text-[10px] uppercase font-bold text-emerald-800">
                                                                    Nota Oficial
                                                                </p>
                                                                <p className="text-xl font-extrabold text-emerald-900">
                                                                    {ev.score != null ? Number(ev.score).toFixed(1) : '—'}
                                                                </p>
                                                            </div>
                                                        </div>
                                                    </div>

                                                    <div className="bg-blue-50/50 border border-blue-100 rounded-xl p-3">
                                                        <p className="text-xs font-bold text-blue-900 mb-1">
                                                            Retroalimentación y Observaciones del Evaluador:
                                                        </p>
                                                        <p className="text-sm text-gray-800 whitespace-pre-wrap">
                                                            {ev.qualitativeComments || 'Sin observaciones adicionales registradas.'}
                                                        </p>
                                                    </div>

                                                    <div className="flex justify-end pt-1">
                                                        <Button
                                                            type="button"
                                                            variant="outline"
                                                            size="sm"
                                                            onClick={() => handleOpenBreakdown(ev)}
                                                        >
                                                            Ver Desglose por Indicadores
                                                        </Button>
                                                    </div>
                                                </article>
                                            );
                                        })}
                                    </div>
                                )}

                                {pendingInstruments.length > 0 && (
                                    <details className="bg-white border border-gray-200 rounded-xl p-3.5 text-xs text-gray-600">
                                        <summary className="font-semibold text-gray-700 cursor-pointer">
                                            Instrumentos pendientes de publicación ({pendingInstruments.length})
                                        </summary>
                                        <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2">
                                            {pendingInstruments.map((inst) => (
                                                <div
                                                    key={inst.type}
                                                    className="flex items-center justify-between p-2 rounded-lg bg-gray-50 border border-gray-200"
                                                >
                                                    <div>
                                                        <p className="font-medium text-gray-800">{inst.name}</p>
                                                        <p className="text-[11px] text-gray-500">
                                                            {inst.role} • {inst.area} • Visita {inst.visit}
                                                        </p>
                                                    </div>
                                                    <Badge variant="gray">Pendiente de publicación</Badge>
                                                </div>
                                            ))}
                                        </div>
                                    </details>
                                )}
                            </section>
                        );
                    })}
                </div>
            )}

            <Modal
                isOpen={Boolean(detailModalEval)}
                onClose={() => setDetailModalEval(null)}
                title="Desglose Oficial de Evaluación Publicada (Modo Lectura)"
                size="lg"
            >
                {detailModalEval && (() => {
                    const meta = getInstrumentMeta(detailModalEval);
                    const publishedDate =
                        detailModalEval.finalizedAt ||
                        detailModalEval.evaluationDate ||
                        detailModalEval.createdAt;
                    return (
                        <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
                            <div className="bg-emerald-50 border border-emerald-300 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                                <div className="space-y-1">
                                    <div className="flex flex-wrap items-center gap-2">
                                        <Badge variant="success">Calificación Oficial Firmada</Badge>
                                        {meta.area && <Badge variant="info">Área: {meta.area}</Badge>}
                                        {meta.visit && <Badge variant="gray">Visita {meta.visit}</Badge>}
                                    </div>
                                    <h3 className="text-base font-bold text-emerald-950">{meta.name}</h3>
                                    <p className="text-xs text-emerald-900">
                                        Firmado por:{' '}
                                        <span className="font-semibold">
                                            {detailModalEval.finalizedByName ||
                                                detailModalEval.evaluator?.fullName ||
                                                detailModalEval.digitalSignature ||
                                                'Evaluador Oficial'}
                                        </span>{' '}
                                        ({meta.role})
                                    </p>
                                    {publishedDate && (
                                        <p className="text-xs text-emerald-800">
                                            Fecha de publicación: {new Date(publishedDate).toLocaleString('es-CL')}
                                        </p>
                                    )}
                                    {detailModalEval.rubricVersion && (
                                        <p className="text-xs text-emerald-800">
                                            Versión de plantilla: {detailModalEval.rubricVersion}
                                        </p>
                                    )}
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-xs font-bold text-emerald-800 uppercase">Nota Oficial</p>
                                    <p className="text-3xl font-extrabold text-emerald-950">
                                        {detailModalEval.score != null
                                            ? Number(detailModalEval.score).toFixed(1)
                                            : '—'}
                                    </p>
                                    {detailModalEval.totalPoints != null && detailModalEval.maxPoints != null && (
                                        <p className="text-xs font-semibold text-emerald-900">
                                            {detailModalEval.totalPoints} / {detailModalEval.maxPoints} puntos
                                        </p>
                                    )}
                                </div>
                            </div>

                            <div className="bg-blue-50 border border-blue-200 rounded-xl p-3.5">
                                <p className="text-xs font-bold text-blue-900 mb-1">
                                    Retroalimentación y Observaciones Generales:
                                </p>
                                <p className="text-sm text-gray-800 whitespace-pre-wrap">
                                    {detailModalEval.qualitativeComments || 'Sin observaciones registradas.'}
                                </p>
                            </div>

                            {loadingDetail ? (
                                <div className="py-6 text-center text-gray-500">
                                    Cargando desglose de indicadores...
                                </div>
                            ) : groupedCriteriaByDimension.length === 0 ? (
                                <div className="p-4 bg-gray-50 border border-gray-200 rounded-xl text-sm text-gray-600">
                                    Esta evaluación histórica fue registrada de forma global sin desglose por indicadores.
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {groupedCriteriaByDimension.map(([dimension, items]) => (
                                        <div
                                            key={dimension}
                                            className="border border-gray-200 rounded-xl overflow-hidden"
                                        >
                                            <div className="bg-gray-100 px-4 py-2 text-xs font-bold uppercase tracking-wider text-gray-800">
                                                Dimensión {dimension}
                                            </div>
                                            <div className="divide-y divide-gray-200">
                                                {items.map((item) => (
                                                    <div
                                                        key={item.code}
                                                        className="p-3 bg-white flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                                                    >
                                                        <div className="space-y-0.5 flex-1">
                                                            <span className="inline-block px-2 py-0.5 text-xs font-bold rounded bg-blue-100 text-blue-900 mr-2">
                                                                {item.code}
                                                            </span>
                                                            <span className="text-sm text-gray-800">
                                                                {item.label}
                                                            </span>
                                                        </div>
                                                        <div className="shrink-0">
                                                            <Badge variant="info">
                                                                {item.points != null
                                                                    ? `${Number(item.points)} / 7 pts`
                                                                    : '—'}
                                                            </Badge>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            <div className="flex justify-end pt-3 border-t border-gray-200">
                                <Button
                                    type="button"
                                    variant="secondary"
                                    onClick={() => setDetailModalEval(null)}
                                >
                                    Cerrar
                                </Button>
                            </div>
                        </div>
                    );
                })()}
            </Modal>
        </div>
    );
}
