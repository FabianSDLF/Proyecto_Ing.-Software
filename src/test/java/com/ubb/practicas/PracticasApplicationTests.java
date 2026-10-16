package com.ubb.practicas;

import com.ubb.practicas.controller.AuthController;
import com.ubb.practicas.controller.SubjectController;
import com.ubb.practicas.controller.UserController;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.AuthService;
import com.ubb.practicas.service.SubjectService;
import com.ubb.practicas.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that the Spring Boot Application Context loads completely
 * with all critical controllers, services, and repositories properly wired.
 * Resolves TS-01 (Empty Test Smell) and uses H2 test profile isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
class PracticasApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SubjectController subjectController;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserController userController;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Spring ApplicationContext must be initialized");
        assertNotNull(subjectController, "SubjectController bean must be loaded in context");
        assertNotNull(authController, "AuthController bean must be loaded in context");
        assertNotNull(userController, "UserController bean must be loaded in context");
        assertNotNull(subjectService, "SubjectService bean must be loaded in context");
        assertNotNull(userService, "UserService bean must be loaded in context");
        assertNotNull(authService, "AuthService bean must be loaded in context");
        assertNotNull(subjectRepository, "SubjectRepository bean must be loaded in context");
        assertNotNull(userRepository, "UserRepository bean must be loaded in context");
    }
}
