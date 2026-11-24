import { Badge } from './ui';
import RubricIndicators from './RubricIndicators';

export default function EvaluationForm({ existingEvaluation, activeRubric, isLocked, rubricProgress, handleSetAllIndicators, displayedIndicators, indicatorPoints, handleSetIndicatorPoint, justifications, onJustificationChange }) {
  return (<section className="space-y-3">
                                <div className="flex flex-wrap items-center justify-between gap-2 bg-gray-50 border border-gray-200 rounded-xl p-3">
                                    <div>
                                        <h3 className="text-sm font-bold text-gray-900">
                                            {existingEvaluation?.instrumentName || activeRubric?.name || 'Desglose por Indicadores'}
                                        </h3>
                                        <p className="text-xs text-gray-600">
                                            Escala oficial por indicador: 2 a 7 puntos (enteros). Exigencia del 60% para nota 4.0.
                                        </p>
                                    </div>
                                    {!isLocked && rubricProgress && (
                                        <div className="flex flex-wrap items-center gap-2 text-xs">
                                            <Badge variant={rubricProgress.isComplete ? 'success' : 'warning'}>
                                                Observados: {rubricProgress.observedCount} / {rubricProgress.totalCount}
                                            </Badge>
                                            <Badge variant="info">
                                                Puntaje: {rubricProgress.sumPoints} / {rubricProgress.maxPoints}
                                            </Badge>
                                            {rubricProgress.preliminaryScore != null ? (
                                                <Badge variant="success">
                                                    Nota preliminar: {rubricProgress.preliminaryScore.toFixed(1)}
                                                </Badge>
                                            ) : (
                                                <span className="text-amber-700 font-medium">
                                                    Complete todos los indicadores para calcular nota
                                                </span>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {!isLocked && activeRubric && (
                                    <div className="flex flex-wrap items-center gap-2 text-xs bg-blue-50/60 border border-blue-100 rounded-lg p-2.5">
                                        <span className="font-semibold text-blue-900">Asignación rápida a todos los indicadores:</span>
                                        {[2, 3, 4, 5, 6, 7].map((pts) => (
                                            <button
                                                key={pts}
                                                type="button"
                                                onClick={() => handleSetAllIndicators(pts)}
                                                className="px-2.5 py-1 rounded border border-blue-300 bg-white hover:bg-blue-100 text-blue-900 font-semibold transition-colors cursor-pointer"
                                            >
                                                Todos {pts}
                                            </button>
                                        ))}
                                    </div>
                                )}

                                <RubricIndicators justifications={justifications} onJustificationChange={onJustificationChange} indicators={displayedIndicators} points={indicatorPoints} readOnly={isLocked} onChange={handleSetIndicatorPoint} />
                            </section>);
}
