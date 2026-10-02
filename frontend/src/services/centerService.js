import apiClient from './apiClient';

export const centerService = {
  /**
   * Listar todos los centros de práctica registrados
   */
  async getCenters(options) {
    return apiClient.get('/api/centros', options);
  },

  /**
   * Crear un nuevo centro de práctica (extensión futura)
   * @param {Object} centerData
   */
  async createCenter(centerData, options) {
    return apiClient.post('/api/centros', centerData, options);
  },
};

export default centerService;
