# Informe de Handoff — Fase 3: Blueprint de Manejo Global de Excepciones y Clean Code en Backend

**Agente:** `explorer_m3_err_1`  
**Fecha de Emisión:** 2026-09-24T01:00:00Z  
**Tipo de Handoff:** Hard Handoff (Análisis completo, reproducible y accionable)  
**Rama Git Verificada:** `fabian` (Commit actual: `f618de60b19ac6f4001279960132c71435e5b405`)  
**Directorio de Trabajo:** `c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_err_1`

---

## 1. Observation

### 1.1. Verificación del Repositorio y Rama Git
- **Archivo inspeccionado:** `.git/HEAD`
  ```
  ref: refs/heads/fabian
  ```
- **Archivo de log de Git inspeccionado:** `.git/logs/refs/heads/fabian`
  ```
  2285bc93d35ddb42aef019e5ccbe58c804bc07b4 f618de60b19ac6f4001279960132c71435e5b405 Fabián Sepúlveda <fabian.sepulveda2301@alumnos.ubiobio.cl> 1790199812 -0300 commit: feat(arch): implement phase 2 layered architecture and frontend services
  ```
- **Estado verificado:** El repositorio se encuentra en la rama `fabian`, con los avances de la Fase 1 (Seguridad P0 / Prevención de Pérdida de Datos) y Fase 2 (Arquitectura en Capas y Servicios Frontend) debidamente consolidados.

---

### 1.2. Auditoría Exhaustiva de Manejo de Errores en Controladores REST

Se auditaron los 8 controladores ubicados en `src/main/java/com/ubb/practicas/controller/`:

#### 1. `AuthController.java` (`/api/auth`)
* **Líneas 23-32 (`login`)**:
  ```java
  try {
      AuthResponse response = authService.login(request);
      return ResponseEntity.ok(response);
  } catch (DisabledException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cuenta desactivada");
  } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
  }
  ```
  *Defecto:* Retorna cadenas de texto plano desestructuradas (`"Cuenta desactivada"`, `"Credenciales incorrectas"`).
* **Líneas 40-50 (`resetPassword`)**:
  ```java
  try {
      return ResponseEntity.ok(authService.resetPassword(tokenStr, newPassword));
  } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
  }
  ```
  *Defecto:* Asimetría de contrato. `login` retorna `String` crudo, mientras `resetPassword` retorna `Map.of("message", ...)`.

---

#### 2. `PortfolioController.java` (`/api/portafolio`)
* **Líneas 32, 39, 46, 68, 89, 110, 126**:
  ```java
  if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  ```
  *Defecto:* Verificación manual de autenticación repetida 7 veces consecutivas con cuerpo vacío sin mensaje ni código de error estándar.
* **Líneas 48-60 (`descargarDocumento`)**:
  ```java
  try {
      DownloadedDocument downloaded = portfolioService.descargarDocumento(id, principal.getName());
      ...
  } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
  } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
  }
  ```
  *Defecto:* Captura indiscriminada de `RuntimeException` enmascarando caídas de disco duro o IO (`StorageException`) como un HTTP 404 vacío.
* **Líneas 70-75 (`subirEvidencia`)**:
  ```java
  try {
      PortfolioDocument doc = portfolioService.subirEvidencia(principal.getName(), assignmentId, file);
      return ResponseEntity.ok(doc);
  } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error al subir archivo: " + e.getMessage());
  }
  ```
  *Defecto:* Bloque `catch (Exception e)` genérico que devuelve texto plano con prefijo manual.
* **Líneas 91-105 (`crearPlanificacion`)**:
  ```java
  try {
      LessonPlan plan = lessonPlanService.crearPlanificacion(...);
      return ResponseEntity.ok(plan);
  } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error al crear planificación: " + e.getMessage());
  }
  ```
  *Defecto:* `catch (Exception e)` genérico devolviendo texto plano.
