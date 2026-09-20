package com.ubb.practicas.controller;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.dto.UserResponse;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.PasswordResetToken;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.repository.PasswordResetTokenRepository;
import com.ubb.practicas.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User usuario = userRepository.findByEmail(request.getEmail());

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        if (!usuario.getActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cuenta desactivada");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }

        String roleName = usuario.getRole() != null ? usuario.getRole().getName() : "SinRol";
        String token = jwtUtil.generateToken(usuario.getEmail(), roleName);
        UserResponse userResponse = UserResponse.fromUser(usuario);

        return ResponseEntity.ok(new AuthResponse(token, userResponse));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            // Por seguridad, retornamos OK incluso si no existe, pero para ayudar al debug en este proyecto enviamos error:
            return ResponseEntity.badRequest().body(Map.of("message", "No existe una cuenta con ese correo institucional."));
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); 
        
        tokenRepository.save(resetToken);
        
        return ResponseEntity.ok(Map.of(
            "message", "Simulación: En un entorno real se enviaría un correo con este enlace.",
            "link", "http://localhost:5173/reset-password?token=" + token
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String tokenStr = request.get("token");
        String newPassword = request.get("newPassword");

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenStr);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El enlace de recuperación es inválido o no existe."));
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El enlace ha expirado. Solicita uno nuevo."));
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Opcional: Eliminar el token después de usarlo para que no se reuse
        tokenRepository.delete(token);

        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida exitosamente."));
    }
}
