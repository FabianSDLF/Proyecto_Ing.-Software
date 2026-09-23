import apiClient from './apiClient';

export const authService = {
  /**
   * Autenticar usuario con credenciales
   * @param {{ email: string, password: string }} credentials
   */
  async login(credentials) {
    return apiClient.post('/api/auth/login', credentials);
  },

  /**
   * Solicitar recuperación de contraseña institucional
   * @param {string} email
   */
  async forgotPassword(email) {
    return apiClient.post('/api/auth/forgot-password', { email });
  },

  /**
   * Confirmar nueva contraseña mediante token UUID
   * @param {{ token: string, newPassword: string }} payload
   */
  async resetPassword({ token, newPassword }) {
    return apiClient.post('/api/auth/reset-password', { token, newPassword });
  },

  /**
   * Obtener perfil del usuario autenticado
   */
  async getProfile(options) {
    return apiClient.get('/api/usuarios/perfil', options);
  },

  /**
   * Actualizar datos del perfil propio
   * @param {{ fullName: string, email: string, phone?: string, password?: string }} profileData
   */
  async updateProfile(profileData, options) {
    return apiClient.put('/api/usuarios/perfil', profileData, options);
  },
};

export default authService;
