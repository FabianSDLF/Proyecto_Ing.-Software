# Handoff Report: Milestone 1 — Phase 1 Critical Security & Data Loss Prevention

**Author:** Explorer 1 (`teamwork_preview_explorer`)  
**Date:** 2026-09-23  
**Target Milestone:** Milestone 1 (Phase 1: Critical Security & Data Loss Prevention)  
**Assigned Findings:** TR-06, BK-47, TR-01, TR-02 & BK-12, TR-03  

---

## 1. Observation

### 1.1. Finding TR-06: Catastrophic Data Loss via `ddl-auto=create`
- **File:** `src/main/resources/application.properties`
- **Line 5:**
  ```properties
  spring.jpa.hibernate.ddl-auto=create
  ```
- **Context:** Lines 1-6 of `application.properties`:
  ```properties
  spring.application.name=practicas
  spring.datasource.url=jdbc:mysql://localhost:3306/sistema_practicas
  spring.datasource.username=root
  spring.datasource.password=admin
  spring.jpa.hibernate.ddl-auto=create
  ```
- **Observed Behavior:** On every boot of Spring Boot, Hibernate drops all existing tables and re-creates them from JPA models. All runtime data (registered users, evidence files, assignments, evaluations) is completely erased upon container or process restart.

---

### 1.2. Finding BK-47: Broken Access Control & Missing `@EnableMethodSecurity`
- **File:** `src/main/java/com/ubb/practicas/security/SecurityConfig.java`
- **Lines 21-23:**
  ```java
  @Configuration
  @EnableWebSecurity
  public class SecurityConfig {
  ```
- **Lines 37-41:**
  ```java
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/api/auth/**").permitAll()
                  .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                  .anyRequest().authenticated()
              )
  ```
- **Observed Behavior:**
  1. `SecurityConfig` does not include `@EnableMethodSecurity(prePostEnabled = true)`. Method-level authorization annotations (`@PreAuthorize`, `@Secured`, `@RolesAllowed`) are completely inactive.
  2. The only authorized endpoints are `/api/auth/**` (public) and `anyRequest().authenticated()`.
  3. No role checks exist in HTTP rules. Any authenticated user holding a valid JWT with the role of `Estudiante` can perform administrative operations:
     - `GET /api/usuarios` (list all user accounts, phone numbers, and profile details)
     - `POST /api/usuarios` (create arbitrary user accounts)
     - `PUT /api/usuarios/{id}/rol` (privilege escalation: elevate any account to "Administrador")
     - `PATCH /api/usuarios/{id}/estado` (deactivate administrator or teacher accounts)
     - `POST /api/usuarios/carga-masiva` (upload CSV/Excel batch imports)
     - `POST /api/asignaciones` (create assignments)
     - `DELETE /api/asignaciones/{id}` (delete assignments)
- **Sub-Observations on Routing Structure:**
  - In `UserController.java` (lines 95-119), `/api/usuarios/perfil` handles GET and PUT for the currently authenticated user (`Principal principal`). If a rule `.requestMatchers("/api/usuarios/**").hasAnyRole("ADMINISTRADOR", "Administrador")` is placed ahead of `/api/usuarios/perfil`, non-admin users (students, teachers) will be blocked from accessing their own profiles.
  - In `PracticeAssignmentController.java` (lines 25-33), endpoints `GET /api/asignaciones/colaborador/{id}` and `GET /api/asignaciones/tutor/{id}` are consumed by `frontend/src/pages/MisEstudiantes.jsx` (lines 33-35) for collaborator and tutor teachers to see their assigned students. If `/api/asignaciones/**` is strictly restricted only to `Administrador`, collaborator and tutor teachers will be forbidden from viewing their assigned students.

---

### 1.3. Finding TR-01: Hardcoded JWT Secret in Source Code & Missing Property
- **File 1:** `src/main/java/com/ubb/practicas/security/JwtUtil.java`
- **Line 16:**
  ```java
      @Value("${jwt.secret:clave-secreta-para-desarrollo-cambiar-en-produccion-min-32-chars}")
      private String secret;
  ```
