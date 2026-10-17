# PITAGORAS - Sistema de Gestión de Prácticas

Plataforma integral desarrollada para la gestión, seguimiento y evaluación del proceso de prácticas pedagógicas (Inicial, Intermedia y Avanzada). El sistema centraliza la comunicación entre estudiantes, profesores colaboradores, tutores y administradores, e integra herramientas avanzadas como un portafolio digital y validación de planificaciones mediante Inteligencia Artificial (ChatBot).

---

## Requisitos
- **Java:** 21 (LTS)
- **MySQL:** 8.4 (LTS)
- **Node.js:** 20 (LTS) y **npm** 10.x

---

## Configuración
1. **Variables de entorno (Backend):** Copiar `.env.example` como `.env` en la raíz del proyecto y completar `DB_PASSWORD` y `MAIL_PASSWORD`:
   ```powershell
   Copy-Item .env.example .env
   ```
2. **Variables de entorno (Front-end):** Copiar `frontend/.env.example` como `frontend/.env`:
   ```powershell
   Copy-Item frontend/.env.example frontend/.env
   ```
3. **Crear la base de datos:** Ejecutar el script `docs/crear-usuario-bd.sql` en MySQL (el esquema de tablas se aplica automáticamente con **Flyway** desde `src/main/resources/db/migration/V1__esquema_inicial.sql`), o bien levantar con Docker:
   ```powershell
   docker compose up -d
   ```

---

## Ejecutar
- **Backend:**
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
  *(Arranca por defecto con el perfil `dev` en `http://localhost:8080`)*

- **Front-end:**
  ```powershell
  cd frontend
  npm ci
  npm run dev
  ```
  *(Disponible en `http://localhost:5173`)*

- **Ejecución completa con Docker (Opcional):**
  ```powershell
  docker compose up -d --build
  ```
  * **Frontend:** [http://localhost](http://localhost)
  * **Backend (API):** [http://localhost:8080](http://localhost:8080)

---

## Pruebas
- **Pruebas Backend (JUnit 5 + Mockito, perfil `test` aislado):**
  ```powershell
  .\mvnw.cmd test
  ```
- **Pruebas Front-end (Vitest):**
  ```powershell
  cd frontend
  npm test
  ```

---

## Versiones entregadas
- `v1.0.0`: Incremento 1
- `v1.1.0`: Incremento 2 + configuración revisada

---

## 📂 Estructura Principal del Proyecto

```text
Proyecto_Ing.-Software/
├── src/main/java/com/ubb/practicas/
│   ├── controller/      # Endpoints REST (API)
│   ├── model/           # Entidades JPA (Base de Datos)
│   ├── repository/      # Interfaces de acceso a datos
│   ├── security/        # Configuración JWT y filtros de seguridad
│   └── service/         # Lógica de negocio
├── src/main/resources/
│   ├── db/migration/    # Scripts SQL versionados con Flyway (V1__esquema_inicial.sql)
│   ├── application.properties       # Configuración común (perfil por defecto: dev)
│   ├── application-dev.properties   # Configuración de desarrollo
│   └── application-prod.properties  # Configuración de producción
├── frontend/
│   ├── src/
│   │   ├── components/  # Componentes UI reutilizables
│   │   ├── pages/       # Vistas de la aplicación (Login, Dashboard, Portafolio)
│   │   └── App.jsx      # Enrutamiento principal
│   ├── .env.example     # Plantilla de variables de entorno del cliente
│   └── .nvmrc           # Versión fijada de Node.js (20)
├── docs/
│   └── crear-usuario-bd.sql # Script inicial de creación de base y usuario MySQL
├── .env.example         # Plantilla de variables de entorno del servidor
├── docker-compose.yml   # Configuración y orquestación de Docker
├── Dockerfile           # Imagen del Backend
└── pom.xml              # Dependencias de Maven
```

---
*Desarrollado para la asignatura de Ingeniería de Software - 2026.*
