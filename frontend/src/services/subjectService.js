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
      professor: subjectData.professorId ? { id: subjectData.professorId } : null,
    };
    return apiClient.post('/api/asignaturas', payload, options);
  },

  /**
   * Eliminar una asignatura por ID
   * @param {number|string} id
   */
  async deleteSubject(id, options) {
    return apiClient.delete(`/api/asignaturas/${id}`, options);
  },
};

export default subjectService;