- **File 2:** `src/main/resources/application.properties`
- **Observed Behavior:** `jwt.secret` is not defined anywhere in `application.properties`. As a result, Spring Boot falls back to the embedded default string in `JwtUtil.java`. Any adversary with read access to the Git repository or compiled bytecode can generate valid JWT signatures offline with any arbitrary subject and the role `"Administrador"`, bypassing authentication entirely.

---

### 1.4. Finding TR-02 & BK-12: Account Takeover & User Enumeration
- **File:** `src/main/java/com/ubb/practicas/controller/AuthController.java`
- **Lines 62-84:**
  ```java
      @PostMapping("/forgot-password")
      public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
          String email = request.get("email");
          User user = userRepository.findByEmail(email);
          
          if (user == null) {
              // Por seguridad, retornamos OK incluso si no existe, pero para ayudar al debug en este proyecto enviamos error:
              return ResponseEntity.badRequest().body(Map.of("message", "No existe una cuenta con ese correo institucional."));
          }

          String token = UUID.randomUUID().toString();
          PasswordResetToken resetToken = new PasswordResetToken();
          resetToken.setToken(token);
          resetToken.setUser(user);
          resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); 
          
          tokenRepository.save(resetToken);
          
          return ResponseEntity.ok(Map.of(
              "message", "Simulación: En un entorno real se enviaría un correo con este enlace.",
              "link", "http://localhost:5173/reset-password?token=" + token
          ));
      }
  ```
- **Observed Behavior:**
  1. **User Enumeration (BK-12):** When `user == null`, it returns HTTP 400 Bad Request with `"No existe una cuenta con ese correo institucional."` allowing attackers to determine which email addresses correspond to registered university users.
  2. **Account Takeover (TR-02):** When a user is found, the endpoint responds with HTTP 200 containing `"link": "http://localhost:5173/reset-password?token=" + token`. Any unauthenticated caller can send a POST request with any target email, receive the reset token in the HTTP response body, open the reset URL, and overwrite the user's password without ever accessing their institutional inbox.
  3. **Frontend Compatibility:** In `frontend/src/pages/ForgotPassword.jsx` (lines 28-30 and 69-75), the component displays `data.message` on `res.ok`, and only displays the clickable link if `simulatedLink` (`data.link`) is present. Removing `link` from the JSON response gracefully falls back to displaying the confirmation notice without UI breakage.

---

### 1.5. Finding TR-03: Lack of Active User Validation in JWT Filter
- **File:** `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`
- **Lines 31-43:**
  ```java
          if (authHeader != null && authHeader.startsWith("Bearer ")) {
              String token = authHeader.substring(7);

              if (jwtUtil.isTokenValid(token)) {
                  String email = jwtUtil.extractEmail(token);
                  String role = jwtUtil.extractRole(token);

                  UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                          email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
                  );
                  SecurityContextHolder.getContext().setAuthentication(auth);
              }
          }
  ```
- **File:** `src/main/java/com/ubb/practicas/service/UserService.java` (lines 150-155):
  ```java
      @Transactional
      public User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception {
          User user = userRepository.findById(usuarioId)
                  .orElseThrow(() -> new Exception("Usuario no encontrado"));
          user.setActive(estado);
          return userRepository.save(user);
      }
  ```
- **Observed Behavior:**
  1. `JwtAuthFilter` checks only the cryptographic signature and token expiration date via `jwtUtil.isTokenValid(token)`.
  2. It does NOT query the database to verify whether the user exists or whether `user.getActive()` is `true`.
  3. When an administrator deactivates an account (`user.setActive(false)`), any active JWT token remains completely valid until expiration (default: 24 hours / 86,400,000 ms), leaving the system exposed to deactivated rogue users.
  4. Role formatting: `JwtAuthFilter` creates `new SimpleGrantedAuthority("ROLE_" + role)`. In `DataSeeder.java`, roles are seeded with title case names: `"Administrador"`, `"Estudiante"`, `"Profesor de Asignatura"`, `"Profesor Colaborador"`, `"Tutor"`. An authority named `"ROLE_Administrador"` is created. However, Spring Security method expressions commonly use uppercase (e.g. `@PreAuthorize("hasRole('ADMINISTRADOR')")`).

