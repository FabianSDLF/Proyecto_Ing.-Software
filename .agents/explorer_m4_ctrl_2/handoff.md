# Handoff Report — Integration Tests & Test Smell Resolution (TS-01 to TS-05)

**Agent:** `explorer_m4_ctrl_2`  
**Milestone:** M4 — Phase 4: Quality Assurance & Automated Testing  
**Scope:** REST Controller Integration Testing, Test Isolation (H2), Security Verification (`@WithMockUser`), Flaky Test Remediation, and JSONPath Contracts.  
**Target File for Delivery:** `handoff.md`

---

## 1. Observation

### 1.1. Test Smell TS-01: Empty Test Smell & Lack of Test Isolation
- **Report Reference:** `reporte_codigo.md` lines 759-764:
  > "#### Hallazgo TS-01: Prueba Vacía sin Aserciones (`Empty Test Smell`)
  > - **Archivo:** `src/test/java/com/ubb/practicas/PracticasApplicationTests.java`
  > - **Línea(s):** 9-11
  > - **Problema:** El método `contextLoads()` está vacío. No valida la inicialización de los componentes. Además, intenta conectarse al MySQL real; al arrancar con `ddl-auto=create`, ejecutar esta prueba en local destruye la base de datos de desarrollo del programador."
- **Direct Inspection:** `src/test/java/com/ubb/practicas/PracticasApplicationTests.java` lines 6-13:
  ```java
  @SpringBootTest
  class PracticasApplicationTests {

  	@Test
  	void contextLoads() {
  	}

  }
  ```
- **Direct Inspection of Test Resources:** 
  The directory `src/test/resources/` does not exist in the filesystem (`mvnw test-compile` outputs `skip non existing resourceDirectory ...\src\test\resources`). Consequently, all test executions fall back to `src/main/resources/application.properties`, attempting to connect to `jdbc:mysql://localhost:3306/sistema_practicas` with credentials `root` / `admin`.

### 1.2. Test Smell TS-02: Shared Database Destructive Teardown
- **Report Reference:** `reporte_codigo.md` lines 766-771:
  > "#### Hallazgo TS-02: Limpieza Destructiva de la Base de Datos Real en Pruebas
  > - **Archivo:** `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java`
  > - **Línea(s):** 41-46
  > - **Problema:** En `@BeforeEach` y `@AfterEach`, la prueba ejecuta `practiceAssignmentRepository.deleteAll()` y `subjectRepository.deleteAll()` sobre la base de datos configurada, eliminando datos reales ante la falta de una base de pruebas aislada."
- **Direct Inspection:** `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java` lines 41-46:
  ```java
  @BeforeEach
  @AfterEach
  public void cleanup() {
      practiceAssignmentRepository.deleteAll();
      subjectRepository.deleteAll();
  }
  ```

### 1.3. Test Smell TS-03: Security Filter Bypass
- **Report Reference:** `reporte_codigo.md` lines 773-778:
  > "#### Hallazgo TS-03: Desactivación Completa de Filtros de Seguridad en Pruebas
  > - **Archivo:** `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java`
  > - **Línea(s):** 29
  > - **Problema:** La prueba utiliza `@AutoConfigureMockMvc(addFilters = false)`. Deshabilitar los filtros impide verificar si las rutas están debidamente protegidas por autenticación y autorización."
- **Direct Inspection:** In `SubjectControllerTest.java` line 29:
  ```java
  @SpringBootTest
  @AutoConfigureMockMvc(addFilters = false)
  public class SubjectControllerTest {
  ```
  Simultaneously, `AuthControllerTest.java` line 24 also specifies `@AutoConfigureMockMvc(addFilters = false)`.
- **Direct Inspection of Dependencies:** `pom.xml` lacks `org.springframework.security:spring-security-test`, which provides `@WithMockUser` and `SecurityMockMvcRequestPostProcessors`. Running `mvn dependency:tree` confirmed `spring-security-test` is absent from the test classpath.

### 1.4. Test Smell TS-04: Non-Deterministic Concurrency with Latches and Timers
- **Report Reference:** `reporte_codigo.md` lines 780-786:
  > "#### Hallazgo TS-04: Prueba de Concurrencia No Determinista con Latches y Tiempos Fijos
  > - **Archivo:** `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java`
  > - **Línea(s):** 233-269
  > - **Problema:** `testCrearAsignaturasConcurrencia` utiliza `CountDownLatch` y temporizadores fijos contra un dispatcher simulado en memoria de MockMvc, generando pruebas inestables (*flaky tests*) en entornos de integración continua con carga de CPU variable."