* **Líneas 112-121 (`eliminarPlanificacion`) y 128-137 (`eliminarDocumento`)**:
  ```java
  try {
      lessonPlanService.eliminarPlanificacion(id, principal.getName());
      return ResponseEntity.ok("Planificación eliminada");
  } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
  } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error al eliminar planificación: " + e.getMessage());
  }
  ```
  *Defecto:* Proliferación de 3 ramas `catch` por método, con retornos HTTP vacíos en 404/403 y texto crudo en 400. En éxito retorna `String` ("Planificación eliminada").

---

#### 3. `PracticeAssignmentController.java` (`/api/asignaciones`)
* **Líneas 36-41 (`crearAsignacion`)**:
  ```java
  try {
      PracticeAssignment created = assignmentService.crearAsignacion(assignment);
      return ResponseEntity.ok(created);
  } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
  }
  ```
  *Defecto:* Retorno de cadena de texto plano cruda en HTTP 400.
* **Líneas 46-51 (`eliminarAsignacion`)**:
  ```java
  try {
      assignmentService.eliminarAsignacion(id);
      return ResponseEntity.ok().build();
  } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
  }
  ```
  *Defecto:* `catch (NoSuchElementException)` que retorna respuesta vacía `404 Not Found`.

---

#### 4. `PracticeCenterController.java` (`/api/centros`)
* **Líneas 18-20 (`listarCentros`)**: No posee bloques `try-catch`. Carece de endpoints de mutación y validación de entidades (BK-23).

---

#### 5. `ProfessorController.java` (`/api/profesor`)
* **Líneas 23-40 (`obtenerEstudiantesPorProfesor`)**:
  ```java
  if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
  try {
      List<PracticeAssignment> assignments = assignmentService.obtenerEstudiantesPorProfesor(professorId, principal.getName());
      return ResponseEntity.ok(assignments);
  } catch (NoSuchElementException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
  } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
  }
  ```
  *Defecto:* Captura manual de 4 tipos distintos de excepción dentro del método HTTP. Nótese que `IllegalArgumentException` se mapea anómalamente a HTTP 403 Forbidden para forzar el rechazo de roles no docentes.

---

#### 6. `RoleController.java` (`/api/roles`)
* **Líneas 20-22 (`obtenerRoles`)**: Delegación limpia a `roleService.obtenerRoles()`. No requiere `try-catch`.

---

#### 7. `SubjectController.java` (`/api/asignaturas`)
* **Líneas 26-33 (`crearAsignatura`)**:
  ```java
  try {
      Subject created = subjectService.crearAsignatura(subject);
      return ResponseEntity.ok(created);
  } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
  }
  ```
  *Defecto:* Retorna `e.getMessage()` como texto plano.
* **Líneas 36-45 (`eliminarAsignatura`)**:
  ```java
  try {
      subjectService.eliminarAsignatura(id);
      return ResponseEntity.ok().build();
  } catch (NoSuchElementException e) {
      return ResponseEntity.notFound().build();
  } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
  }
  ```
  *Defecto:* Retorno de 404 vacío y 409 con texto plano.

---

#### 8. `UserController.java` (`/api/usuarios`)
Este controlador concentra la mayor cantidad de antipatrones de manejo de errores del sistema:
* **Líneas 34-40 (`crearUsuario`)**:
  ```java
  try {
      User created = userService.crearUsuario(nuevoUsuario);
      return ResponseEntity.ok(created);
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  }
  ```
* **Líneas 44-52 (`cargaMasivaUsuarios`)**:
  ```java
  try {
      userService.procesarArchivoMasivo(archivo);
      return ResponseEntity.ok("Usuarios cargados exitosamente de forma masiva.");
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("Error al procesar el archivo: " + e.getMessage());
  }
  ```
* **Líneas 55-65 (`actualizarRol`)**:
  ```java
  try {
      String roleName = payload.get("roleName");
      if (roleName == null || roleName.isEmpty()) {
          return ResponseEntity.badRequest().body("El nombre del rol es requerido");
      }
      User updatedUser = userService.actualizarRolUsuario(id, roleName);
      return ResponseEntity.ok(updatedUser);
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }
  ```
  *Falla crítica detectada:* Cualquier excepción (e.g. error de SQL, rol inválido, caída de red) es capturada ciegamente y transformada en `404 Not Found`.
