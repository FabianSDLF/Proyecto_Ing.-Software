import apiClient from './apiClient';

/**
 * Fórmula oficial de previsualización en cliente (escala 2,0–7,0 al 60% de exigencia).
 * La nota definitiva oficial siempre es calculada y validada por el servidor.
 */
export function calculatePreliminaryGrade(totalPoints, maxPoints) {
  if (!maxPoints || maxPoints <= 0 || totalPoints == null || totalPoints < 0) {
    return null;
  }
  const p = Number(totalPoints);
  const m = Number(maxPoints);
  const t = m * 0.6;
  const unrounded = p <= t ? 2 + (2 * p) / t : 4 + (3 * (p - t)) / (m - t);
  const rounded = Math.round((unrounded + Number.EPSILON) * 10) / 10;
  return Math.min(7.0, Math.max(2.0, rounded));
}

export const evaluationService = {
  getRubrics: (assignmentId, options = {}) =>
    apiClient.get('/api/evaluaciones/pautas', {
      ...options,
      params: {
        ...(options.params || {}),
        ...(assignmentId != null ? { assignmentId } : {}),
      },
    }),

  getCollaboratorRubrics: (options = {}) =>
    apiClient.get('/api/evaluaciones/pautas/colaborador', options),

  getTutorRubrics: (options = {}) =>
    apiClient.get('/api/evaluaciones/pautas/tutor', options),

  createOrUpdate: (payload, options = {}) =>
    apiClient.post('/api/evaluaciones', payload, options),

  finalize: (evaluationId, payload, options = {}) =>
    apiClient.post(`/api/evaluaciones/${evaluationId}/finalizar`, payload, options),

  getById: (evaluationId, options = {}) =>
    apiClient.get(`/api/evaluaciones/${evaluationId}`, options),

  getByAssignment: (assignmentId, options = {}) =>
    apiClient.get(`/api/evaluaciones/asignacion/${assignmentId}`, options),

  calculatePreliminaryGrade,
};

export default evaluationService;