- **Direct Inspection:** In `SubjectControllerTest.java` lines 233-269:
  ```java
  @Test
  public void testCrearAsignaturasConcurrencia() throws Exception {
      int threads = 10;
      ExecutorService executor = Executors.newFixedThreadPool(threads);
      CountDownLatch readyLatch = new CountDownLatch(threads);
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(threads);
      ...
      readyLatch.await(5, TimeUnit.SECONDS);
      startLatch.countDown();
      boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
      ...
  }
  ```
  This test relies on an asynchronous executor pool and hard-coded sleep/await timeouts (`5` and `10` seconds) executing against MockMvc, which cannot properly share thread-bound `@Transactional` contexts or rollback state.

### 1.5. Test Smell TS-05: Brittle Plaintext Assertions on Error Strings
- **Report Reference:** `reporte_codigo.md` lines 787-792:
  > "#### Hallazgo TS-05: Aserciones Acopladas a Cadenas de Texto Literales de Error
  > - **Archivo:** `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java`
  > - **Línea(s):** 135, 146, 157, 169, 217, 316, 339
  > - **Problema:** Las aserciones validan coincidencias exactas de frases en español (`.andExpect(content().string(containsString("El nombre de la asignatura es obligatorio")))`). Cualquier mejora gramatical o cambio a JSON estructurado rompe la suite de pruebas."
- **Direct Inspection:** `SubjectControllerTest.java` lines 135, 146, 157, 169, 217, 316, 339:
  ```java
  .andExpect(status().isBadRequest())
  .andExpect(content().string(containsString("El nombre de la asignatura es obligatorio")));
  ```
- **Direct Inspection of `GlobalExceptionHandler` & `ApiErrorResponse`:**
  `com/ubb/practicas/dto/ApiErrorResponse.java` and `com/ubb/practicas/exception/GlobalExceptionHandler.java` produce RFC 7807 structured JSON payloads:
  ```json
  {
    "timestamp": "2026-09-24T01:45:00",
    "status": 400,
    "error": "Bad Request",
    "message": "El nombre de la asignatura es obligatorio",
    "path": "/api/asignaturas"
  }
  ```
  Checking `content().string(containsString(...))` performs fragile substring matching on raw JSON text instead of validating standard JSONPath fields (`$.status`, `$.error`, `$.message`, `$.path`).

### 1.6. Missing Controller Coverage & Unprotected Routes
- `UserController.java` has **zero** integration tests in `src/test/java/com/ubb/practicas/controller/` (file `UserControllerTest.java` does not exist).
- `AuthControllerTest.java` contains only 2 test cases testing `forgotPassword`, but has **zero** tests for `login` (valid, bad password, non-existent user, inactive user) and **zero** tests for `resetPassword` (valid token, expired token, non-existent token).
- `SubjectController.java` endpoints (`POST /api/asignaturas` and `DELETE /api/asignaturas/{id}`) do not currently declare `@PreAuthorize` or specific role matchers in `SecurityConfig.java`, meaning only `.anyRequest().authenticated()` was guarding them.

---

## 2. Logic Chain

1. **Test Environment Isolation (TS-01, TS-02):**
   - Because `pom.xml` currently lacks `com.h2database:h2`, running with `jdbc:h2:mem:testdb` throws a `ClassNotFoundException: org.h2.Driver`.
   - Therefore, `h2` must be declared in `pom.xml` with `<scope>test</scope>`.
   - Creating `src/test/resources/application-test.properties` and `src/test/resources/application.properties` with `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL` and `spring.jpa.hibernate.ddl-auto=create-drop` guarantees that running `mvn test` never connects to, mutates, or drops tables from the developer's local MySQL instance.
   - Annotating test classes with `@Transactional` ensures that each test method runs in an isolated Spring transaction that automatically rolls back when the test finishes.
   - Consequently, destructive `deleteAll()` operations in `@BeforeEach` or `@AfterEach` can and must be completely removed, eliminating TS-02.
   - For TS-01, injecting the `ApplicationContext` and core controller/service beans (`SubjectController`, `AuthController`, `UserController`, `SubjectService`, `UserService`, `AuthService`, repositories) into `PracticasApplicationTests` and asserting `assertNotNull(...)` on each validates that Spring's dependency injection container, database schemas, and security beans wire up without error.