* **Líneas 69-75 (`actualizarUsuario`)**:
  ```java
  try {
      User updatedUser = userService.actualizarUsuario(id, datosActualizados);
      return ResponseEntity.ok(updatedUser);
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }
  ```
  *Falla crítica detectada:* Si el correo ya está en uso (`El email ya está en uso`), el método devuelve `404 Not Found` en lugar de `409 Conflict` o `400 Bad Request`.
* **Líneas 79-89 (`cambiarEstado`)**:
  ```java
  try {
      ...
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }
  ```
* **Líneas 102-115 (`actualizarMiPerfil`)**:
  ```java
  try {
      User updatedUser = userService.actualizarMiPerfil(principal.getName(), request);
      return ResponseEntity.ok(updatedUser);
  } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  }
  ```

---

### 1.3. Auditoría de la Capa de Servicios
- **Archivo:** `src/main/java/com/ubb/practicas/service/UserService.java`
  Líneas 16-21:
  ```java
  void procesarArchivoMasivo(MultipartFile archivo) throws Exception;
  User actualizarRolUsuario(Long usuarioId, String roleName) throws Exception;
  User actualizarUsuario(Long usuarioId, User datosActualizados) throws Exception;
  User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception;
  User crearUsuario(User nuevoUsuario) throws Exception;
  User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request) throws Exception;
  ```
  *Defecto (BK-33):* Los métodos de la interfaz declaran `throws Exception`, forzando a los llamadores a usar bloques `try-catch` genéricos y ocultando la naturaleza del error de negocio.
- **Archivo:** `src/main/java/com/ubb/practicas/service/impl/UserServiceImpl.java`
  Líneas 56, 60, 78, 104, 110, 132, 142:
  ```java
  throw new Exception("Usuario no encontrado");
  throw new Exception("Rol no encontrado: " + roleName);
  throw new Exception("El email ya está en uso");
  ```
  *Defecto:* Emisión directa de `java.lang.Exception` crudo con mensajes literales en español en lugar de excepciones de dominio no chequeadas.
- **Archivo:** `src/main/java/com/ubb/practicas/service/impl/PracticeAssignmentServiceImpl.java`
  Líneas 131-133:
  ```java
  Optional<TeachingRole> teachingRole = TeachingRole.fromRoleName(targetProfessor.getRole().getName());
  if (teachingRole.isEmpty()) {
      throw new IllegalArgumentException("El usuario no tiene un rol docente válido");
  }
  ```

---

### 1.4. Auditoría de Pruebas Automatizadas Existentes
- **`SubjectControllerTest.java`**:
  - Línea 135: `.andExpect(content().string(containsString("El nombre de la asignatura es obligatorio")))`
  - Línea 146: `.andExpect(content().string(containsString("El nombre de la asignatura es obligatorio")))`
  - Línea 157: `.andExpect(content().string(containsString("El ciclo de la asignatura es obligatorio")))`
  - Línea 169: `.andExpect(content().string(containsString("no puede superar los 255 caracteres")))`
  - Línea 216-217: `.andExpect(status().isConflict()).andExpect(content().string(containsString("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica.")))`
- **`ProfessorControllerTest.java`**:
  - Línea 124: `.andExpect(status().isForbidden()).andExpect(content().string(containsString("no tiene permisos para consultar los estudiantes")))`
  - Línea 138-139: `.andExpect(status().isForbidden()).andExpect(content().string(containsString("El usuario no tiene un rol docente válido")))`
- **`PortfolioControllerTest.java`**:
  - Líneas 144, 151: `.andExpect(status().isForbidden())`

---

