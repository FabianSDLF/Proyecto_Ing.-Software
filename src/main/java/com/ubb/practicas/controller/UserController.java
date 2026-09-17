package com.ubb.practicas.controller;

import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/usuarios")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> obtenerTodosUsuarios() {
        return userRepository.findAll();
    }

    @GetMapping("/rol/{roleName}")
    public List<User> listarUsuariosPorRol(@PathVariable String roleName) {
        return userRepository.findByRoleName(roleName);
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

    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody LoginRequest request) {
        // 1. Buscamos en la base de datos si existe alguien con ese correo
        User usuario = userRepository.findByEmail(request.getEmail());
        // 2. Si el usuario existe, comparamos la contraseña
        if (usuario != null && usuario.getPasswordHash().equals(request.getPassword())) {

            // ¡Éxito! Devolvemos los datos del usuario con un código HTTP 200 (OK)
            return ResponseEntity.ok(usuario);

        } else {

            // ¡Fallo! Devolvemos un código HTTP 401 (No Autorizado)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");

        }
    }

    @PostMapping("/carga-masiva")
    public ResponseEntity<?> cargaMasivaUsuarios(@RequestParam("archivo") MultipartFile archivo) {
        try {
            // Le pasamos la pelota al cerebro (el Service)
            userService.procesarArchivoMasivo(archivo);

            return ResponseEntity.ok("Usuarios cargados exitosamente de forma masiva.");
        } catch (Exception e) {
            // Si el archivo está corrupto o le falta una columna, atrapamos el error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar el archivo: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<?> actualizarRol(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
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
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> payload) {
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
}
