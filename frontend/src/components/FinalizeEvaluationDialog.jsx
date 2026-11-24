import React, { useState, useEffect } from 'react';
import { Modal, Button, Badge } from './ui';

export default function FinalizeEvaluationDialog({
    isOpen,
    onClose,
    onConfirm,
    evaluation,
    assignment,
    rubric,
    user,
    userRole,
    submitting = false,
    conflictError = null,
}) {
    const [confirmed, setConfirmed] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setConfirmed(false);
        }
    }, [isOpen, evaluation?.id, evaluation?.revision]);

    if (!isOpen || !evaluation) return null;

    const instrumentName =
        evaluation.instrumentName ||
        rubric?.name ||
        evaluation.type ||
        'Pauta de Evaluación';
    const area = evaluation.area || rubric?.area || null;
    const visit = evaluation.visit || rubric?.visit || null;
    const rubricVersion = evaluation.rubricVersion || rubric?.rubricVersion || '2025-2-v1';
    const totalPoints = evaluation.totalPoints ?? null;
    const maxPoints = evaluation.maxPoints ?? rubric?.maxPoints ?? null;
    const indicatorCount = evaluation.totalIndicators ?? rubric?.indicatorCount ?? evaluation.criteria?.length ?? 0;
    const officialScore = evaluation.score != null ? Number(evaluation.score).toFixed(1) : '—';

    return (
        <Modal
            isOpen={isOpen}
            onClose={submitting ? () => {} : onClose}
            title="Confirmar Firma Digital y Publicación Oficial"
        >
            <div className="space-y-4">
                <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 space-y-1.5">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-xs font-bold uppercase tracking-wider text-blue-800">
                            Resumen de Pauta a Firmar
                        </span>
                        <Badge variant="info">
                            Versión {rubricVersion} • Rev. #{evaluation.revision ?? 1}
                        </Badge>
                    </div>
                    <p className="text-base font-bold text-blue-950">{instrumentName}</p>
                    {(area || visit) && (
                        <p className="text-xs text-blue-800 font-medium">
                            {area ? `Área: ${area}` : ''}{area && visit ? ' • ' : ''}{visit ? `Visita ${visit}` : ''}
                        </p>
                    )}
                    <p className="text-sm text-blue-900 pt-1">
                        <span className="font-semibold">Estudiante:</span>{' '}
                        {assignment?.student?.fullName || 'Estudiante'} ({assignment?.student?.email || ''})
                    </p>
                    <p className="text-xs text-blue-700">
                        <span className="font-semibold">Asignatura:</span>{' '}
                        {assignment?.subject?.name || 'Práctica'}
                    </p>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 text-center">
                        <p className="text-xs text-gray-500 font-medium">Indicadores Evaluados</p>
                        <p className="text-lg font-extrabold text-gray-900">
                            {evaluation.observedIndicators ?? indicatorCount} / {indicatorCount}
                        </p>
                    </div>
                    <div className="bg-gray-50 border border-gray-200 rounded-xl p-3 text-center">
                        <p className="text-xs text-gray-500 font-medium">Puntaje Obtenido</p>
                        <p className="text-lg font-extrabold text-gray-900">
                            {totalPoints != null && maxPoints != null ? `${totalPoints} / ${maxPoints}` : '—'}
                        </p>
                    </div>
                    <div className="bg-emerald-50 border border-emerald-300 rounded-xl p-3 text-center">
                        <p className="text-xs text-emerald-800 font-semibold">Nota Oficial Calculada</p>
                        <p className="text-2xl font-extrabold text-emerald-900" data-testid="dialog-official-score">
                            {officialScore}
                        </p>
                    </div>
                </div>

                {evaluation.qualitativeComments && (
                    <div className="bg-gray-50 border border-gray-200 rounded-xl p-3">
                        <p className="text-xs font-semibold text-gray-600 mb-1">
                            Observaciones y Retroalimentación:
                        </p>
                        <p className="text-sm text-gray-800 whitespace-pre-wrap">
                            {evaluation.qualitativeComments}
                        </p>
                    </div>
                )}

                <div className="bg-amber-50 border border-amber-300 rounded-xl p-4 space-y-2">
                    <p className="text-xs font-bold uppercase tracking-wider text-amber-900">
                        Identidad del Firmante Autenticado (Registrada por el Servidor)
                    </p>
                    <p className="text-sm text-amber-950">
                        <span className="font-semibold">{user?.fullName || 'Evaluador Autenticado'}</span>{' '}
                        {user?.email ? `<${user.email}>` : ''} —{' '}
                        <span className="italic">{userRole || 'Evaluador'}</span>
                    </p>
                    <p className="text-xs text-amber-800">
                        Al confirmar, el servidor registrará su cuenta autenticada, la fecha/hora UTC, la huella SHA-256 de la instantánea y publicará inmediatamente los resultados en el portafolio del estudiante. Esta operación es definitiva e inamovible.
                    </p>

                    <label className="flex items-start gap-3 pt-2 cursor-pointer select-none">
                        <input
                            type="checkbox"
                            className="mt-0.5 h-5 w-5 text-blue-600 rounded border-amber-400"
                            checked={confirmed}
                            disabled={submitting}
                            onChange={(e) => setConfirmed(e.target.checked)}
                        />
                        <span className="text-sm font-semibold text-gray-900">
                            Confirmo el cierre definitivo, firma digital con mi cuenta autenticada y publicación oficial de esta evaluación.
                        </span>
                    </label>
                </div>

                {conflictError && (
                    <div role="alert" className="bg-red-50 border border-red-300 rounded-xl p-3 text-xs text-red-800 font-medium">
                        {conflictError}
                    </div>
                )}

                <div className="flex justify-end gap-3 pt-3 border-t border-gray-200">
                    <Button
                        type="button"
                        variant="outline"
                        disabled={submitting}
                        onClick={onClose}
                    >
                        Volver al Borrador
                    </Button>
                    <Button
                        type="button"
                        variant="primary"
                        disabled={!confirmed || submitting}
                        onClick={() => onConfirm(evaluation)}
                    >
                        {submitting ? 'Firmando y Publicando...' : 'Confirmar Firma y Publicar'}
                    </Button>
                </div>
            </div>
        </Modal>
    );
}