### 1.5. Auditoría del Consumo Frontend (`apiClient.js`)
- **Archivo:** `frontend/src/services/apiClient.js`
  Líneas 77-98:
  ```javascript
  if (!response.ok) {
    let errorData = null;
    let errorMessage = `Error HTTP ${response.status}: ${response.statusText}`;
    const contentType = response.headers.get('content-type') || '';

    try {
      if (contentType.includes('application/json')) {
        errorData = await response.json();
        errorMessage = errorData.message || errorData.error || errorMessage;
      } else {
        const text = await response.text();
        if (text) {
          errorMessage = text;
          errorData = text;
        }
      }
    } catch (_) {}

    throw new ApiError(errorMessage, response.status, errorData);
  }
  ```
  *Observación crítica:* El cliente HTTP del frontend ya espera explícitamente un objeto JSON con los atributos `message` y `error`. Cuando el backend devuelve texto plano o cuerpos vacíos, `apiClient` pierde la estructura y no puede acceder a mapas de errores de validación.

---

## 2. Logic Chain

1. **Premisa 1 (Violación de SRP y DRY — BK-26):**  
   Los controladores REST no deben responsabilizarse de la captura manual de excepciones ni de la traducción artesanal a respuestas HTTP de error. La presencia de más de 20 bloques `try-catch` dispersos en 8 controladores duplica código, genera inconsistencia en los contratos de la API y distrae al controlador de su única responsabilidad: coordinar la capa de transporte HTTP.
2. **Premisa 2 (Falla de Integridad Semántica — BK-33):**  
   Declarar `throws Exception` en `UserService` y lanzar `new Exception(...)` en `UserServiceImpl` destruye la capacidad de discriminar entre fallas de negocio (usuario no encontrado, correo en uso) y fallas técnicas. Esto indujo al desarrollador original a capturar `Exception` en `UserController` y mapear ciegamente todo error a HTTP 404, ocultando errores reales del sistema.
3. **Premisa 3 (Compatibilidad con Frontend `apiClient.js`):**  
   El cliente web implementado en la Fase 2 ya está programado para deserializar JSON con los campos `message` y `error`. Unificar todas las respuestas de error bajo un DTO formal `ApiErrorResponse` con `timestamp`, `status`, `error`, `message`, `path` y `validationErrors` estandariza el contrato sin requerir modificaciones en la lógica del cliente.
4. **Premisa 4 (Compatibilidad con Pruebas Automatizadas — TS-05):**  
   Las pruebas unitarias existentes (`SubjectControllerTest`, `ProfessorControllerTest`) utilizan la aserción `content().string(containsString("..."))`. Al incluir el mensaje de la excepción dentro del atributo `"message"` de `ApiErrorResponse`, el cuerpo JSON serializado continuará conteniendo la subcadena esperada, asegurando que el 100% de las pruebas pasen sin regresión.
5. **Premisa 5 (Mapeo Semántico de Códigos HTTP):**  
   - Recursos inexistentes (`ResourceNotFoundException`, `UserNotFoundException`, `RoleNotFoundException`, `NoSuchElementException`) $\rightarrow$ **404 Not Found**.
   - Parámetros inválidos, validaciones de Bean Validation, formatos incorrectos (`BadRequestException`, `IllegalArgumentException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`) $\rightarrow$ **400 Bad Request**.
   - Credenciales inválidas o falta de token (`UnauthorizedException`, `BadCredentialsException`, `SecurityException`) $\rightarrow$ **401 Unauthorized**.
   - Cuentas inactivas, acceso BOLA/IDOR o roles no permitidos (`ForbiddenException`, `AccessDeniedException`, `DisabledException`) $\rightarrow$ **403 Forbidden**.
   - Conflictos de estado, llaves duplicadas o dependencias activas (`ResourceConflictException`, `EmailAlreadyInUseException`, `IllegalStateException`, `DataIntegrityViolationException`) $\rightarrow$ **409 Conflict**.
   - Fallas de almacenamiento o excepciones no controladas (`StorageException`, `Exception`) $\rightarrow$ **500 Internal Server Error** (sin fugar stacktraces al cliente).

---

## 3. Caveats

