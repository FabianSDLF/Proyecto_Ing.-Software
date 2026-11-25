package com.ubb.practicas.controller;

import com.ubb.practicas.dto.BatchImportResultDto;
import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.exception.UnauthorizedException;
import com.ubb.practicas.model.User;
import com.ubb.practicas.service.UserBatchImportService;
import com.ubb.practicas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserBatchImportService userBatchImportService;

    @GetMapping
    public ResponseEntity<List<User>> obtenerTodosUsuarios() {
        return ResponseEntity.ok(userService.obtenerTodosUsuarios());
    }

    @GetMapping("/rol/{roleName}")
    public ResponseEntity<List<User>> listarUsuariosPorRol(@PathVariable String roleName) {
        return ResponseEntity.ok(userService.listarUsuariosPorRol(roleName));
    }

    @PostMapping
    public ResponseEntity<User> crearUsuario(@RequestBody User nuevoUsuario) {
        User created = userService.crearUsuario(nuevoUsuario);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/carga-masiva")
    public ResponseEntity<Map<String, Object>> cargaMasivaUsuarios(@RequestParam("archivo") MultipartFile archivo) {
        BatchImportResultDto result = userBatchImportService.importUsers(archivo);
        if (result.getSuccessCount() == 0) {
            String detail = result.getErrors().isEmpty()
                    ? "No se importó ningún usuario válido."
                    : String.join(" | ", result.getErrors());
            throw new FileProcessingException("No se pudo importar ningún usuario: " + detail);
        }

        int createdCount = result.getCreatedUserEmails() != null ? result.getCreatedUserEmails().size() : 0;
        int updatedCount = result.getUpdatedUserEmails() != null ? result.getUpdatedUserEmails().size() : 0;
        String message = result.getFailureCount() > 0
                ? String.format("Importación parcial: %d creados, %d actualizados y %d rechazados.",
                        createdCount, updatedCount, result.getFailureCount())
                : String.format("Importación completada: %d creados y %d actualizados.",
                        createdCount, updatedCount);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("totalRecords", result.getTotalRecords());
        body.put("successCount", result.getSuccessCount());
        body.put("createdCount", createdCount);
        body.put("updatedCount", updatedCount);
        body.put("failureCount", result.getFailureCount());
        body.put("errors", result.getErrors());
        body.put("createdUserEmails", result.getCreatedUserEmails());
        body.put("updatedUserEmails", result.getUpdatedUserEmails());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<User> actualizarRol(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String roleName = payload.get("roleName");
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new BadRequestException("El nombre del rol es requerido");
        }
        User updatedUser = userService.actualizarRolUsuario(id, roleName.trim());
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> actualizarUsuario(@PathVariable Long id, @RequestBody User datosActualizados) {
        User updatedUser = userService.actualizarUsuario(id, datosActualizados);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<User> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        Boolean estado = payload.get("active");
        if (estado == null) {
            throw new BadRequestException("El campo 'active' es requerido");
        }
        User updatedUser = userService.cambiarEstadoUsuario(id, estado);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/perfil")
    public ResponseEntity<User> obtenerMiPerfil(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }
        User user = userService.obtenerPorEmail(principal.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/perfil")
    public ResponseEntity<User> actualizarMiPerfil(
            Principal principal,
            @RequestBody UserProfileUpdateRequest request) {

        if (principal == null) {
            throw new UnauthorizedException("Sesión no autorizada");
        }

        User updatedUser = userService.actualizarMiPerfil(principal.getName(), request);
        return ResponseEntity.ok(updatedUser);
    }
}