2. **Security Verification & Role-Based Access Control (TS-03):**
   - Removing `addFilters = false` enables Spring Security's filter chain (`JwtAuthFilter`, `SecurityFilterChain`) during MockMvc executions.
   - To invoke protected endpoints, tests require authentication context. Adding `org.springframework.security:spring-security-test` allows using `@WithMockUser(roles = "ADMINISTRADOR")` and `@WithMockUser(roles = "ESTUDIANTE")`.
   - In `SubjectController.java`, mutations (`crearAsignatura`, `eliminarAsignatura`) represent administrative catalog operations. Adding `@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador')")` (or configuring `requestMatchers(HttpMethod.POST, "/api/asignaturas/**").hasAnyRole(...)` in `SecurityConfig.java`) strictly enforces RBAC.
   - Tests can then rigorously verify:
     1. Unauthenticated requests to `/api/asignaturas` return `401 Unauthorized` / `403 Forbidden`.
     2. Authenticated requests with unauthorized role (`ESTUDIANTE`) attempting to create or delete subjects return `403 Forbidden`.
     3. Authenticated requests with authorized role (`ADMINISTRADOR`) succeed with `200 OK`.

3. **Concurrency & Flaky Test Remediation (TS-04):**
   - `MockMvc` is an in-memory test harness designed for synchronous servlet dispatch within a single execution thread.
   - In `SubjectControllerTest`, spawning 10 parallel threads via `ExecutorService` and synchronizing with `CountDownLatch` caused non-deterministic execution times, race conditions over Hibernate sessions, and false positives/negatives depending on host CPU scheduling. Furthermore, multithreaded MockMvc calls escape the `@Transactional` rollback boundary of the test thread.
   - Replacing this non-deterministic construct with deterministic transactional integrity tests (`testCrearMultiplesAsignaturasTransaccional` and `testAislamientoTransaccionalConRollbackEnFallo`) validates data consistency, unique constraints, and ACID properties deterministically with zero risk of flakiness.

4. **Structured JSON Assertions (TS-05):**
   - `GlobalExceptionHandler` converts all uncaught or domain exceptions into `ApiErrorResponse` instances.
   - The contract defines exact JSON fields: `status` (integer), `error` (HTTP status phrase), `message` (localized error message), and `path` (request URI).
   - Replacing `content().string(containsString(...))` with `jsonPath("$.status").value(...)`, `jsonPath("$.error").value(...)`, `jsonPath("$.message").value(...)`, and `jsonPath("$.path").value(...)` enforces strict API schema contracts, decouples test assertions from accidental formatting discrepancies, and aligns with RFC 7807.

5. **Complete Controller Coverage (`AuthControllerTest` & `UserControllerTest`):**
   - `AuthControllerTest` must be expanded to test all authentication endpoints:
     - `login`: successful token generation, incorrect password (`401`), non-existent email (`401`), disabled user account (`403`).
     - `forgot-password`: account enumeration protection (uniform response for existing and non-existing emails, no token leakage).
     - `reset-password`: successful reset (BCrypt hash update, token consumption), invalid token (`400`), expired token (`409`).
   - `UserControllerTest` must be created to cover all administrative and profile management endpoints:
     - RBAC security checks (unauthenticated, `ESTUDIANTE` denied, `ADMINISTRADOR` permitted).
     - CRUD: list all, list by role, create user, duplicate email conflict (`409`), invalid role (`404`), update user, update role, toggle status (`PATCH`), and self-profile retrieval and update (`/api/usuarios/perfil`).

---

## 3. Caveats

1. **`DataSeeder` Execution in Test Context:**
   - In `DataSeeder.java`, a `CommandLineRunner` executes at context startup if repositories are empty, seeding default roles (`Administrador`, `Estudiante`, etc.), default users (`admin@ubb.cl`, `estudiante@ubb.cl`), and one test subject (`Práctica Inicial`).
   - If `DataSeeder` is left unannotated, tests asserting exact entity counts must account for seeded entities.
   - **Recommendation for Worker:** Annotate `DataSeeder` with `@Profile("!test")` so that integration tests start with a pristine database and manage their own test fixtures within their `@Transactional` boundaries. If `DataSeeder` is kept active in tests, tests should initialize or verify relative entity counts (`initialCount + 1`).