1. **Operación de Solo Lectura:** En estricto cumplimiento del mandato de agente explorer, no se modificó ninguna línea de código en los archivos fuente del proyecto.
2. **Excepción Docente en `PracticeAssignmentServiceImpl`:** Actualmente, `PracticeAssignmentServiceImpl` lanza `IllegalArgumentException("El usuario no tiene un rol docente válido")` y `ProfessorController` lo captura mapeándolo a HTTP 403 Forbidden para satisfacer `testConsultaUsuarioConRolNoDocenteRetornaMensajeValido`. En la refactorización propuesta, se introduce `ForbiddenException` para que dicha regla de negocio emita directamente una excepción semántica que `GlobalExceptionHandler` mapee naturalmente a 403 sin alterar `IllegalArgumentException` (que debe corresponder a 400).
3. **Spring Security Filter Chain:** `GlobalExceptionHandler` captura excepciones lanzadas dentro del ciclo de vida del DispatcherServlet de Spring MVC (controladores y servicios). Las excepciones de autenticación ocurridas en los filtros previos (`JwtAuthFilter`) deben ser respaldadas por un `AuthenticationEntryPoint` y `AccessDeniedHandler` configurados en `SecurityConfig` para devolver la misma estructura `ApiErrorResponse`.

---

## 4. Conclusion & Blueprints de Implementación

### 4.1. Diseño del DTO de Respuesta Estándar: `ApiErrorResponse`
**Ubicación de Destino:** `src/main/java/com/ubb/practicas/dto/ApiErrorResponse.java`

```java
package com.ubb.practicas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized API Error Response DTO.
 * Conforms to RFC 7807 and Spring Boot error conventions.
 * Resolves BK-26, BK-27, and provides structured contracts for frontend apiClient.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> validationErrors;

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    public static ApiErrorResponse ofValidation(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }
}
```

---

### 4.2. Jerarquía de Excepciones de Dominio Personalizadas

Todas las excepciones de dominio son no chequeadas (`RuntimeException`):

1. **`ResourceNotFoundException.java`** (`src/main/java/com/ubb/practicas/exception/ResourceNotFoundException.java`):
```java
package com.ubb.practicas.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
```

2. **`BadRequestException.java`** (`src/main/java/com/ubb/practicas/exception/BadRequestException.java`):
```java
package com.ubb.practicas.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

3. **`ResourceConflictException.java`** (`src/main/java/com/ubb/practicas/exception/ResourceConflictException.java`):
```java
package com.ubb.practicas.exception;

public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

4. **`UnauthorizedException.java`** (`src/main/java/com/ubb/practicas/exception/UnauthorizedException.java`):
```java
package com.ubb.practicas.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

5. **`ForbiddenException.java`** (`src/main/java/com/ubb/practicas/exception/ForbiddenException.java`):
```java
package com.ubb.practicas.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

6. **Alineación de Excepciones Preexistentes:**
   - `UserNotFoundException`: hereda de `ResourceNotFoundException`.
   - `RoleNotFoundException`: hereda de `ResourceNotFoundException`.
   - `StorageFileNotFoundException`: hereda de `ResourceNotFoundException`.
   - `EmailAlreadyInUseException`: hereda de `ResourceConflictException`.
   - `StorageException`: se mantiene como `RuntimeException` (HTTP 500).
   - `FileProcessingException` y `UnsupportedFileFormatException`: se mantienen como `RuntimeException` (HTTP 400).

---

### 4.3. Implementación de `GlobalExceptionHandler` (`@RestControllerAdvice`)
**Ubicación de Destino:** `src/main/java/com/ubb/practicas/exception/GlobalExceptionHandler.java`

