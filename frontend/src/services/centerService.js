import apiClient from './apiClient';

export const centerService = {
  /**
   * Listar todos los centros de práctica registrados
   */
  async getCenters(options) {
    return apiClient.get('/api/centros', options);
  },

  /**
   * Crear un nuevo centro de práctica
   * @param {Object} centerData
   */
  async createCenter(centerData, options) {
    return apiClient.post('/api/centros', centerData, options);
  },

  /**
   * Actualizar un centro de práctica existente
   * @param {number|string} id
   * @param {Object} centerData
   */
  async updateCenter(id, centerData, options) {
    return apiClient.put(`/api/centros/${id}`, centerData, options);
  },

  /**
   * Eliminar un centro de práctica por ID
   * @param {number|string} id
   */
  async deleteCenter(id, options) {
    return apiClient.delete(`/api/centros/${id}`, options);
  },
};

export default centerService;