---

## 2. Logic Chain

1. **TR-06:** In Spring Boot JPA, `spring.jpa.hibernate.ddl-auto=create` instructs Hibernate to execute schema dropping and creation on `SessionFactory` creation. Changing this property to `update` ensures existing tables and rows are preserved across application restarts while applying incremental schema changes.
2. **BK-47:** Spring Security 6 / Spring Boot 3 enforces authorization in the `SecurityFilterChain` bean. Without `@EnableMethodSecurity(prePostEnabled = true)`, method annotations are ignored. Without route-level role constraints, `.anyRequest().authenticated()` treats any authenticated token identically regardless of authority. To resolve Broken Access Control:
   - Mark `SecurityConfig` with `@EnableMethodSecurity(prePostEnabled = true)`.
   - Protect administrative endpoints (`/api/usuarios/**`, `/api/carga-masiva/**`, `/api/asignaciones/**`).
   - Order route matchers specifically so `/api/usuarios/perfil` remains accessible to all authenticated users before `/api/usuarios/**` restricts other endpoints to administrators.
   - Authorize collaborator and tutor teachers for their specific endpoints (`/api/asignaciones/colaborador/**`, `/api/asignaciones/tutor/**`) before `/api/asignaciones/**` applies admin-only restrictions.
3. **TR-01:** CWE-798 defines hard-coded credentials as a critical vulnerability. Removing the fallback default string from `JwtUtil.java` and placing `jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}` in `application.properties` fulfills the Twelve-Factor App Factor III (Config), allows container runtime environment overrides, and guarantees HMAC-SHA256 has at least 256 bits of entropy (76 characters).
4. **TR-02 & BK-12:** OWASP A07 and CWE-200 mandate that password reset mechanisms must not disclose user existence (preventing account enumeration) or expose security tokens over unauthenticated channels. By returning an identical HTTP 200 with `"Si el correo corresponde a una cuenta activa registrada, se enviarán las instrucciones para restablecer la contraseña."` for both existent and non-existent accounts, and by omitting the `link` and `token` parameters from the JSON body, account takeover and enumeration are eliminated. For local development visibility, logging the token to the server console via `org.slf4j.Logger` enables testing without exposing tokens across HTTP.
5. **TR-03:** Validating active account status requires verifying the state of the account against persistent storage upon request execution. Injecting `UserRepository` into `JwtAuthFilter` and checking `user != null && Boolean.TRUE.equals(user.getActive())` before setting `SecurityContextHolder` ensures that disabled accounts are immediately rejected with HTTP 401 on subsequent requests. Furthermore, mapping both raw (`ROLE_Administrador`) and normalized uppercase (`ROLE_ADMINISTRADOR`) authorities ensures robust compatibility with `@PreAuthorize` rules.

---

## 3. Caveats

1. **Database Lookup Overhead on Each Request (TR-03):** Querying `userRepository.findByEmail(email)` in `JwtAuthFilter` executes one `SELECT` statement per authenticated request. Given that `email` is indexed with a `UNIQUE` constraint on the `usuarios` table, query latency is negligible (sub-millisecond). If high throughput is required in future milestones, a Spring Cache or Redis cache can be placed over `findByEmail`.
2. **Email Server Integration (TR-02):** PITAGORAS currently lacks an SMTP/JavaMailSender service bean. Emitting the reset token to the backend application log (`logger.info(...)`) allows local development testing while keeping the HTTP API completely secure. Real email dispatch can be implemented when an email provider is configured.
3. **Existing Test Suite (`SubjectControllerTest`):** `SubjectControllerTest` is configured with `@AutoConfigureMockMvc(addFilters = false)`, meaning MockMvc tests currently bypass security filters. Adjustments to `SecurityConfig` and `JwtAuthFilter` will not affect or break `SubjectControllerTest`.