```java
package com.ubb.practicas.exception;

import com.ubb.practicas.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Centralized REST API Exception Handler.
 * Intercepts all controller and service exceptions and produces RFC 7807 compliant ApiErrorResponse.
 * Resolves BK-26, BK-27, and provides uniform error translation across the platform.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // 404 NOT FOUND
    // ==========================================
    @ExceptionHandler({
            ResourceNotFoundException.class,
            UserNotFoundException.class,
            RoleNotFoundException.class,
            StorageFileNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "El recurso solicitado no fue encontrado.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ==========================================
    // 400 BAD REQUEST & VALIDATION
    // ==========================================
    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            FileProcessingException.class,
            UnsupportedFileFormatException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Solicitud inválida [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Datos de solicitud inválidos.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String summary = fieldErrors.isEmpty()
                ? "Error de validación en la entrada."
                : fieldErrors.values().iterator().next();

        log.warn("Error de validación de campos [{}]: {}", request.getRequestURI(), fieldErrors);
        ApiErrorResponse response = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                summary,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo de solicitud ilegible [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la solicitud no tiene un formato JSON válido o contiene valores incompatibles.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Parámetro requerido ausente [{}]: {}", request.getRequestURI(), ex.getParameterName());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El parámetro requerido '" + ex.getParameterName() + "' no fue proporcionado.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Tamaño de archivo excedido [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El archivo excede el tamaño máximo permitido para subida.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==========================================
    // 401 UNAUTHORIZED
    // ==========================================
    @ExceptionHandler({
            UnauthorizedException.class,
            BadCredentialsException.class,
            SecurityException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(Exception ex, HttpServletRequest request) {
        log.warn("Acceso no autenticado [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Credenciales inválidas o sesión no autorizada.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ==========================================
    // 403 FORBIDDEN
    // ==========================================
    @ExceptionHandler({
            AccessDeniedException.class,
            ForbiddenException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        log.warn("Acceso prohibido [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "No tiene permisos para acceder a este recurso.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==========================================
    // 409 CONFLICT
    // ==========================================
    @ExceptionHandler({
            ResourceConflictException.class,
            EmailAlreadyInUseException.class,
            IllegalStateException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de estado o integridad [{}]: {}", request.getRequestURI(), ex.getMessage());
        String message = ex.getMessage();
        if (ex instanceof DataIntegrityViolationException) {
            message = "Conflicto de integridad de datos en la base de datos.";
        }
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                message != null ? message : "Conflicto en la operación solicitada.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ==========================================
    // 500 INTERNAL SERVER ERROR
    // ==========================================
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(StorageException ex, HttpServletRequest request) {
        log.error("Falla en servicio de almacenamiento [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Error en el almacenamiento de archivos: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Excepción no controlada en servidor [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error interno inesperado en el servidor.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

### 4.4. Plan de Reemplazo Detallado en Capa de Servicios y Controladores

#### 1. Sanación de `UserService.java` y `UserServiceImpl.java` (BK-33)
* En `UserService.java`:
  Eliminar `throws Exception` de todas las firmas de método.
* En `UserServiceImpl.java`:
  - Línea 56: `.orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + usuarioId));`
  - Línea 60: `throw new RoleNotFoundException("Rol no encontrado: " + roleName);`
  - Línea 71: `.orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + usuarioId));`
  - Línea 78: `throw new EmailAlreadyInUseException("El email ya está en uso");`
  - Línea 95: `.orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + usuarioId));`
  - Líneas 104, 142: `throw new EmailAlreadyInUseException("El email ya está en uso");`
  - Línea 110: `throw new RoleNotFoundException("Rol no encontrado: " + nuevoUsuario.getRole().getName());`
  - Línea 132: `throw new UserNotFoundException("Usuario no encontrado con email: " + emailActual);`

#### 2. Sanación de `PracticeAssignmentServiceImpl.java`
* En `obtenerEstudiantesPorProfesor`:
  Reemplazar:
  ```java
  if (teachingRole.isEmpty()) {
      throw new IllegalArgumentException("El usuario no tiene un rol docente válido");
  }
  ```
  Por:
  ```java
  if (teachingRole.isEmpty()) {
      throw new ForbiddenException("El usuario no tiene un rol docente válido");
  }
  ```

#### 3. Reemplazos en `UserController.java`
Eliminar los 6 bloques `try-catch` y dejar el controlador limpio y declarativo:
```java
@PostMapping
public ResponseEntity<User> crearUsuario(@RequestBody User nuevoUsuario) {
    return ResponseEntity.ok(userService.crearUsuario(nuevoUsuario));
}

