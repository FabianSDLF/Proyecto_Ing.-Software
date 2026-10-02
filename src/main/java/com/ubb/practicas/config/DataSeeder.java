package com.ubb.practicas.config;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    public CommandLineRunner cargarDatosIniciales(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PracticeCenterRepository centerRepository,
            SubjectRepository subjectRepository,
            PracticeAssignmentRepository assignmentRepository,
            AcademicPeriodRepository periodRepository) {
        return args -> {
            // 1. Crear Roles
            if (roleRepository.count() == 0) {
                Role admin = new Role(); admin.setName("Coordinador de practicas");
                Role estudiante = new Role(); estudiante.setName("Estudiante");
                Role profesor = new Role(); profesor.setName("Profesor de Asignatura");
                Role colaborador = new Role(); colaborador.setName("Profesor Colaborador");
                Role tutor = new Role(); tutor.setName("Tutor");
                roleRepository.saveAll(List.of(admin, estudiante, profesor, colaborador, tutor));
                System.out.println("SEMILLAS DE DATOS: Roles creados exitosamente en la BD.");
            }

            // 2. Crear Usuarios (Uno de cada tipo para Testing)
            if (userRepository.count() == 0) {
                User admin = createUser("Coordinador de practicas", "coordinador@ubb.cl", roleRepository.findByName("Coordinador de practicas"), passwordEncoder);
                User estudiante = createUser("Estudiante Prueba", "estudiante@ubb.cl", roleRepository.findByName("Estudiante"), passwordEncoder);
                User profeAsig = createUser("Profesor Asignatura Prueba", "profesor@ubb.cl", roleRepository.findByName("Profesor de Asignatura"), passwordEncoder);
                User profeColab = createUser("Colaborador Prueba", "colaborador@ubb.cl", roleRepository.findByName("Profesor Colaborador"), passwordEncoder);
                User tutor = createUser("Tutor Prueba", "tutor@ubb.cl", roleRepository.findByName("Tutor"), passwordEncoder);
                
                userRepository.saveAll(List.of(admin, estudiante, profeAsig, profeColab, tutor));
                System.out.println("SEMILLAS DE DATOS: Usuarios de prueba creados exitosamente.");
            }

            // 3. Crear Periodo Académico
            if (periodRepository.count() == 0) {
                AcademicPeriod periodo = new AcademicPeriod();
                periodo.setName("Primer Semestre 2026");
                periodo.setYear(2026);
                periodo.setSemesterNumber(1);
                periodo.setStartDate(LocalDate.of(2026, 3, 1));
                periodo.setEndDate(LocalDate.of(2026, 7, 31));
                periodo.setStatus(com.ubb.practicas.model.enums.PeriodStatus.ACTIVO);
                periodRepository.save(periodo);
                System.out.println("SEMILLAS DE DATOS: Periodo academico creado.");
            }

            // 4. Crear Asignatura
            if (subjectRepository.count() == 0) {
                User profeAsig = userRepository.findByEmail("profesor@ubb.cl");
                if (profeAsig != null) {
                    Subject subject = new Subject();
                    subject.setName("Práctica Inicial");
                    subject.setCode("PRC-100");
                    subject.setCycle(PracticeCycle.INICIAL);
                    subject.setProfessor(profeAsig);
                    subjectRepository.save(subject);
                    System.out.println("SEMILLAS DE DATOS: Asignatura de prueba creada.");
                }
            }

            // 5. Crear Centro de Práctica
            if (centerRepository.count() == 0) {
                PracticeCenter center = new PracticeCenter();
                center.setName("Liceo Bicentenario de Prueba");
                center.setDependency(com.ubb.practicas.model.enums.SchoolDependency.PUBLICO_SLEP);
                center.setAddress("Avenida Siempreviva 742");
                center.setCity("Concepcin");
                center.setDistrict("Concepcin");
                center.setPhone("+56912345678");
                center.setContactName("Director Prueba");
                center.setContactEmail("director@liceoprueba.cl");
                centerRepository.save(center);
                System.out.println("SEMILLAS DE DATOS: Centro de practica creado.");
            }

            // 6. Crear Asignación de Práctica (vinculando a todos)
            if (assignmentRepository.count() == 0) {
                User estudiante = userRepository.findByEmail("estudiante@ubb.cl");
                User colaborador = userRepository.findByEmail("colaborador@ubb.cl");
                User tutor = userRepository.findByEmail("tutor@ubb.cl");
                Subject subject = subjectRepository.findAll().stream().findFirst().orElse(null);
                PracticeCenter center = centerRepository.findAll().stream().findFirst().orElse(null);
                AcademicPeriod period = periodRepository.findAll().stream().findFirst().orElse(null);

                if (estudiante != null && colaborador != null && tutor != null && subject != null && center != null && period != null) {
                    PracticeAssignment assignment = new PracticeAssignment();
                    assignment.setStudent(estudiante);
                    assignment.setSubject(subject);
                    assignment.setCenter(center);
                    assignment.setCollaborator(colaborador);
                    assignment.setTutor(tutor);
                    assignment.setPeriod(period);
                    assignment.setStatus(AssignmentStatus.ACTIVA);
                    assignment.setCreatedAt(LocalDateTime.now());
                    assignmentRepository.save(assignment);
                    System.out.println("SEMILLAS DE DATOS: Asignacion de prueba creada vinculando todos los usuarios.");
                }
            }
        };
    }

    private User createUser(String fullName, String email, Role role, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(email.split("@")[0] + "123")); // e.g. admin123, tutor123
        user.setActive(true);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
