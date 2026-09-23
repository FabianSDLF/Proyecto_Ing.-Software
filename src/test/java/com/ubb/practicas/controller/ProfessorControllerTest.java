package com.ubb.practicas.controller;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class ProfessorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    private User professor;
    private User otherProfessor;
    private User admin;
    private User student;

    @BeforeEach
    public void setup() {
        cleanup();

        Role profRole = roleRepository.findByName("Profesor de Asignatura");
        if (profRole == null) {
            profRole = new Role();
            profRole.setName("Profesor de Asignatura");
            profRole = roleRepository.save(profRole);
        }

        Role adminRole = roleRepository.findByName("Administrador");
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("Administrador");
            adminRole = roleRepository.save(adminRole);
        }

        Role studentRole = roleRepository.findByName("Estudiante");
        if (studentRole == null) {
            studentRole = new Role();
            studentRole.setName("Estudiante");
            studentRole = roleRepository.save(studentRole);
        }

        professor = new User();
        professor.setEmail("profesor@ubb.cl");
        professor.setFullName("Profesor Test");
        professor.setPasswordHash("hash");
        professor.setActive(true);
        professor.setRole(profRole);
        professor = userRepository.save(professor);

        otherProfessor = new User();
        otherProfessor.setEmail("otro.profesor@ubb.cl");
        otherProfessor.setFullName("Otro Profesor");
        otherProfessor.setPasswordHash("hash");
        otherProfessor.setActive(true);
        otherProfessor.setRole(profRole);
        otherProfessor = userRepository.save(otherProfessor);

        admin = new User();
        admin.setEmail("admin@ubb.cl");
        admin.setFullName("Admin Pitagoras");
        admin.setPasswordHash("hash");
        admin.setActive(true);
        admin.setRole(adminRole);
        admin = userRepository.save(admin);

        student = new User();
        student.setEmail("estudiante@ubb.cl");
        student.setFullName("Estudiante Alumno");
        student.setPasswordHash("hash");
        student.setActive(true);
        student.setRole(studentRole);
        student = userRepository.save(student);
    }

    @AfterEach
    public void cleanup() {
        practiceAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testProfesorConsultaSusPropiosEstudiantesPermitido() throws Exception {
        mockMvc.perform(get("/api/profesor/" + professor.getId() + "/estudiantes")
                .principal(() -> professor.getEmail()))
                .andExpect(status().isOk());
    }

    @Test
    public void testAdminConsultaEstudiantesDeProfesorPermitido() throws Exception {
        mockMvc.perform(get("/api/profesor/" + professor.getId() + "/estudiantes")
                .principal(() -> admin.getEmail()))
                .andExpect(status().isOk());
    }

    @Test
    public void testOtroUsuarioConsultaEstudiantesDeProfesorDenegado403() throws Exception {
        mockMvc.perform(get("/api/profesor/" + professor.getId() + "/estudiantes")
                .principal(() -> otherProfessor.getEmail()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("no tiene permisos para consultar los estudiantes")));
    }

    @Test
    public void testEstudianteConsultaEstudiantesDeProfesorDenegado403() throws Exception {
        mockMvc.perform(get("/api/profesor/" + professor.getId() + "/estudiantes")
                .principal(() -> student.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testConsultaUsuarioConRolNoDocenteRetornaMensajeValido() throws Exception {
        mockMvc.perform(get("/api/profesor/" + student.getId() + "/estudiantes")
                .principal(() -> admin.getEmail()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("El usuario no tiene un rol docente válido")));
    }
}