@PostMapping("/carga-masiva")
public ResponseEntity<Map<String, String>> cargaMasivaUsuarios(@RequestParam("archivo") MultipartFile archivo) {
    userService.procesarArchivoMasivo(archivo);
    return ResponseEntity.ok(Map.of("message", "Usuarios cargados exitosamente de forma masiva."));
}

@PutMapping("/{id}/rol")
public ResponseEntity<User> actualizarRol(@PathVariable Long id, @RequestBody Map<String, String> payload) {
    String roleName = payload.get("roleName");
    if (roleName == null || roleName.trim().isEmpty()) {
        throw new BadRequestException("El nombre del rol es requerido");
    }
    return ResponseEntity.ok(userService.actualizarRolUsuario(id, roleName.trim()));
}

@PutMapping("/{id}")
public ResponseEntity<User> actualizarUsuario(@PathVariable Long id, @RequestBody User datosActualizados) {
    return ResponseEntity.ok(userService.actualizarUsuario(id, datosActualizados));
}

@PatchMapping("/{id}/estado")
public ResponseEntity<User> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
    Boolean estado = payload.get("active");
    if (estado == null) {
        throw new BadRequestException("El campo 'active' es requerido");
    }
    return ResponseEntity.ok(userService.cambiarEstadoUsuario(id, estado));
}

@GetMapping("/perfil")
public ResponseEntity<User> obtenerMiPerfil(Principal principal) {
    if (principal == null) throw new UnauthorizedException("No autorizado");
    return ResponseEntity.ok(userService.obtenerPorEmail(principal.getName()));
}

@PutMapping("/perfil")
public ResponseEntity<User> actualizarMiPerfil(Principal principal, @RequestBody UserProfileUpdateRequest request) {
    if (principal == null) throw new UnauthorizedException("No autorizado");
    return ResponseEntity.ok(userService.actualizarMiPerfil(principal.getName(), request));
}
```

#### 4. Reemplazos en `PortfolioController.java`
Eliminar todos los `try-catch` y normalizar respuestas:
```java
@GetMapping("/documentos/{id}/descargar")
public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
    if (principal == null) throw new UnauthorizedException("No autenticado");
    DownloadedDocument downloaded = portfolioService.descargarDocumento(id, principal.getName());
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(downloaded.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloaded.fileName() + "\"")
            .body(downloaded.resource());
}

@PostMapping("/evidencias/upload")
public ResponseEntity<PortfolioDocument> subirEvidencia(
        Principal principal,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
    if (principal == null) throw new UnauthorizedException("No autenticado");
    return ResponseEntity.ok(portfolioService.subirEvidencia(principal.getName(), assignmentId, file));
}

@PostMapping("/planificaciones/upload")
public ResponseEntity<LessonPlan> crearPlanificacion(...) {
    if (principal == null) throw new UnauthorizedException("No autenticado");
    return ResponseEntity.ok(lessonPlanService.crearPlanificacion(...));
}

@DeleteMapping("/planificaciones/{id}")
public ResponseEntity<Map<String, String>> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
    if (principal == null) throw new UnauthorizedException("No autenticado");
    lessonPlanService.eliminarPlanificacion(id, principal.getName());
    return ResponseEntity.ok(Map.of("message", "Planificación eliminada exitosamente."));
}

