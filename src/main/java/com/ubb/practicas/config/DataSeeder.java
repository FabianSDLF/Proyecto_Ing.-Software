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
            AcademicPeriodRepository periodRepository,
            StudentPairRepository studentPairRepository) {
        return args -> {
            // 1. Crear Roles
            if (roleRepository.count() == 0) {
                Role admin = new Role(); admin.setName("Administrador");
                Role coord = new Role(); coord.setName("Coordinador de practicas");
                Role estudiante = new Role(); estudiante.setName("Estudiante");
                Role profesor = new Role(); profesor.setName("Profesor de Asignatura");
                Role colaborador = new Role(); colaborador.setName("Profesor Colaborador");
                Role tutor = new Role(); tutor.setName("Tutor");
                roleRepository.saveAll(List.of(admin, coord, estudiante, profesor, colaborador, tutor));
                System.out.println("SEMILLAS DE DATOS: Roles creados exitosamente en la BD.");
            }

            // 2. Crear Usuarios (Uno de cada tipo para Testing)
            if (userRepository.count() == 0) {
                User admin = createUser("Administrador General", "admin@ubb.cl", roleRepository.findByName("Administrador"), passwordEncoder);
                User coord = createUser("Coordinador de practicas", "coordinador@ubb.cl", roleRepository.findByName("Coordinador de practicas"), passwordEncoder);
                User estudiante = createUser("Estudiante Prueba", "estudiante@ubb.cl", roleRepository.findByName("Estudiante"), passwordEncoder);
                User estudiante1 = createUser("Estudiante Intermedio Uno", "estudiante1@ubb.cl", roleRepository.findByName("Estudiante"), passwordEncoder);
                User estudiante2 = createUser("Estudiante Intermedio Dos", "estudiante2@ubb.cl", roleRepository.findByName("Estudiante"), passwordEncoder);
                User estudiante3 = createUser("Estudiante Intermedio Tres", "estudiante3@ubb.cl", roleRepository.findByName("Estudiante"), passwordEncoder);
                User profeAsig = createUser("Profesor Asignatura Prueba", "profesor@ubb.cl", roleRepository.findByName("Profesor de Asignatura"), passwordEncoder);
                User profeColab = createUser("Colaborador Prueba", "colaborador@ubb.cl", roleRepository.findByName("Profesor Colaborador"), passwordEncoder);
                User tutor = createUser("Tutor Prueba", "tutor@ubb.cl", roleRepository.findByName("Tutor"), passwordEncoder);
                
                userRepository.saveAll(List.of(admin, coord, estudiante, estudiante1, estudiante2, estudiante3, profeAsig, profeColab, tutor));
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

            // 4. Crear Asignaturas (Inicial e Intermedia)
            if (subjectRepository.count() == 0) {
                User profeAsig = userRepository.findByEmail("profesor@ubb.cl");
                if (profeAsig != null) {
                    Subject subjectInicial = new Subject();
                    subjectInicial.setName("Práctica Inicial");
                    subjectInicial.setCode("PRC-100");
                    subjectInicial.setCycle(PracticeCycle.INICIAL);
                    subjectInicial.setProfessor(profeAsig);

                    Subject subjectIntermedio = new Subject();
                    subjectIntermedio.setName("Práctica Pedagógica Intermedia");
                    subjectIntermedio.setCode("PRC-200");
                    subjectIntermedio.setCycle(PracticeCycle.INTERMEDIO);
                    subjectIntermedio.setProfessor(profeAsig);

                    subjectRepository.saveAll(List.of(subjectInicial, subjectIntermedio));
                    System.out.println("SEMILLAS DE DATOS: Asignaturas de prueba creadas (Inicial e Intermedia).");
                }
            }

            // 5. Crear Centro de Práctica
            if (centerRepository.count() == 0) {
                PracticeCenter center = new PracticeCenter();
                center.setName("Liceo Bicentenario de Prueba");
                center.setDependency(com.ubb.practicas.model.enums.SchoolDependency.PUBLICO_SLEP);
                center.setAddress("Avenida Siempreviva 742");
                center.setCity("Concepción");
                center.setDistrict("Concepción");
                center.setPhone("+56912345678");
                center.setContactName("Director Prueba");
                center.setContactEmail("director@liceoprueba.cl");
                centerRepository.save(center);
                System.out.println("SEMILLAS DE DATOS: Centro de practica creado.");
            }

            // 6. Crear Asignaciones de Práctica (vinculando a todos)
            if (assignmentRepository.count() == 0) {
                User estudiante = userRepository.findByEmail("estudiante@ubb.cl");
                User estudiante1 = userRepository.findByEmail("estudiante1@ubb.cl");
                User estudiante2 = userRepository.findByEmail("estudiante2@ubb.cl");
                User estudiante3 = userRepository.findByEmail("estudiante3@ubb.cl");

                User colaborador = userRepository.findByEmail("colaborador@ubb.cl");
                User tutor = userRepository.findByEmail("tutor@ubb.cl");
                Subject subInicial = subjectRepository.findAll().stream().filter(s -> s.getCycle() == PracticeCycle.INICIAL).findFirst().orElse(null);
                Subject subIntermedio = subjectRepository.findAll().stream().filter(s -> s.getCycle() == PracticeCycle.INTERMEDIO).findFirst().orElse(null);
                PracticeCenter center = centerRepository.findAll().stream().findFirst().orElse(null);
                AcademicPeriod period = periodRepository.findAll().stream().findFirst().orElse(null);

                if (colaborador != null && tutor != null && center != null && period != null) {
                    if (estudiante != null && subInicial != null) {
                        PracticeAssignment asig0 = new PracticeAssignment();
                        asig0.setStudent(estudiante);
                        asig0.setSubject(subInicial);
                        asig0.setCenter(center);
                        asig0.setCollaborator(colaborador);
                        asig0.setTutor(tutor);
                        asig0.setPeriod(period);
                        asig0.setStatus(AssignmentStatus.ACTIVA);
                        asig0.setCreatedAt(LocalDateTime.now());
                        assignmentRepository.save(asig0);
                    }

                    if (subIntermedio != null) {
                        PracticeAssignment asig1 = null;
                        PracticeAssignment asig2 = null;

                        if (estudiante1 != null) {
                            asig1 = new PracticeAssignment();
                            asig1.setStudent(estudiante1);
                            asig1.setSubject(subIntermedio);
                            asig1.setCenter(center);
                            asig1.setCollaborator(colaborador);
                            asig1.setTutor(tutor);
                            asig1.setPeriod(period);
                            asig1.setStatus(AssignmentStatus.ACTIVA);
                            asig1.setCreatedAt(LocalDateTime.now());
                            asig1 = assignmentRepository.save(asig1);
                        }

                        if (estudiante2 != null) {
                            asig2 = new PracticeAssignment();
                            asig2.setStudent(estudiante2);
                            asig2.setSubject(subIntermedio);
                            asig2.setCenter(center);
                            asig2.setCollaborator(colaborador);
                            asig2.setTutor(tutor);
                            asig2.setPeriod(period);
                            asig2.setStatus(AssignmentStatus.ACTIVA);
                            asig2.setCreatedAt(LocalDateTime.now());
                            asig2 = assignmentRepository.save(asig2);
                        }

                        if (estudiante3 != null) {
                            PracticeAssignment asig3 = new PracticeAssignment();
                            asig3.setStudent(estudiante3);
                            asig3.setSubject(subIntermedio);
                            asig3.setCenter(center);
                            asig3.setCollaborator(colaborador);
                            asig3.setTutor(tutor);
                            asig3.setPeriod(period);
                            asig3.setStatus(AssignmentStatus.ACTIVA);
                            asig3.setCreatedAt(LocalDateTime.now());
                            assignmentRepository.save(asig3);
                        }

                        // 7. Crear Dupla Semilla Inicial entre estudiante1 y estudiante2
                        if (studentPairRepository.count() == 0 && asig1 != null && asig2 != null) {
                            StudentPair pair = new StudentPair();
                            pair.setAssignment1(asig1);
                            pair.setAssignment2(asig2);
                            pair.setPeriod(period);
                            pair.setCreatedAt(LocalDateTime.now());
                            studentPairRepository.save(pair);
                            System.out.println("SEMILLAS DE DATOS: Dupla de prueba creada entre estudiante1 y estudiante2.");
                        }
                    }

                    System.out.println("SEMILLAS DE DATOS: Asignaciones de prueba creadas vinculando todos los usuarios.");
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
