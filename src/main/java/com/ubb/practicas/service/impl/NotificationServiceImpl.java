package com.ubb.practicas.service.impl;

import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.model.Notification;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.NotificationType;
import com.ubb.practicas.repository.NotificationRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> obtenerMisNotificaciones(String correoUsuario) {
        return notificationRepository.findByTargetUser_EmailOrderByCreatedAtDesc(correoUsuario);
    }

    @Override
    @Transactional
    public Notification crearNotificacion(User usuarioDestino, NotificationType tipoNotificacion, String mensajeTexto, Long referenciaId, String tipoReferencia) {
        Notification nuevaNotificacion = new Notification();
        nuevaNotificacion.setTargetUser(usuarioDestino);
        nuevaNotificacion.setType(tipoNotificacion);
        nuevaNotificacion.setMessage(mensajeTexto);
        nuevaNotificacion.setRead(false);
        nuevaNotificacion.setCreatedAt(LocalDateTime.now());
        nuevaNotificacion.setReferenceId(referenciaId);
        nuevaNotificacion.setReferenceType(tipoReferencia);
        return notificationRepository.save(nuevaNotificacion);
    }

    @Override
    @Transactional
    public Notification marcarComoLeida(Long notificacionId, String correoUsuario) {
        Notification notificacion = notificationRepository.findById(notificacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (notificacion.getTargetUser() == null
                || notificacion.getTargetUser().getEmail() == null
                || !notificacion.getTargetUser().getEmail().equalsIgnoreCase(correoUsuario)) {
            throw new AccessDeniedException("No tiene permisos para modificar esta notificación");
        }

        notificacion.setRead(true);
        return notificationRepository.save(notificacion);
    }

    @Override
    @Transactional
    public void marcarTodasComoLeidas(String correoUsuario) {
        List<Notification> listaNotificaciones = notificationRepository.findByTargetUser_EmailOrderByCreatedAtDesc(correoUsuario);
        List<Notification> listaPorActualizar = new ArrayList<>();
        for (Notification notificacionActual : listaNotificaciones) {
            if (!Boolean.TRUE.equals(notificacionActual.getRead())) {
                notificacionActual.setRead(true);
                listaPorActualizar.add(notificacionActual);
            }
        }
        if (!listaPorActualizar.isEmpty()) {
            notificationRepository.saveAll(listaPorActualizar);
        }
    }

    @Override
    @Transactional
    public List<Notification> publicarComunicadoSistema(String mensajeComunicado, String correoDestinatario, String correoRemitente) {
        if (mensajeComunicado == null || mensajeComunicado.trim().isEmpty()) {
            throw new BadRequestException("El mensaje del comunicado no puede estar vacío.");
        }

        String textoLimpio = mensajeComunicado.trim();
        List<Notification> notificacionesCreadas = new ArrayList<>();

        if (correoDestinatario != null
                && !correoDestinatario.isBlank()
                && !"TODOS".equalsIgnoreCase(correoDestinatario.trim())) {
            User usuarioDestino = userRepository.findByEmail(correoDestinatario.trim());
            if (usuarioDestino == null) {
                throw new ResourceNotFoundException("Usuario destinatario no encontrado: " + correoDestinatario.trim());
            }
            notificacionesCreadas.add(crearNotificacion(
                    usuarioDestino,
                    NotificationType.ALERTA_SISTEMA,
                    textoLimpio,
                    null,
                    "COMUNICADO_SISTEMA"
            ));
            return notificacionesCreadas;
        }

        List<User> listaUsuarios = userRepository.findAll();
        for (User usuarioActual : listaUsuarios) {
            if (Boolean.TRUE.equals(usuarioActual.getActive())) {
                notificacionesCreadas.add(crearNotificacion(
                        usuarioActual,
                        NotificationType.ALERTA_SISTEMA,
                        textoLimpio,
                        null,
                        "COMUNICADO_SISTEMA"
                ));
            }
        }
        return notificacionesCreadas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> obtenerDestinatariosActivos() {
        List<User> todosLosUsuarios = userRepository.findAll();
        List<User> usuariosActivos = new ArrayList<>();
        for (User usuarioActual : todosLosUsuarios) {
            if (Boolean.TRUE.equals(usuarioActual.getActive())) {
                usuariosActivos.add(usuarioActual);
            }
        }
        return usuariosActivos;
    }
}
