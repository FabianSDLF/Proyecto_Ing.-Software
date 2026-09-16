package com.ubb.practicas.config;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner cargarDatosIniciales(RoleRepository roleRepository) {
        return args -> {
            // Verificamos si la tabla de roles está vacía
            if (roleRepository.count() == 0) {

                Role admin = new Role();
                admin.setName("Administrador");

                Role estudiante = new Role();
                estudiante.setName("Estudiante");

                Role profesor = new Role();
                profesor.setName("Profesor");

                // Guardamos los 3 roles de golpe en MySQL
                roleRepository.saveAll(List.of(admin, estudiante, profesor));

                System.out.println("✅ SEMILLAS DE DATOS: Roles creados exitosamente en la BD.");
            }
        };
    }
}