@DeleteMapping("/documentos/{id}")
public ResponseEntity<Map<String, String>> eliminarDocumento(@PathVariable Long id, Principal principal) {
    if (principal == null) throw new UnauthorizedException("No autenticado");
    portfolioService.eliminarDocumento(id, principal.getName());
    return ResponseEntity.ok(Map.of("message", "Documento eliminado exitosamente."));
}
```

#### 5. Reemplazos en `PracticeAssignmentController.java`
```java
@PostMapping
public ResponseEntity<PracticeAssignment> crearAsignacion(@RequestBody PracticeAssignment assignment) {
    return ResponseEntity.ok(assignmentService.crearAsignacion(assignment));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminarAsignacion(@PathVariable Long id) {
    assignmentService.eliminarAsignacion(id);
    return ResponseEntity.ok().build();
}
```

#### 6. Reemplazos en `SubjectController.java`
```java
@PostMapping
public ResponseEntity<Subject> crearAsignatura(@RequestBody Subject subject) {
    return ResponseEntity.ok(subjectService.crearAsignatura(subject));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminarAsignatura(@PathVariable Long id) {
    subjectService.eliminarAsignatura(id);
    return ResponseEntity.ok().build();
}
```

#### 7. Reemplazos en `ProfessorController.java`
```java
@GetMapping("/{professorId}/estudiantes")
public ResponseEntity<List<PracticeAssignment>> obtenerEstudiantesPorProfesor(
        @PathVariable Long professorId, 
        Principal principal) {
    if (principal == null) throw new UnauthorizedException("No autenticado");
    return ResponseEntity.ok(assignmentService.obtenerEstudiantesPorProfesor(professorId, principal.getName()));
}
```

#### 8. Reemplazos en `AuthController.java`
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
}

@PostMapping("/forgot-password")
public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
    return ResponseEntity.ok(authService.forgotPassword(request.get("email")));
}

@PostMapping("/reset-password")
public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
    return ResponseEntity.ok(authService.resetPassword(request.get("token"), request.get("newPassword")));
}
```

---

## 5. Verification Method

Para verificar independientemente la validez y robustez de este blueprint, el equipo implementador debe ejecutar las siguientes comprobaciones:

### 5.1. Comandos de Compilación y Ejecución de Pruebas
1. **Compilación estricta de Backend:**
   ```bash
   mvn clean compile
   ```
   *Criterio de éxito:* Cero errores de sintaxis o resolución de dependencias.
2. **Ejecución de Pruebas Unitarias e Integración:**
   ```bash
   mvn test
   ```
   *Criterio de éxito:*
   - `SubjectControllerTest`: Los tests `testCrearAsignaturaConNombreVacioFalla`, `testCrearAsignaturaConNombreNullFalla`, `testCrearAsignaturaConCicloNullFalla`, `testCrearAsignaturaConNombreExcesivoFalla` y `testEliminarAsignaturaAsociadaAPracticaRetorna409Conflict` pasan al 100%. Las aserciones `containsString` continúan encontrando el texto en el campo `"message"` del JSON.
   - `ProfessorControllerTest`: Los tests `testOtroUsuarioConsultaEstudiantesDeProfesorDenegado403` y `testConsultaUsuarioConRolNoDocenteRetornaMensajeValido` pasan al 100% devolviendo HTTP 403 Forbidden y la subcadena esperada.
   - `PortfolioControllerTest`: Los tests `testDescargarDocumentoPorUsuarioAjenoRetorna403` y `testEliminarDocumentoPorUsuarioAjenoRetorna403` pasan con HTTP 403.
   - `AuthControllerTest`: Las pruebas de no enumeración y protección de tokens pasan sin regresiones.

### 5.2. Verificación de Compilación Frontend
   ```bash
   cd frontend
   npm run build
   ```
   *Criterio de éxito:* Construcción exitosa en Vite sin fallas de tipos o llamadas rotas. `apiClient.js` consume fluidamente `errorData.message` provisto por `ApiErrorResponse`.

### 5.3. Condiciones de Invalidación
El blueprint se considerará invalidado si:
1. Algún test existente de Spring Boot falla debido a que `GlobalExceptionHandler` altera el código de estado HTTP esperado.
2. La respuesta JSON no serializa el campo `"message"` con el texto de la excepción.
3. Se introduce alguna excepción chequeada (`checked exception`) que reintroduzca la necesidad de `throws` o `try-catch` en los controladores.
