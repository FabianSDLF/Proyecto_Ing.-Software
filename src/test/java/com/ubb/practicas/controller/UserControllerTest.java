package com.ubb.practicas.controller;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for UserController.
 * Tests RBAC permissions, user CRUD operations, role assignments,
 * status changes, self-profile endpoints, and structured error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role studentRole;
    private Role tutorRole;
    private User testStudent;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminRole = getOrCreateRole("Administrador");
        studentRole = getOrCreateRole("Estudiante");
        tutorRole = getOrCreateRole("Tutor");

        testStudent = new User();
        testStudent.setFullName("Juan Estudiante");
        testStudent.setEmail("juan.estudiante@ubb.cl");
        testStudent.setPasswordHash(passwordEncoder.encode("Secret123!"));
        testStudent.setActive(true);
        testStudent.setRole(studentRole);
        testStudent.setPhone("+56911112222");
        testStudent.setCreatedAt(LocalDateTime.now());
        testStudent = userRepository.save(testStudent);
    }

    private Role getOrCreateRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            role = roleRepository.save(role);
        }
        return role;
    }

    // ==========================================
    // SECURITY & RBAC VERIFICATION
    // ==========================================

    @Test
    public void testObtenerUsuariosSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    public void testObtenerUsuariosConRolEstudianteRetorna403Forbidden() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testObtenerTodosUsuariosComoAdministradorRetorna200() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.email == 'juan.estudiante@ubb.cl')].fullName").value("Juan Estudiante"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testListarUsuariosPorRolComoAdministrador() throws Exception {
        mockMvc.perform(get("/api/usuarios/rol/Estudiante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].role.name", is("Estudiante")));
    }

    // ==========================================
    // USER CREATION & VALIDATION
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearUsuarioComoAdministradorExitoso() throws Exception {
        String json = "{"
                + "\"fullName\":\"Nuevo Docente\","
                + "\"email\":\"docente.nuevo@ubb.cl\","
                + "\"phone\":\"+56933334444\","
                + "\"role\":{\"name\":\"Tutor\"}"
                + "}";

        mockMvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("docente.nuevo@ubb.cl")))
                .andExpect(jsonPath("$.fullName", is("Nuevo Docente")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()); // Security: never expose hash

        User created = userRepository.findByEmail("docente.nuevo@ubb.cl");
        assertNotNull(created);
        assertTrue(created.getActive());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearUsuarioEmailDuplicadoRetorna409Conflict() throws Exception {
        String json = "{"
                + "\"fullName\":\"Duplicado Test\","
                + "\"email\":\"juan.estudiante@ubb.cl\"," // Existing email
                + "\"role\":{\"name\":\"Estudiante\"}"
                + "}";

        mockMvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("El email ya está en uso")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearUsuarioRolInexistenteRetorna404NotFound() throws Exception {
        String json = "{"
                + "\"fullName\":\"Rol Inexistente User\","
                + "\"email\":\"rol.fantasma@ubb.cl\","
                + "\"role\":{\"name\":\"ROL_QUE_NO_EXISTE\"}"
                + "}";

        mockMvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Rol no encontrado")));
    }

    // ==========================================
    // USER MUTATION & STATUS TOGGLING
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testActualizarRolUsuarioComoAdministrador() throws Exception {
        String json = "{\"roleName\":\"Tutor\"}";

        mockMvc.perform(put("/api/usuarios/" + testStudent.getId() + "/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role.name", is("Tutor")));

        User updated = userRepository.findById(testStudent.getId()).orElseThrow();
        assertEquals("Tutor", updated.getRole().getName());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testActualizarRolUsuarioPayloadInvalidoRetorna400() throws Exception {
        String json = "{}"; // Missing roleName

        mockMvc.perform(put("/api/usuarios/" + testStudent.getId() + "/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El nombre del rol es requerido"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testActualizarUsuarioDatosBasicos() throws Exception {
        String json = "{\"fullName\":\"Juan Actualizado\"}";

        mockMvc.perform(put("/api/usuarios/" + testStudent.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Juan Actualizado")));

        User updated = userRepository.findById(testStudent.getId()).orElseThrow();
        assertEquals("Juan Actualizado", updated.getFullName());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCambiarEstadoUsuario() throws Exception {
        String json = "{\"active\":false}";

        mockMvc.perform(patch("/api/usuarios/" + testStudent.getId() + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        User updated = userRepository.findById(testStudent.getId()).orElseThrow();
        assertFalse(updated.getActive());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCambiarEstadoUsuarioPayloadInvalidoRetorna400() throws Exception {
        String json = "{}";

        mockMvc.perform(patch("/api/usuarios/" + testStudent.getId() + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El campo 'active' es requerido"));
    }

    // ==========================================
    // USER PROFILE ENDPOINTS (/api/usuarios/perfil)
    // ==========================================

    @Test
    public void testObtenerMiPerfilSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "juan.estudiante@ubb.cl", roles = "ESTUDIANTE")
    public void testObtenerMiPerfilAutenticadoRetorna200() throws Exception {
        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("juan.estudiante@ubb.cl")))
                .andExpect(jsonPath("$.fullName", is("Juan Estudiante")));
    }

    @Test
    @WithMockUser(username = "juan.estudiante@ubb.cl", roles = "ESTUDIANTE")
    public void testActualizarMiPerfilAutenticadoExitoso() throws Exception {
        String json = "{\"fullName\":\"Juan Perfil Modificado\",\"phone\":\"+56999887766\"}";

        mockMvc.perform(put("/api/usuarios/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Juan Perfil Modificado")))
                .andExpect(jsonPath("$.phone", is("+56999887766")));

        User updated = userRepository.findByEmail("juan.estudiante@ubb.cl");
        assertEquals("Juan Perfil Modificado", updated.getFullName());
    }
}
