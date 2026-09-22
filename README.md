# PITAGORAS - Sistema de Gestión de Prácticas

Plataforma integral desarrollada para la gestión, seguimiento y evaluación del proceso de prácticas pedagógicas (Inicial, Intermedia y Avanzada). El sistema centraliza la comunicación entre estudiantes, profesores colaboradores, tutores y administradores, e integra herramientas avanzadas como un portafolio digital y validación de planificaciones mediante Inteligencia Artificial (ChatBot).

## 🚀 Tecnologías y Stack

El proyecto es una aplicación **Full-Stack** dividida en dos componentes principales:

### Backend (API REST)
* **Lenguaje:** Java 21
* **Framework:** Spring Boot
* **Persistencia:** Spring Data JPA / Hibernate
* **Base de Datos:** MySQL
* **Seguridad:** Spring Security con JSON Web Tokens (JWT)
* **Librerías Adicionales:** Maven, Lombok, Apache POI (para carga masiva vía Excel)

### Frontend (Cliente Web)
* **Framework:** React
* **Build Tool:** Vite
* **Estilos:** Tailwind CSS
* **Enrutamiento:** React Router Dom

---

## 📋 Requisitos Previos

Asegúrate de tener instalado lo siguiente en tu entorno de desarrollo local:
* **Java Development Kit (JDK) 21**
* **Node.js** (versión 18 o superior recomendada) y **npm**
* **MySQL Server**

---

## 🛠️ Instalación y Ejecución

Sigue estos pasos para levantar el entorno de desarrollo en tu máquina local.

### 1. Configuración del Backend (Base de Datos)
1. Abre tu gestor de MySQL (ej. MySQL Workbench) y crea una base de datos para el proyecto (por ejemplo, `practicas_db`).
2. Verifica que las credenciales de conexión en tu archivo `src/main/resources/application.properties` sean correctas:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/practicas_db
   spring.datasource.username=TU_USUARIO_MYSQL
   spring.datasource.password=TU_PASSWORD_MYSQL
   spring.jpa.hibernate.ddl-auto=update
   ```
3. Ejecuta el servidor Backend. Si usas la consola:
   ```bash
   ./mvnw spring-boot:run
   ```
   *El servidor Spring Boot se iniciará en `http://localhost:8080`.*

### 2. Ejecución del Frontend
1. Abre una nueva terminal y navega a la carpeta del frontend:
   ```bash
   cd frontend
   ```
2. Instala las dependencias del proyecto:
   ```bash
   npm install
   ```
3. Inicia el servidor de desarrollo de React:
   ```bash
   npm run dev
   ```
   *La aplicación web estará disponible, por defecto, en `http://localhost:5173`.*

---

## 📂 Estructura Principal del Proyecto

```text
Proyecto_IngSoft_Practicas/
├── src/main/java/.../practicas/
│   ├── controller/      # Endpoints REST (API)
│   ├── model/           # Entidades JPA (Base de Datos)
│   ├── repository/      # Interfaces de acceso a datos
│   ├── security/        # Configuración JWT y filtros de seguridad
│   └── service/         # Lógica de negocio
├── frontend/
│   ├── src/
│   │   ├── components/  # Componentes UI reutilizables
│   │   ├── pages/       # Vistas de la aplicación (Login, Dashboard, Portafolio)
│   │   └── App.jsx      # Enrutamiento principal
├── uploads/             # Almacenamiento local de archivos y evidencias
└── pom.xml              # Dependencias de Maven
```

---
*Desarrollado para la asignatura de Ingeniería de Software - 2026.*
