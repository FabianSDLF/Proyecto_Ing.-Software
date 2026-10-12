import apiClient from './apiClient';

export const assignmentService = {
  /**
   * Listar todas las asignaciones del sistema
   */
  async getAssignments(options) {
    return apiClient.get('/api/asignaciones', options);
  },

  /**
   * Crear una nueva asignación de práctica
   * @param {{ studentId: number|string, subjectId: number|string, centerId?: number|string, collaboratorId?: number|string, tutorId?: number|string, status?: string }} data
   */
  async createAssignment(data, options) {
    const payload = {
      student: data.studentId ? { id: data.studentId } : null,
      subject: data.subjectId ? { id: data.subjectId } : null,
      center: data.centerId ? { id: data.centerId } : null,
      collaborator: data.collaboratorId ? { id: data.collaboratorId } : null,
      tutor: data.tutorId ? { id: data.tutorId } : null,
      status: data.status || 'ACTIVA',
    };
    return apiClient.post('/api/asignaciones', payload, options);
  },

  /**
   * Eliminar una asignación por ID
   * @param {number|string} id
   */
  async deleteAssignment(id, options) {
    return apiClient.delete(`/api/asignaciones/${id}`, options);
  },

  /**
   * Listar asignaciones por profesor colaborador
   * @param {number|string} collaboratorId
   */
  async getByCollaborator(collaboratorId, options) {
    return apiClient.get(`/api/asignaciones/colaborador/${collaboratorId}`, options);
  },

  /**
   * Listar asignaciones por tutor UBB
   * @param {number|string} tutorId
   */
  async getByTutor(tutorId, options) {
    return apiClient.get(`/api/asignaciones/tutor/${tutorId}`, options);
  },

  /**
   * Listar estudiantes asignados a un docente según rol docente
   * @param {number|string} professorId
   */
  async getByProfessor(professorId, options) {
    return apiClient.get(`/api/profesor/${professorId}/estudiantes`, options);
  },

  /**
   * HU9: Listar duplas de estudiantes en ciclo intermedio por periodo
   * @param {number|string|null} [periodId]
   */
  async getDuplas(periodId, options = {}) {
    const params = { ...options?.params };
    if (periodId !== undefined && periodId !== null && typeof periodId !== 'object') {
      params.periodId = periodId;
    }
    return apiClient.get('/api/duplas', { ...options, params });
  },

  /**
   * HU9: Crear una nueva dupla de estudiantes definitiva
   * @param {{ assignment1Id: number|string, assignment2Id: number|string, periodId?: number|string }} payload
   */
  async crearDupla(payload, options = {}) {
    return apiClient.post('/api/duplas', payload, options);
  },

  /**
   * HU9: Listar estudiantes elegibles en ciclo intermedio sin pareja para un periodo
   * @param {number|string|null} [periodId]
   */
  async getEstudiantesDisponiblesDupla(periodId, options = {}) {
    const params = { ...options?.params };
    if (periodId !== undefined && periodId !== null && typeof periodId !== 'object') {
      params.periodId = periodId;
    }
    return apiClient.get('/api/duplas/disponibles', { ...options, params });
  },
};

export default assignmentService;
