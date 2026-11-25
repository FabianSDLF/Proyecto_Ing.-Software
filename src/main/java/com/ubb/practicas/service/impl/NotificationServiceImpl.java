package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.NotificationRecipientDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.model.Notification;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.NotificationType;
import com.ubb.practicas.repository.NotificationRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.AssignmentScope;
import com.ubb.practicas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Set<String> ANNOUNCEMENT_ROLES = Set.of(
            "ADMINISTRADOR", "ADMIN", "COORDINADOR_DE_PRACTICAS", "PROFESOR_DE_ASIGNATURA"
    );

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;

    private User requireAuthorizedSender(String correoRemitente) {
        if (correoRemitente == null || correoRemitente.isBlank()) {
            throw new AccessDeniedException("No tiene permisos para gestionar comunicados");
        }
        User remitente = userRepository.findByEmail(correoRemitente.trim());
        if (remitente == null || !Boolean.TRUE.equals(remitente.getActive())) {
            throw new AccessDeniedException("Cuenta no autorizada para gestionar comunicados");
        }
        String role = AssignmentScope.role(remitente);
        if (!ANNOUNCEMENT_ROLES.contains(role)) {
            throw new AccessDeniedException("Solo coordinación y profesores de asignatura pueden gestionar comunicados");
        }
        return remitente;
    }

    private List<User> resolveAllowedActiveRecipients(User remitente) {
        if (AssignmentScope.isAdmin(remitente) || "ADMIN".equals(AssignmentScope.role(remitente))) {
            List<User> activos = new ArrayList<>();
            for (User u : userRepository.findAll()) {
                if (Boolean.TRUE.equals(u.getActive())) {
                    activos.add(u);
                }
            }
            return activos;
        }

        // Profesor de Asignatura: usuarios activos de sus prácticas asignadas + coordinación + sí mismo
        Map<String, User> scoped = new LinkedHashMap<>();
        addIfActive(scoped, remitente);
        if (remitente.getId() != null) {
            List<PracticeAssignment> asignaciones = practiceAssignmentRepository.findBySubjectProfessorId(remitente.getId());
            if (asignaciones != null) {
                for (PracticeAssignment a : asignaciones) {
                    addIfActive(scoped, a.getStudent());
                    addIfActive(scoped, a.getTutor());
                    addIfActive(scoped, a.getCollaborator());
                }
            }
        }
        for (User u : userRepository.findAll()) {
            if (Boolean.TRUE.equals(u.getActive()) && (AssignmentScope.isAdmin(u) || "ADMIN".equals(AssignmentScope.role(u)))) {
                addIfActive(scoped, u);
            }
        }
        return new ArrayList<>(scoped.values());
    }

    private void addIfActive(Map<String, User> target, User user) {
        if (user != null && Boolean.TRUE.equals(user.getActive()) && user.getEmail() != null) {
            target.putIfAbsent(user.getEmail().toLowerCase(java.util.Locale.ROOT), user);
        }
    }

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
        User remitente = requireAuthorizedSender(correoRemitente);

        if (mensajeComunicado == null || mensajeComunicado.trim().isEmpty()) {
            throw new BadRequestException("El mensaje del comunicado no puede estar vacío.");
        }

        String textoLimpio = mensajeComunicado.trim();
        List<User> destinatariosPermitidos = resolveAllowedActiveRecipients(remitente);
        List<Notification> notificacionesCreadas = new ArrayList<>();

        if (correoDestinatario != null
                && !correoDestinatario.isBlank()
                && !"TODOS".equalsIgnoreCase(correoDestinatario.trim())) {
            String correoLimpio = correoDestinatario.trim();
            User usuarioDestino = userRepository.findByEmail(correoLimpio);
            if (usuarioDestino == null) {
                throw new ResourceNotFoundException("Usuario destinatario no encontrado: " + correoLimpio);
            }
            boolean permitido = destinatariosPermitidos.stream()
                    .anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(correoLimpio));
            if (!permitido) {
                throw new AccessDeniedException("No tiene permisos para enviar comunicados a este destinatario");
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

        for (User usuarioActual : destinatariosPermitidos) {
            notificacionesCreadas.add(crearNotificacion(
                    usuarioActual,
                    NotificationType.ALERTA_SISTEMA,
                    textoLimpio,
                    null,
                    "COMUNICADO_SISTEMA"
            ));
        }
        return notificacionesCreadas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRecipientDTO> obtenerDestinatariosActivos(String correoSolicitante) {
        User solicitante = requireAuthorizedSender(correoSolicitante);
        return resolveAllowedActiveRecipients(solicitante).stream()
                .map(NotificationRecipientDTO::from)
                .toList();
    }
}
