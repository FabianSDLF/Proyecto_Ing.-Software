package com.ubb.practicas.controller;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.dto.UserResponse;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
}