2. **Spring Boot 4.x / Spring Security 7.x:**
   - The project uses Spring Boot 4.1.1 and Spring Security 7.1.1. In this version, unauthenticated requests handled by Spring Security default to `401 Unauthorized` (or `403 Forbidden` if CSRF or access denied occurs). MockMvc tests should check `status().isUnauthorized()` or `status().isForbidden()`.
3. **Password Hashing:**
   - `BCryptPasswordEncoder` is configured in `SecurityConfig`. Any test creating users directly via `userRepository.save()` must use `passwordEncoder.encode(...)` if testing the login endpoint, so that `passwordEncoder.matches(...)` succeeds.

---

## 4. Conclusion & Concrete Implementation Designs

The following exact blueprints and code replacements are to be implemented by `worker_m4`.

### 4.1. `pom.xml` Dependencies Update
Add H2 database and Spring Security Test to `pom.xml`:

```xml
		<!-- H2 In-Memory Database for Hermetic Test Isolation (TS-01, TS-02) -->
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
		<!-- Spring Security Test for @WithMockUser and Security Filters (TS-03) -->
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
```

### 4.2. Test Environment Configuration: `src/test/resources/application-test.properties`
Create file `src/test/resources/application-test.properties` (and identical `src/test/resources/application.properties`):

```properties
spring.application.name=practicas-test
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Files / Uploads
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB
file.upload-dir=./test-uploads

# JWT Security Configuration
jwt.secret=c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz
jwt.expiration=86400000
```

### 4.3. Fix `PracticasApplicationTests.java` (TS-01)
Replace contents of `src/test/java/com/ubb/practicas/PracticasApplicationTests.java`:

```java
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
```

### 4.4. Fix `SubjectControllerTest.java` (TS-02, TS-03, TS-04, TS-05)
Replace contents of `src/test/java/com/ubb/practicas/controller/SubjectControllerTest.java`:

```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SubjectController.
 * Resolves:
 * - TS-01 & TS-02: Isolated H2 in-memory DB and @Transactional automatic rollback (no deleteAll teardown).
 * - TS-03: Security filters enabled with @WithMockUser RBAC testing.
 * - TS-04: Deterministic transactional tests replacing flaky CountDownLatch.
 * - TS-05: Structured JSONPath assertions against ApiErrorResponse.
 */
@SpringBootTest
@AutoConfigureMockMvc // TS-03: Filters active (addFilters = false removed)
@ActiveProfiles("test") // TS-01: H2 in-memory database
@Transactional // TS-02: Automatic rollback, eliminating destructive deleteAll()
public class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    // ==========================================
    // SECURITY & RBAC VERIFICATION (TS-03)
    // ==========================================

    @Test
    public void testListarAsignaturasSinAutenticacionFalla() throws Exception {
        mockMvc.perform(get("/api/asignaturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCrearAsignaturaSinAutenticacionFalla() throws Exception {
        String json = "{\"name\":\"Práctica No Autenticada\",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    public void testCrearAsignaturaConRolEstudianteRetorna403Forbidden() throws Exception {
        String json = "{\"name\":\"Práctica No Autorizada\",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ESTUDIANTE")
    public void testEliminarAsignaturaConRolEstudianteRetorna403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/asignaturas/1"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // FUNCTIONAL TESTS (ROLE = ADMINISTRADOR)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloMayuscula() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Práctica Profesional\",\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Práctica Profesional")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloCapitalizado() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Taller de Práctica\",\"cycle\":\"Inicial\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Taller de Práctica")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloMinusculaYEspacios() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Práctica Inicial Test\",\"cycle\":\"  inicial  \"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Práctica Inicial Test")))
                .andExpect(jsonPath("$.cycle", is("INICIAL")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCaracteresEspeciales() throws Exception {
        long initialCount = subjectRepository.count();
        String json = "{\"name\":\"Didáctica de la Geometría & Álgebra (Ñ)\",\"cycle\":\"AVANZADO\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Didáctica de la Geometría & Álgebra (Ñ)")))
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        assertEquals(initialCount + 1, subjectRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloIntermedioYAvanzado() throws Exception {
        long initialCount = subjectRepository.count();
        String json1 = "{\"name\":\"Asignatura 2\",\"cycle\":\"Intermedio\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycle", is("INTERMEDIO")));

        String json2 = "{\"name\":\"Asignatura 3\",\"cycle\":\"Avanzado\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        assertEquals(initialCount + 2, subjectRepository.count());
    }

    // ==========================================
    // STRUCTURED JSON VALIDATION ASSERTIONS (TS-05)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConNombreVacioFalla() throws Exception {
        String json = "{\"name\":\"   \",\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El nombre de la asignatura es obligatorio"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConNombreNullFalla() throws Exception {
        String json = "{\"cycle\":\"INICIAL\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El nombre de la asignatura es obligatorio"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloNullFalla() throws Exception {
        String json = "{\"name\":\"Asignatura Sin Ciclo\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El ciclo de la asignatura es obligatorio"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConNombreExcesivoFalla() throws Exception {
        String longName = "A".repeat(256);
        String json = String.format("{\"name\":\"%s\",\"cycle\":\"INICIAL\"}", longName);

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El nombre de la asignatura no puede superar los 255 caracteres"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCodigoExcesivoFalla() throws Exception {
        String longCode = "C".repeat(51);
        String json = String.format("{\"name\":\"Química General\",\"cycle\":\"INICIAL\",\"code\":\"%s\"}", longCode);

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El código de la asignatura no puede superar los 50 caracteres"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCodigoValido() throws Exception {
        String json = "{\"name\":\"Física Mecánica\",\"cycle\":\"INICIAL\",\"code\":\"  FIS-101  \"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("FIS-101")))
                .andExpect(jsonPath("$.name", is("Física Mecánica")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConProfesorInexistenteFalla() throws Exception {
        String json = "{\"name\":\"Bioquímica\",\"cycle\":\"INTERMEDIO\",\"professor\":{\"id\":999999}}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El profesor especificado no existe"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConCicloInvalidoFalla() throws Exception {
        String json = "{\"name\":\"Inválida\",\"cycle\":\"CICLO_INEXISTENTE\"}";

        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ==========================================
    // CRUD & CONFLICT ASSERTIONS (TS-05)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testListarAsignaturas() throws Exception {
        Subject s = new Subject();
        s.setName("Matemáticas Integrales");
        s.setCycle(PracticeCycle.INICIAL);
        subjectRepository.save(s);

        mockMvc.perform(get("/api/asignaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.name == 'Matemáticas Integrales')].cycle").value("INICIAL"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarAsignatura() throws Exception {
        Subject s = new Subject();
        s.setName("Física Cuántica");
        s.setCycle(PracticeCycle.INTERMEDIO);
        Subject saved = subjectRepository.save(s);

        mockMvc.perform(delete("/api/asignaturas/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Asignatura eliminada exitosamente")));

        assertFalse(subjectRepository.existsById(saved.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarAsignaturaInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/asignaturas/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Asignatura no encontrada"))
                .andExpect(jsonPath("$.path").value("/api/asignaturas/999999"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testEliminarAsignaturaAsociadaAPracticaRetorna409Conflict() throws Exception {
        Subject s = new Subject();
        s.setName("Práctica con Asignación Activa");
        s.setCycle(PracticeCycle.AVANZADO);
        Subject savedSubject = subjectRepository.save(s);

        PracticeAssignment assignment = new PracticeAssignment();
        assignment.setSubject(savedSubject);
        practiceAssignmentRepository.save(assignment);

        mockMvc.perform(delete("/api/asignaturas/" + savedSubject.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica."))
                .andExpect(jsonPath("$.path").value("/api/asignaturas/" + savedSubject.getId()));

        assertTrue(subjectRepository.existsById(savedSubject.getId()), "Subject must not be deleted when referenced by practice assignments");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearAsignaturaConIdIgnoradoYGenerado() throws Exception {
        Subject existing = new Subject();
        existing.setName("Materia Original Existente");
        existing.setCycle(PracticeCycle.INICIAL);
        Subject savedExisting = subjectRepository.save(existing);
        Long originalId = savedExisting.getId();

        String jsonWithExistingId = String.format("{\"id\":%d,\"name\":\"Materia Nueva Separada\",\"cycle\":\"AVANZADO\"}", originalId);
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithExistingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(is(originalId.intValue()))))
                .andExpect(jsonPath("$.name", is("Materia Nueva Separada")))
                .andExpect(jsonPath("$.cycle", is("AVANZADO")));

        Subject refetchedOriginal = subjectRepository.findById(originalId).orElseThrow();
        assertEquals("Materia Original Existente", refetchedOriginal.getName());
    }

    // ==========================================
    // DETERMINISTIC TRANSACTIONAL TESTS (TS-04)
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearMultiplesAsignaturasTransaccional() throws Exception {
        int count = 5;
        long initialCount = subjectRepository.count();

        for (int i = 0; i < count; i++) {
            String json = String.format("{\"name\":\"Asignatura Secuencial %d\",\"cycle\":\"INICIAL\",\"code\":\"SEC-%03d\"}", i, i);
            mockMvc.perform(post("/api/asignaturas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is(String.format("Asignatura Secuencial %d", i))))
                    .andExpect(jsonPath("$.code", is(String.format("SEC-%03d", i))));
        }

        assertEquals(initialCount + count, subjectRepository.count(), "All sequential transactional subjects must be persisted");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testAislamientoTransaccionalConRollbackEnFallo() throws Exception {
        long initialCount = subjectRepository.count();

        // 1. Successful creation
        String validJson = "{\"name\":\"Asignatura Valida Transaccion\",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
                .andExpect(status().isOk());

        assertEquals(initialCount + 1, subjectRepository.count());

        // 2. Failed creation must not alter persisted state beyond the failed request
        String invalidJson = "{\"name\":\"   \",\"cycle\":\"INICIAL\"}";
        mockMvc.perform(post("/api/asignaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertEquals(initialCount + 1, subjectRepository.count());
    }
}
```

