import apiClient from './apiClient';

export const evaluationService = {
  createOrUpdate: (payload, options = {}) =>
    apiClient.post('/api/evaluaciones', payload, options),

  getByAssignment: (assignmentId, options = {}) =>
    apiClient.get(`/api/evaluaciones/asignacion/${assignmentId}`, options),
};

export default evaluationService;
