package com.ubb.practicas.controller;

import com.ubb.practicas.model.PasswordResetToken;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.PasswordResetTokenRepository;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for AuthController.
 * Validates authentication flows, account enumeration protection,
 * token generation, password resets, and RFC 7807 error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User existingUser;
    private User disabledUser;

    @BeforeEach
    public void setup() {
        Role role = roleRepository.findByName("Estudiante");
        if (role == null) {
            role = new Role();
            role.setName("Estudiante");
            role = roleRepository.save(role);
        }

        existingUser = new User();
        existingUser.setEmail("estudiante.activo@ubb.cl");
        existingUser.setFullName("Estudiante Activo");
        existingUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        existingUser.setActive(true);
        existingUser.setRole(role);
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser = userRepository.save(existingUser);

        disabledUser = new User();
        disabledUser.setEmail("estudiante.inactivo@ubb.cl");
        disabledUser.setFullName("Estudiante Inactivo");
        disabledUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        disabledUser.setActive(false);
        disabledUser.setRole(role);
        disabledUser.setCreatedAt(LocalDateTime.now());
        disabledUser = userRepository.save(disabledUser);
    }

    // ==========================================
    // LOGIN ENDPOINT TESTS (/api/auth/login)
    // ==========================================

    @Test
    public void testLoginExitosoRetornaTokenYUsuario() throws Exception {
        String json = "{\"email\":\"estudiante.activo@ubb.cl\",\"password\":\"Password123!\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.email", is("estudiante.activo@ubb.cl")))
                .andExpect(jsonPath("$.user.fullName", is("Estudiante Activo")))
                .andExpect(jsonPath("$.user.active", is(true)))
                .andExpect(jsonPath("$.user.role.name", is("Estudiante")));
    }

    @Test
    public void testLoginClaveIncorrectaRetorna401() throws Exception {
        String json = "{\"email\":\"estudiante.activo@ubb.cl\",\"password\":\"ClaveErronea999\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    public void testLoginUsuarioInexistenteRetorna401() throws Exception {
        String json = "{\"email\":\"no.existe@ubb.cl\",\"password\":\"Password123!\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    public void testLoginUsuarioDesactivadoRetorna403() throws Exception {
        String json = "{\"email\":\"estudiante.inactivo@ubb.cl\",\"password\":\"Password123!\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Cuenta desactivada"));
    }

    // ==========================================
    // FORGOT PASSWORD TESTS (/api/auth/forgot-password)
    // ==========================================

    @Test
    public void testForgotPasswordConUsuarioExistenteNoExponeTokenEnRespuesta() throws Exception {
        String json = "{\"email\":\"estudiante.activo@ubb.cl\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.link").doesNotExist());

        assertEquals(1, passwordResetTokenRepository.count());
    }

    @Test
    public void testForgotPasswordConUsuarioInexistenteRetorna200SinEnumeracion() throws Exception {
        long initialTokens = passwordResetTokenRepository.count();
        String json = "{\"email\":\"fantasma@ubb.cl\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.link").doesNotExist());

        assertEquals(initialTokens, passwordResetTokenRepository.count());
    }

    // ==========================================
    // RESET PASSWORD TESTS (/api/auth/reset-password)
    // ==========================================

    @Test
    public void testResetPasswordExitosoActualizaPassword() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(rawToken);
        token.setUser(existingUser);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(token);

        String json = String.format("{\"token\":\"%s\",\"newPassword\":\"NuevaClaveSegura2026!\"}", rawToken);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Contraseña restablecida exitosamente.")));

        // Verify password hash updated in DB
        User updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NuevaClaveSegura2026!", updatedUser.getPasswordHash()));

        // Verify token deleted
        assertTrue(passwordResetTokenRepository.findByToken(rawToken).isEmpty());
    }

    @Test
    public void testResetPasswordTokenInvalidoRetorna400() throws Exception {
        String json = "{\"token\":\"token-completamente-falso\",\"newPassword\":\"Clave123!\"}";

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El enlace de recuperación es inválido o no existe."));
    }

    @Test
    public void testResetPasswordTokenExpiradoRetorna409() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken(rawToken);
        expiredToken.setUser(existingUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusHours(1)); // Expired
        passwordResetTokenRepository.save(expiredToken);

        String json = String.format("{\"token\":\"%s\",\"newPassword\":\"NuevaClave123!\"}", rawToken);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("El enlace ha expirado. Solicita uno nuevo."));
    }
}