### 4.5. `AuthControllerTest.java` Implementation Design
Replace contents of `src/test/java/com/ubb/practicas/controller/AuthControllerTest.java`:

```java
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
```

### 4.6. `UserControllerTest.java` Implementation Design
Create file `src/test/java/com/ubb/practicas/controller/UserControllerTest.java`:

```java
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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
```

### 4.7. RBAC Protection on `SubjectController`
To guarantee that the tests for TS-03 pass deterministically, the worker should ensure `SubjectController` enforces RBAC:
In `SubjectController.java`:
```java
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador')")
    public ResponseEntity<Subject> crearAsignatura(@RequestBody Subject subject) {
        Subject created = subjectService.crearAsignatura(subject);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR', 'Administrador')")
    public ResponseEntity<Map<String, String>> eliminarAsignatura(@PathVariable Long id) {
        subjectService.eliminarAsignatura(id);
        return ResponseEntity.ok(Map.of("message", "Asignatura eliminada exitosamente"));
    }
```
Or alternatively in `SecurityConfig.java`:
```java
.requestMatchers(HttpMethod.POST, "/api/asignaturas/**").hasAnyRole("ADMINISTRADOR", "Administrador")
.requestMatchers(HttpMethod.DELETE, "/api/asignaturas/**").hasAnyRole("ADMINISTRADOR", "Administrador")
.requestMatchers(HttpMethod.GET, "/api/asignaturas/**").authenticated()
```

---

## 5. Verification Method

To independently verify all changes when implemented by the worker:

1. **Verify Dependency Resolution & Test Compilation:**
   Execute from project root:
   ```powershell
   .\mvnw.cmd test-compile
   ```
   *Expected Result:* BUILD SUCCESS, zero syntax errors, dependencies `h2` and `spring-security-test` resolved.

2. **Execute Full Test Suite in Hermetic Isolation:**
   Execute:
   ```powershell
   .\mvnw.cmd test
   ```
   *Expected Result:*
   - All tests in `PracticasApplicationTests`, `SubjectControllerTest`, `AuthControllerTest`, and `UserControllerTest` run against in-memory H2 database (`jdbc:h2:mem:testdb`).
   - Zero connections attempted to MySQL (`localhost:3306`).
   - Zero flaky timeouts or `CountDownLatch` failures.
   - All security assertions (`@WithMockUser` and unauthenticated) pass with expected HTTP codes (`200`, `401`, `403`).
   - All error assertions strictly validate RFC 7807 `ApiErrorResponse` JSONPath fields.

3. **Verify File Layout Compliance:**
   - Source code under `src/test/java/com/ubb/practicas/controller/` and `src/test/resources/`.
   - `.agents/` folder contains only investigation reports, briefings, and progress logs.