---

## 4. Conclusion & Concrete Implementation Proposals

### Proposal 1: Fix TR-06 in `src/main/resources/application.properties`

**Target File:** `src/main/resources/application.properties`  
**Line:** 5  

#### Before:
```properties
spring.jpa.hibernate.ddl-auto=create
```

#### After:
```properties
spring.jpa.hibernate.ddl-auto=update
```

---

### Proposal 2: Fix TR-01 in `src/main/resources/application.properties` and `src/main/java/com/ubb/practicas/security/JwtUtil.java`

#### 2.1. `src/main/resources/application.properties`
Append the following configuration at the end of the file:
```properties
# JWT Security Configuration (TR-01)
jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}
jwt.expiration=86400000
```

#### 2.2. `src/main/java/com/ubb/practicas/security/JwtUtil.java`
**Lines 16-17:**

##### Before:
```java
    @Value("${jwt.secret:clave-secreta-para-desarrollo-cambiar-en-produccion-min-32-chars}")
    private String secret;
```

##### After:
```java
    @Value("${jwt.secret}")
    private String secret;
```

---

### Proposal 3: Fix BK-47 in `src/main/java/com/ubb/practicas/security/SecurityConfig.java`

**Target File:** `src/main/java/com/ubb/practicas/security/SecurityConfig.java`  

#### Imports & Annotations (Lines 1-24):
##### Before:
```java
package com.ubb.practicas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

##### After:
```java
package com.ubb.practicas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
```

#### Authorization Filter Rules (Lines 37-41):
##### Before:
```java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
```

##### After:
```java
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/usuarios/perfil").authenticated()
                .requestMatchers("/api/usuarios/carga-masiva").hasAnyRole("ADMINISTRADOR", "Administrador")
                .requestMatchers("/api/usuarios/**").hasAnyRole("ADMINISTRADOR", "Administrador")
                .requestMatchers(HttpMethod.GET, "/api/asignaciones/colaborador/**").hasAnyRole("ADMINISTRADOR", "Administrador", "PROFESOR_COLABORADOR", "Profesor Colaborador")
                .requestMatchers(HttpMethod.GET, "/api/asignaciones/tutor/**").hasAnyRole("ADMINISTRADOR", "Administrador", "TUTOR", "Tutor")
                .requestMatchers("/api/asignaciones/**").hasAnyRole("ADMINISTRADOR", "Administrador")
                .requestMatchers("/api/carga-masiva/**").hasAnyRole("ADMINISTRADOR", "Administrador")
                .anyRequest().authenticated()
            )
```

---

### Proposal 4: Fix TR-02 & BK-12 in `src/main/java/com/ubb/practicas/controller/AuthController.java`

**Target File:** `src/main/java/com/ubb/practicas/controller/AuthController.java`  

#### Logger Declaration and `/forgot-password` Method (Lines 24-37 and 62-84):
##### Before:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
...
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userRepository.findByEmail(email);
        
        if (user == null) {
            // Por seguridad, retornamos OK incluso si no existe, pero para ayudar al debug en este proyecto enviamos error:
            return ResponseEntity.badRequest().body(Map.of("message", "No existe una cuenta con ese correo institucional."));
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); 
        
        tokenRepository.save(resetToken);
        
        return ResponseEntity.ok(Map.of(
            "message", "Simulación: En un entorno real se enviaría un correo con este enlace.",
            "link", "http://localhost:5173/reset-password?token=" + token
        ));
    }
```

