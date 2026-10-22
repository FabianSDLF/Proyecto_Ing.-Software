import apiClient from './apiClient';

/**
 * HU5: Servicio de ciclos de práctica (Inicial, Intermedio, Avanzado).
 */
export const cycleService = {
  /**
   * Listar los tres ciclos con sus asignaturas asociadas
   */
  async getCycles(options) {
    return apiClient.get('/api/ciclos', options);
  },

  /**
   * Obtener un ciclo por su código
   * @param {'INICIAL'|'INTERMEDIO'|'AVANZADO'} code
   */
  async getCycle(code, options) {
    return apiClient.get(`/api/ciclos/${code}`, options);
  },

  /**
   * Actualizar los datos descriptivos de un ciclo
   * @param {string} code
   * @param {{ name: string, description?: string, startSemester?: number|null, endSemester?: number|null }} data
   */
  async updateCycle(code, data, options) {
    const payload = {
      name: data.name,
      description: data.description || null,
      startSemester: data.startSemester === '' || data.startSemester == null ? null : Number(data.startSemester),
      endSemester: data.endSemester === '' || data.endSemester == null ? null : Number(data.endSemester),
    };
    return apiClient.put(`/api/ciclos/${code}`, payload, options);
  },
};

export default cycleService;
