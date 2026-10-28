import apiClient from './apiClient';

export const subjectService = {
  /**
   * Listar todas las asignaturas registradas
   */
  async getSubjects(options) {
    return apiClient.get('/api/asignaturas', options);
  },

  /**
   * Crear una nueva asignatura de práctica
   * @param {{ name: string, cycle: string, code?: string, professorId?: number|string }} subjectData
   */
  async createSubject(subjectData, options) {
    const payload = {
      name: subjectData.name,
      cycle: subjectData.cycle,
      code: subjectData.code || null,
      professor: subjectData.professorId ? { id: Number(subjectData.professorId) } : null,
    };
    return apiClient.post('/api/asignaturas', payload, options);
  },

  /**
   * Actualizar una asignatura existente
   * @param {number|string} id
   * @param {{ name: string, cycle: string, code?: string, professorId?: number|string }} subjectData
   */
  async updateSubject(id, subjectData, options) {
    const payload = {
      name: subjectData.name,
      cycle: subjectData.cycle,
      code: subjectData.code || null,
      professor: subjectData.professorId ? { id: Number(subjectData.professorId) } : null,
    };
    return apiClient.put(`/api/asignaturas/${id}`, payload, options);
  },

  /**
   * Eliminar una asignatura por ID
   * @param {number|string} id
   */
  async deleteSubject(id, options) {
    return apiClient.delete(`/api/asignaturas/${id}`, options);
  },

  /**
   * Listar todos los periodos académicos
   */
  async getPeriods(options) {
    return apiClient.get('/api/periodos', options);
  },

  /**
   * Crear un nuevo periodo académico
   */
  async createPeriod(periodData, options) {
    return apiClient.post('/api/periodos', periodData, options);
  },

  /**
   * Actualizar un periodo académico existente
   */
  async updatePeriod(id, periodData, options) {
    return apiClient.put(`/api/periodos/${id}`, periodData, options);
  },

  /**
   * Cambiar estado de un periodo académico (ACTIVO, CERRADO, ARCHIVADO)
   */
  async changePeriodStatus(id, status, options) {
    return apiClient.patch(`/api/periodos/${id}/estado`, { status }, options);
  },

  /**
   * Eliminar un periodo académico
   */
  async deletePeriod(id, options) {
    return apiClient.delete(`/api/periodos/${id}`, options);
  },
};

export default subjectService;
