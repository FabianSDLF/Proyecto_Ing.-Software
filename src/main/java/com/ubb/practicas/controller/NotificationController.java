package com.ubb.practicas.controller;

import com.ubb.practicas.dto.ComunicadoRequest;
import com.ubb.practicas.exception.UnauthorizedException;
import com.ubb.practicas.model.Notification;
import com.ubb.practicas.model.User;
import com.ubb.practicas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> obtenerMisNotificaciones(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }
        return ResponseEntity.ok(notificationService.obtenerMisNotificaciones(principal.getName()));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Notification> marcarNotificacionLeida(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }
        return ResponseEntity.ok(notificationService.marcarComoLeida(id, principal.getName()));
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<Map<String, String>> marcarTodasLeidas(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }
        notificationService.marcarTodasComoLeidas(principal.getName());
        return ResponseEntity.ok(Map.of("mensaje", "Todas las notificaciones fueron marcadas como leídas"));
    }

    @PostMapping("/comunicado")
    public ResponseEntity<List<Notification>> publicarComunicado(@RequestBody ComunicadoRequest solicitud, Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }
        List<Notification> comunicadosEnviados = notificationService.publicarComunicadoSistema(
                solicitud != null ? solicitud.getMensaje() : null,
                solicitud != null ? solicitud.getCorreoDestinatario() : null,
                principal.getName()
        );
        return ResponseEntity.ok(comunicadosEnviados);
    }

    @GetMapping("/destinatarios")
    public ResponseEntity<List<User>> obtenerDestinatariosActivos(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }
        return ResponseEntity.ok(notificationService.obtenerDestinatariosActivos());
    }
}
