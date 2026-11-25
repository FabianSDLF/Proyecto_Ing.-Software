# Pruebas de Integración de APIs con Postman (PITÁGORAS)

Esta carpeta contiene la colección y el environment de Postman correspondientes a la **Parte 5 del Laboratorio de Pruebas de Integración de APIs** (Evidencia 6 / Sprint 3).

## Archivos

- `pitagoras-api.postman_collection.json` — Colección v2.1 con:
  - **01 - Autenticación**: login JWT (`POST /api/auth/login`) que guarda `{{token}}` automáticamente y verificación de perfil (`GET /api/usuarios/perfil`).
  - **02 - CRUD Centros de Práctica**: ciclo completo sobre `/api/centros` (listar, crear, obtener por ID, actualizar, prueba negativa `400`, eliminar y verificar eliminación `404`).
  - **03 - Chatbot Pedagógico (Nivel A y Nivel B)**:
    - **Nivel A**: `/health`, `POST /api/chat` (estructura y umbral `< 10000 ms`), prueba negativa con mensaje vacío (`400 Bad Request`) y `POST /api/evaluar-planificacion`.
    - **Nivel B**: prueba directa a la API externa de Google Gemini usando `{{llmApiKey}}` en el header `x-goog-api-key`.
- `pitagoras-local.postman_environment.json` — Environment `PITAGORAS Local` sin contraseñas ni API keys almacenadas.

## Cómo usarla en Postman

1. Levantar el backend Spring Boot en `http://localhost:8080` (`.\mvnw.cmd spring-boot:run`) y el microservicio de chatbot en `http://localhost:8000` (`uvicorn main:app --port 8000` dentro de `chatbot/`).
2. En Postman, ir a **Import** e importar ambos archivos `.json` de esta carpeta.
3. Seleccionar el environment **PITAGORAS Local** en la esquina superior derecha.
4. Configurar **solo en Current value** (nunca en Initial value) las variables secretas:
   - `adminPassword`: contraseña del usuario `admin@ubb.cl` en tu entorno local.
   - `llmApiKey`: tu API key de Google Gemini (solo requerida para la request **Nivel B - 5. API Externa LLM**).
5. Abrir el **Collection Runner** sobre `PITAGORAS API & Chatbot - Pruebas de Integracion` y ejecutarla **dos veces seguidas** para confirmar que es idempotente y pasa completa en verde.
   - *Nota:* Si no deseas consumir cuota de Gemini en ejecuciones repetidas, puedes desmarcar `Nivel B - 5. API Externa LLM (Google Gemini)` en el Runner.
