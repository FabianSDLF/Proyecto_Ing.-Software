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
* **Servidor Web:** Nginx (en entorno Dockerizado)

---

## 🐳 Ejecución Rápida con Docker (Recomendado)

La forma más sencilla y recomendada de levantar todo el proyecto (Base de datos, Backend y Frontend) sin necesidad de instalar Java, Node o MySQL manualmente en tu máquina o servidor, es utilizando **Docker**.

### Requisitos Previos (Docker)
* Tener instalado [Docker Desktop](https://www.docker.com/products/docker-desktop/) (o Docker Engine en Linux).

### Pasos para ejecutar
1. Abre una terminal en la raíz del proyecto (`Proyecto_IngSoft_Practicas`).
2. Ejecuta el siguiente comando para construir y levantar todos los contenedores en segundo plano:
   ```bash
   docker compose up -d --build
   ```
3. ¡Listo! Una vez finalizado el proceso de construcción, la aplicación estará disponible en:
   * **Frontend:** [http://localhost](http://localhost)
   * **Backend (API):** [http://localhost:8080](http://localhost:8080)

Para detener los contenedores, simplemente ejecuta:
```bash
docker compose down
```

---

## 🛠️ Instalación y Ejecución Manual (Modo Desarrollo)

Si prefieres ejecutar el código manualmente (por ejemplo, para depurar o desarrollar), sigue estos pasos.

### Requisitos Previos (Manual)
* **Java Development Kit (JDK) 21 (LTS)**
* **Node.js** 20.x (LTS) y **npm** 10.x
* **MySQL Server** 8.4 (LTS)

### 1. Configuración del Backend (Base de Datos)
1. Abre tu gestor de MySQL y crea una base de datos.
2. Verifica que las credenciales de conexión en tu archivo `src/main/resources/application.properties` sean correctas.
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
├── docker-compose.yml   # Configuración y orquestación de Docker
├── Dockerfile           # Imagen del Backend
└── pom.xml              # Dependencias de Maven
```

---
*Desarrollado para la asignatura de Ingeniería de Software - 2026.*
