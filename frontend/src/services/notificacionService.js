import apiClient from './apiClient';

export const notificacionService = {
  obtenerMisNotificaciones: (opciones = {}) =>
    apiClient.get('/api/notificaciones', opciones),

  marcarComoLeida: (notificacionId, opciones = {}) =>
    apiClient.put(`/api/notificaciones/${notificacionId}/leer`, {}, opciones),

  marcarTodasComoLeidas: (opciones = {}) =>
    apiClient.put('/api/notificaciones/leer-todas', {}, opciones),

  publicarComunicado: (datosComunicado, opciones = {}) =>
    apiClient.post('/api/notificaciones/comunicado', datosComunicado, opciones),

  obtenerDestinatariosDisponibles: (opciones = {}) =>
    apiClient.get('/api/notificaciones/destinatarios', opciones),
};

export default notificacionService;
