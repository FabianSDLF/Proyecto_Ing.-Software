package com.ubb.practicas.service;

import com.ubb.practicas.model.Notification;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    List<Notification> obtenerMisNotificaciones(String correoUsuario);
    Notification crearNotificacion(User usuarioDestino, NotificationType tipoNotificacion, String mensajeTexto, Long referenciaId, String tipoReferencia);
    Notification marcarComoLeida(Long notificacionId, String correoUsuario);
    void marcarTodasComoLeidas(String correoUsuario);
    List<Notification> publicarComunicadoSistema(String mensajeComunicado, String correoDestinatario, String correoRemitente);
    List<User> obtenerDestinatariosActivos();
}
