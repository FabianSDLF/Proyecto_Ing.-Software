# Documentación Técnica: Sistema de Prácticas Pedagógicas

Este documento proporciona una visión general de la arquitectura, la estructura de directorios y los flujos funcionales del **Sistema de Prácticas Pedagógicas** del Liceo Narciso Tondreau. El objetivo de este documento es facilitar a los desarrolladores el entendimiento del código base y acelerar la curva de aprendizaje (Onboarding).

---

## 1. Arquitectura del Sistema

El proyecto sigue una arquitectura clásica **Cliente-Servidor (Full-Stack)** separada en dos directorios principales:

- **Backend (`/src`)**: Desarrollado en **Java 21 con Spring Boot 3**. Provee una API RESTFUL protegida mediante JSON Web Tokens (JWT) y Spring Security. Utiliza Spring Data JPA para el mapeo objeto-relacional (ORM) y conecta con una base de datos MySQL (`sistema_practicas`).
- **Frontend (`/frontend`)**: Desarrollado en **React.js (Vite)**. Actúa como una Single Page Application (SPA). Utiliza React Router DOM para la navegación y Tailwind CSS para los estilos de la interfaz de usuario. No utiliza Axios, todas las llamadas a la API se realizan mediante la función nativa `fetch`.

---

## 2. Estructura del Backend (Spring Boot)

La estructura principal del código Java se encuentra en `src/main/java/com/ubb/practicas/`:

### `model/` (Entidades de Base de Datos)
Aquí se definen las clases que mapean directamente a las tablas de la base de datos (anotadas con `@Entity`).
- **`User.java` / `Role.java`**: Gestionan los usuarios del sistema (Estudiantes, Profesores, Tutores, Administradores). La relación es de muchos-a-uno (`Role`) en la versión actual.
- **`PracticeAssignment.java`**: Es la entidad núcleo. Relaciona a un `Estudiante` con su `Profesor de Asignatura`, `Profesor Colaborador`, `Tutor` y `PracticeCenter`.
- **`LessonPlan.java` (Planificación)**: Guarda las planificaciones creadas por los estudiantes. Contiene un campo `contentJson` para almacenar secciones extensas de la clase (Inicio, Desarrollo, Cierre).
- **`PortfolioDocument.java`**: Representa los archivos físicos (PDFs, Word) subidos al sistema.
- **`enums/`**: Contiene enumeraciones estáticas de la lógica de negocio (`MathAxis`, `PlanStatus`, `DocumentType`, etc).

### `controller/` (Controladores REST)
Exponen los endpoints HTTP consumidos por el Frontend.
- **`AuthController.java`**: Endpoints abiertos (`/api/auth/login`) para validación de credenciales y generación de tokens JWT.
- **`PortfolioController.java`**: Maneja la subida de evidencias y la creación de planificaciones (`/api/portafolio/planificaciones/upload`). Se encarga de guardar archivos adjuntos en el sistema de archivos del servidor.
- **`PracticeAssignmentController.java`**: Retorna los estudiantes asignados a un Profesor, Tutor o Colaborador.
- **`UserController.java` / `RoleController.java`**: CRUD de usuarios y configuración de roles.

### `security/` (Configuración de Seguridad)
- **`SecurityConfig.java`**: Define las reglas CORS, las rutas públicas y configura BCrypt para encriptar contraseñas.
- **`JwtUtil.java`**: Lógica de generación, validación y extracción de firmas y claims de los tokens JWT.
- **`JwtAuthFilter.java`**: Filtro interceptor que revisa el header `Authorization: Bearer <token>` en cada request.

---

## 3. Estructura del Frontend (React.js)

El cliente se encuentra en la carpeta `/frontend/src`:

### `layouts/` (Estructuras de Página)
- **`MainLayout.jsx`**: Es la carcasa principal de la aplicación después del inicio de sesión. Contiene el Sidebar (menú lateral) que **se renderiza de forma condicional** basado en el rol del usuario extraído del `localStorage`.

### `pages/` (Vistas)
- **`Login.jsx`**: Formulario inicial. Realiza la solicitud al backend y guarda el Token y el Rol en `localStorage`.
- **`Dashboard.jsx`**: Vista de inicio general.
- **`MisEstudiantes.jsx`**: Vista unificada para `Profesor de Asignatura`, `Profesor Colaborador` y `Tutor`. Consulta dinámicamente el endpoint correspondiente según el rol del usuario para listar sus estudiantes a cargo.
- **`Portafolio.jsx`**: Usado por el rol de `Estudiante`. Tiene dos pestañas: *Evidencias* y *Planificaciones*. Permite descargar y eliminar documentos.
- **`NuevaPlanificacion.jsx`**: Un formulario extenso y estandarizado que fuerza al estudiante a completar todos los campos requeridos de la clase (OAT, Objetivo Específico, Inicio, Desarrollo, Cierre). Permite subir un archivo opcional y transforma los textos en un JSON.
- **`GestionRoles.jsx` / `GestionAsignaturas.jsx`**: Vistas exclusivas de Administrador para configurar la base de datos.

---

## 4. Flujos Clave Implementados

1. **Autenticación Segura (BCrypt + JWT)**:
   - Las contraseñas en la base de datos están *hasheadas*. Al hacer login, `AuthController` verifica la contraseña usando `BCryptPasswordEncoder`. 
   - Si es correcta, genera un Token JWT y lo envía al Frontend. El Frontend lo adhiere a la cabecera HTTP en futuras peticiones mediante el esquema `Bearer`.

2. **Reutilización de Vista "Mis Estudiantes" (HU11 y HU12)**:
   - Para evitar duplicar código, la vista `MisEstudiantes.jsx` detecta si el usuario logueado es `Profesor de Asignatura`, `Profesor Colaborador` o `Tutor`. 
   - Cambia dinámicamente la URL de `fetch()` a (`/api/profesor/...`, `/api/asignaciones/colaborador/...` o `/api/asignaciones/tutor/...`) según el rol del usuario, consumiendo el mismo DTO de respuesta y llenando la misma tabla visual.

3. **Formulario Estandarizado de Planificaciones (HU25)**:
   - La página `NuevaPlanificacion.jsx` captura múltiples campos de texto en formato JSON.
   - Envia un `FormData` (para soportar un archivo `MultipartFile` opcional) hacia `PortfolioController`. 
   - El controlador de Java persiste un archivo vacío si el estudiante no adjunta nada, y vincula la entidad `LessonPlan` con `PortfolioDocument` gestionando el borrado en cascada en el endpoint `@DeleteMapping("/planificaciones/{id}")`.

---

## 5. Comandos Útiles

**Levantar el Backend (Spring Boot)**:
```bash
cd practicas
./mvnw.cmd spring-boot:run
```

**Levantar el Frontend (React)**:
```bash
cd practicas/frontend
npm run dev
```

**Compilación para Producción (React)**:
```bash
cd practicas/frontend
npm run build
```
