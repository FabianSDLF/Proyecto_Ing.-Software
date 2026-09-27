package com.ubb.practicas.service;

import com.ubb.practicas.dto.AuthResponse;
import com.ubb.practicas.dto.LoginRequest;
import com.ubb.practicas.model.PasswordResetToken;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.PasswordResetTokenRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.security.JwtUtil;
import com.ubb.practicas.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    void login_Success() {
        Role role = new Role(1L, "Estudiante");
        User user = new User();
        user.setEmail("estudiante@ubb.cl");
        user.setPasswordHash("hashed_pass");
        user.setActive(true);
        user.setRole(role);

        when(userRepository.findByEmail("estudiante@ubb.cl")).thenReturn(user);
        when(passwordEncoder.matches("raw_pass", "hashed_pass")).thenReturn(true);
        when(jwtUtil.generateToken("estudiante@ubb.cl", "Estudiante")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(createLoginRequest("estudiante@ubb.cl", "raw_pass"));

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("estudiante@ubb.cl", response.getUser().getEmail());
        verify(jwtUtil).generateToken("estudiante@ubb.cl", "Estudiante");
    }

    @Test
    void login_UserNotFound_ThrowsBadCredentialsException() {
        when(userRepository.findByEmail("unknown@ubb.cl")).thenReturn(null);

        assertThrows(BadCredentialsException.class, () ->
                authService.login(createLoginRequest("unknown@ubb.cl", "pass")));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_DisabledAccount_ThrowsDisabledException() {
        User user = new User();
        user.setEmail("disabled@ubb.cl");
        user.setActive(false);

        when(userRepository.findByEmail("disabled@ubb.cl")).thenReturn(user);

        assertThrows(DisabledException.class, () ->
                authService.login(createLoginRequest("disabled@ubb.cl", "pass")));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_InvalidPassword_ThrowsBadCredentialsException() {
        User user = new User();
        user.setEmail("test@ubb.cl");
        user.setPasswordHash("hashed_pass");
        user.setActive(true);

        when(userRepository.findByEmail("test@ubb.cl")).thenReturn(user);
        when(passwordEncoder.matches("wrong_pass", "hashed_pass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                authService.login(createLoginRequest("test@ubb.cl", "wrong_pass")));

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_UserWithoutRole_FallsBackToSinRol() {
        User user = new User();
        user.setEmail("norole@ubb.cl");
        user.setPasswordHash("hashed_pass");
        user.setActive(true);
        user.setRole(null);

        when(userRepository.findByEmail("norole@ubb.cl")).thenReturn(user);
        when(passwordEncoder.matches("pass", "hashed_pass")).thenReturn(true);
        when(jwtUtil.generateToken("norole@ubb.cl", "SinRol")).thenReturn("mock.token.sinrol");

        AuthResponse response = authService.login(createLoginRequest("norole@ubb.cl", "pass"));

        assertNotNull(response);
        verify(jwtUtil).generateToken("norole@ubb.cl", "SinRol");
    }

    @Test
    void forgotPassword_ActiveUser_GeneratesAndSavesToken() {
        User user = new User();
        user.setEmail("test@ubb.cl");
        user.setActive(true);

        when(userRepository.findByEmail("test@ubb.cl")).thenReturn(user);

        Map<String, String> response = authService.forgotPassword("test@ubb.cl");

        assertNotNull(response);
        assertTrue(response.containsKey("message"));
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getToken());
        assertTrue(savedToken.getExpiryDate().isAfter(LocalDateTime.now()));
    }

    @Test
    void forgotPassword_NonExistentUser_ReturnsUniformMessage() {
        when(userRepository.findByEmail("none@ubb.cl")).thenReturn(null);

        Map<String, String> response = authService.forgotPassword("none@ubb.cl");

        assertNotNull(response);
        assertEquals("Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.",
                response.get("message"));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void forgotPassword_DisabledUser_ReturnsUniformMessage() {
        User user = new User();
        user.setEmail("disabled@ubb.cl");
        user.setActive(false);

        when(userRepository.findByEmail("disabled@ubb.cl")).thenReturn(user);

        Map<String, String> response = authService.forgotPassword("disabled@ubb.cl");

        assertNotNull(response);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void forgotPassword_NullOrBlankEmail_ReturnsUniformMessage() {
        Map<String, String> resNull = authService.forgotPassword(null);
        Map<String, String> resBlank = authService.forgotPassword("   ");

        assertNotNull(resNull);
        assertNotNull(resBlank);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_Success_EncodesPasswordAndDeletesToken() {
        User user = new User();
        user.setEmail("test@ubb.cl");
        user.setPasswordHash("old_hash");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new_secret_pass")).thenReturn("encoded_new_pass");

        Map<String, String> response = authService.resetPassword("valid-token", "new_secret_pass");

        assertEquals("Contraseña restablecida exitosamente.", response.get("message"));
        assertEquals("encoded_new_pass", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPassword_TokenNotFound_ThrowsIllegalArgumentException() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                authService.resetPassword("invalid-token", "new_pass"));

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void resetPassword_TokenExpired_ThrowsIllegalStateException() {
        User user = new User();
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken("expired-token");
        expiredToken.setUser(user);
        expiredToken.setExpiryDate(LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThrows(IllegalStateException.class, () ->
                authService.resetPassword("expired-token", "new_pass"));

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }
}
