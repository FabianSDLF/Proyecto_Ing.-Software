import apiClient from './apiClient';

export const userService = {
  /**
   * Listar todos los usuarios del sistema
   */
  async getUsers(options) {
    return apiClient.get('/api/usuarios', options);
  },

  /**
   * Listar usuarios filtrados por rol
   * @param {string} roleName
   */
  async getUsersByRole(roleName, options) {
    return apiClient.get(`/api/usuarios/rol/${encodeURIComponent(roleName)}`, options);
  },

  /**
   * Listar todos los roles del sistema
   */
  async getRoles(options) {
    return apiClient.get('/api/roles', options);
  },

  /**
   * Crear un nuevo usuario individualmente
   * @param {{ fullName: string, email: string, passwordHash?: string, password?: string, role: { name: string }, active?: boolean }} userData
   */
  async createUser(userData, options) {
    const payload = {
      fullName: userData.fullName,
      email: userData.email,
      passwordHash: userData.password || userData.passwordHash,
      role: typeof userData.role === 'string' ? { name: userData.role } : userData.role,
      active: userData.active !== false,
    };
    return apiClient.post('/api/usuarios', payload, options);
  },

  /**
   * Actualizar datos de un usuario existente
   * @param {number|string} id
   * @param {{ fullName: string, email: string, role: { name: string } }} userData
   */
  async updateUser(id, userData, options) {
    const payload = {
      fullName: userData.fullName,
      email: userData.email,
      role: typeof userData.role === 'string' ? { name: userData.role } : userData.role,
    };
    return apiClient.put(`/api/usuarios/${id}`, payload, options);
  },

  /**
   * Actualizar rol de un usuario
   * @param {number|string} id
   * @param {string} roleName
   */
  async updateRole(id, roleName, options) {
    return apiClient.put(`/api/usuarios/${id}/rol`, { roleName }, options);
  },

  /**
   * Cambiar estado activo/inactivo de un usuario
   * @param {number|string} id
   * @param {boolean} active
   */
  async updateStatus(id, active, options) {
    return apiClient.patch(`/api/usuarios/${id}/estado`, { active }, options);
  },

  /**
   * Subir archivo Excel o CSV para importación masiva de usuarios
   * @param {File} file
   */
  async uploadBatch(file, options) {
    const formData = new FormData();
    formData.append('archivo', file);
    return apiClient.upload('/api/usuarios/carga-masiva', formData, options);
  },
};

export default userService;