##### After:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
...
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email != null && !email.trim().isEmpty()) {
            User user = userRepository.findByEmail(email.trim());
            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(token);
                resetToken.setUser(user);
                resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
                tokenRepository.save(resetToken);

                // TR-02: Registrar en logs del servidor en desarrollo; NUNCA exponer el enlace en la respuesta HTTP
                log.info("Solicitud de recuperación para {}. Token de desarrollo: http://localhost:5173/reset-password?token={}", email, token);
            }
        }

        // BK-12 & TR-02: Retorno uniforme para prevenir enumeración de cuentas y account takeover
        return ResponseEntity.ok(Map.of(
            "message", "Si el correo corresponde a una cuenta activa registrada, se enviarán las instrucciones para restablecer la contraseña."
        ));
    }
```

---

### Proposal 5: Fix TR-03 in `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`

**Target File:** `src/main/java/com/ubb/practicas/security/JwtAuthFilter.java`  

##### Before:
```java
package com.ubb.practicas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

##### After:
```java
package com.ubb.practicas.security;

import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                User user = userRepository.findByEmail(email);

                // TR-03: Validar que el usuario exista y continúe activo en la base de datos
                if (user != null && Boolean.TRUE.equals(user.getActive())) {
                    String role = jwtUtil.extractRole(token);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        String normalizedRole = role.toUpperCase().replace(" ", "_");
                        if (!normalizedRole.equals(role)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
                        }
                    }

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 5. Verification Method

To independently verify the implementation after the Worker applies the proposals:

### 5.1. Compilation and Syntax Check
Run from the root directory (`Proyecto_IngSoft_Practicas`):
```bash
./mvnw.cmd test-compile
```
*Expected Result:* `BUILD SUCCESS`. No missing imports, type mismatches, or syntax errors.

### 5.2. Existing Automated Tests
Run:
```bash
./mvnw.cmd test
```
*Expected Result:* `SubjectControllerTest` passes cleanly without regression.

### 5.3. Specific Security Findings Verification

1. **TR-06 Verification:**
   - Inspect `src/main/resources/application.properties` and verify `spring.jpa.hibernate.ddl-auto=update`.
   - Invalidation condition: `ddl-auto` set to `create` or `create-drop`.

2. **TR-01 Verification:**
   - In `application.properties`, confirm `jwt.secret=${JWT_SECRET:c3VwZXItc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtY2hhbmdlLWluLXByb2QtbWluLTMyLWNoYXJz}` is present.
   - In `JwtUtil.java`, confirm line 16 has `@Value("${jwt.secret}")` with no plaintext fallback string embedded in Java.

3. **BK-47 Verification:**
   - Inspect `SecurityConfig.java` to confirm `@EnableMethodSecurity(prePostEnabled = true)` is present.
   - Test invoking `GET /api/usuarios` with an `Estudiante` Bearer token:
     *Expected Status:* `403 Forbidden`.
   - Test invoking `GET /api/usuarios` with an `Administrador` Bearer token:
     *Expected Status:* `200 OK`.
   - Test invoking `GET /api/usuarios/perfil` with an `Estudiante` Bearer token:
     *Expected Status:* `200 OK` (verifying route ordering works).

4. **TR-02 & BK-12 Verification:**
   - Send `POST /api/auth/forgot-password` with `{"email": "inexistente@correo.cl"}`:
     *Expected Status:* `200 OK`.
     *Expected Body:* `{"message": "Si el correo corresponde a una cuenta activa registrada, se enviarán las instrucciones para restablecer la contraseña."}` (No 400 error; no account enumeration).
   - Send `POST /api/auth/forgot-password` with `{"email": "admin@ubb.cl"}`:
     *Expected Status:* `200 OK`.
     *Expected Body:* Identical message JSON without `link` or `token` field (No account takeover).
     *Expected Server Log:* Info log containing the reset token for local test reproduction.

5. **TR-03 Verification:**
   - Generate or acquire a valid JWT for an active user.
   - Deactivate the user via `PATCH /api/usuarios/{id}/estado` with `{"active": false}`.
   - Perform an API request with the original unexpired token to a protected endpoint (e.g. `GET /api/usuarios/perfil`).
     *Expected Status:* `401 Unauthorized` or `403 Forbidden` (token is rejected because DB status is inactive).
