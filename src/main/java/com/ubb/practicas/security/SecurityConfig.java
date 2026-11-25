package com.ubb.practicas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuditRequestFilter auditFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuditRequestFilter auditFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.auditFilter = auditFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, denied) -> response.setStatus(HttpStatus.FORBIDDEN.value()))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/usuarios/perfil").authenticated()
                .requestMatchers("/api/usuarios/carga-masiva").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS"
                )
                .requestMatchers("/api/usuarios/**").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS"
                )
                .requestMatchers("/api/asignaciones/mis-asignaciones", "/api/asignaciones/visibles", "/api/asignaciones/*/ficha").authenticated()
                .requestMatchers("/api/admin/**").hasAnyRole("Administrador", "ADMINISTRADOR")
                .requestMatchers("/api/asignaciones/colaborador/**", "/api/asignaciones/tutor/**").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS",
                    "PROFESOR_COLABORADOR", "Profesor Colaborador",
                    "TUTOR", "Tutor",
                    "PROFESOR_DE_ASIGNATURA", "Profesor de Asignatura"
                )
                .requestMatchers("/api/asignaciones/**").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS"
                )
                .requestMatchers("/api/carga-masiva/**").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS"
                )
                .requestMatchers("/api/duplas/**").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS"
                )
                .requestMatchers("/api/notificaciones/comunicado", "/api/notificaciones/destinatarios").hasAnyRole(
                    "Administrador", "ADMINISTRADOR",
                    "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS",
                    "Profesor de Asignatura", "PROFESOR_DE_ASIGNATURA"
                )
                .requestMatchers("/api/portafolio/download-zip").hasAnyRole("Administrador", "ADMINISTRADOR", "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS", "Profesor de Asignatura", "PROFESOR_DE_ASIGNATURA")
                .requestMatchers("/api/portafolio/export-csv").hasAnyRole("Administrador", "ADMINISTRADOR", "Coordinador de practicas", "COORDINADOR_DE_PRACTICAS", "Profesor de Asignatura", "PROFESOR_DE_ASIGNATURA", "Profesor Colaborador", "PROFESOR_COLABORADOR", "Tutor", "TUTOR")
                .requestMatchers("/api/portafolio/**", "/api/profesor/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(auditFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<AuditRequestFilter> auditRegistration() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(auditFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
