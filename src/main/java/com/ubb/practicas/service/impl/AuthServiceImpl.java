package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.dto.UserResponse;
import com.ubb.practicas.model.PasswordResetToken;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.PasswordResetTokenRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.security.JwtUtil;
import com.ubb.practicas.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository tokenRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User usuario = userRepository.findByEmail(request.getEmail());

        if (usuario == null) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new DisabledException("Cuenta desactivada");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        String roleName = usuario.getRole() != null ? usuario.getRole().getName() : "SinRol";
        String token = jwtUtil.generateToken(usuario.getEmail(), roleName);
        UserResponse userResponse = UserResponse.fromUser(usuario);

        return new AuthResponse(token, userResponse);
    }

    @Override
    @Transactional
    public Map<String, String> forgotPassword(String email) {
        if (email != null && !email.trim().isEmpty()) {
            User user = userRepository.findByEmail(email.trim());
            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(token);
                resetToken.setUser(user);
                resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
                tokenRepository.save(resetToken);

                // TR-02: Registrar en logs del servidor en desarrollo; NUNCA exponer el enlace en la respuesta HTTP
                log.info("Solicitud de recuperación para {}. Token de desarrollo: http://localhost:5173/reset-password?token={}", email, token);
            }
        }

        // BK-12 & TR-02: Retorno uniforme para prevenir enumeración de cuentas y account takeover
        return Map.of(
                "message", "Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación."
        );
    }

    @Override
    @Transactional
    public Map<String, String> resetPassword(String tokenStr, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenStr);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("El enlace de recuperación es inválido o no existe.");
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El enlace ha expirado. Solicita uno nuevo.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(token);

        return Map.of("message", "Contraseña restablecida exitosamente.");
    }
}
