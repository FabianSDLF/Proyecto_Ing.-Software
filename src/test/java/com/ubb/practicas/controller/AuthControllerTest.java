package com.ubb.practicas.controller;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.PasswordResetTokenRepository;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User existingUser;

    @BeforeEach
    public void setup() {
        cleanup();

        Role role = roleRepository.findByName("Estudiante");
        if (role == null) {
            role = new Role();
            role.setName("Estudiante");
            role = roleRepository.save(role);
        }

        existingUser = new User();
        existingUser.setEmail("registrado@ubb.cl");
        existingUser.setFullName("Usuario Registrado");
        existingUser.setPasswordHash("hash");
        existingUser.setActive(true);
        existingUser.setRole(role);
        existingUser = userRepository.save(existingUser);
    }

    @AfterEach
    public void cleanup() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testForgotPasswordConUsuarioInexistenteRetorna200SinEnumeracion() throws Exception {
        String json = "{\"email\":\"noexiste@ubb.cl\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.")))
                .andExpect(jsonPath("$.link").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertEquals(0, passwordResetTokenRepository.count());
    }

    @Test
    public void testForgotPasswordConUsuarioExistenteNoExponeTokenEnRespuesta() throws Exception {
        String json = "{\"email\":\"registrado@ubb.cl\"}";

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.")))
                .andExpect(jsonPath("$.link").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        assertEquals(1, passwordResetTokenRepository.count());
    }
}
