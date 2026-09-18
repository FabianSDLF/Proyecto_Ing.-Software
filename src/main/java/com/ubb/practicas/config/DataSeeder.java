package com.ubb.practicas.config;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner cargarDatosIniciales(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificamos si la tabla de roles está vacía
            if (roleRepository.count() == 0) {
                Role admin = new Role();
                admin.setName("Administrador");

                Role estudiante = new Role();
                estudiante.setName("Estudiante");

                Role profesor = new Role();
                profesor.setName("Profesor");

                roleRepository.saveAll(List.of(admin, estudiante, profesor));
                System.out.println("✅ SEMILLAS DE DATOS: Roles creados exitosamente en la BD.");
            }

            // Verificamos si existe el usuario administrador, si no, lo creamos
            if (userRepository.count() == 0) {
                Role adminRole = roleRepository.findByName("Administrador");
                if (adminRole != null) {
                    User adminUser = new User();
                    adminUser.setFullName("Administrador Sistema");
                    adminUser.setEmail("admin@ubb.cl");
                    // Contraseña encriptada con BCrypt
                    adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
                    adminUser.setActive(true);
                    adminUser.setRole(adminRole);
                    adminUser.setCreatedAt(LocalDateTime.now());
                    
                    userRepository.save(adminUser);
                    System.out.println("✅ SEMILLAS DE DATOS: Usuario Administrador (admin@ubb.cl) creado exitosamente.");
                }
            }
        };
    }
}