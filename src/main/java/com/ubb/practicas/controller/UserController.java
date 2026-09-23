package com.ubb.practicas.controller;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.model.User;
import com.ubb.practicas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> obtenerTodosUsuarios() {
        return userService.obtenerTodosUsuarios();
    }

    @GetMapping("/rol/{roleName}")
    public List<User> listarUsuariosPorRol(@PathVariable String roleName) {
        return userService.listarUsuariosPorRol(roleName);
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody User nuevoUsuario) {
        try {
            User created = userService.crearUsuario(nuevoUsuario);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/carga-masiva")
    public ResponseEntity<?> cargaMasivaUsuarios(@RequestParam("archivo") MultipartFile archivo) {
        try {
            userService.procesarArchivoMasivo(archivo);
            return ResponseEntity.ok("Usuarios cargados exitosamente de forma masiva.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar el archivo: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<?> actualizarRol(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String roleName = payload.get("roleName");
            if (roleName == null || roleName.isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del rol es requerido");
            }
            User updatedUser = userService.actualizarRolUsuario(id, roleName);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody User datosActualizados) {
        try {
            User updatedUser = userService.actualizarUsuario(id, datosActualizados);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean estado = payload.get("active");
            if (estado == null) {
                return ResponseEntity.badRequest().body("El campo 'active' es requerido");
            }
            User updatedUser = userService.cambiarEstadoUsuario(id, estado);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerMiPerfil(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }
        User user = userService.obtenerPorEmail(principal.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarMiPerfil(
            Principal principal,
            @RequestBody UserProfileUpdateRequest request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }

        try {
            User updatedUser = userService.actualizarMiPerfil(principal.getName(), request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
