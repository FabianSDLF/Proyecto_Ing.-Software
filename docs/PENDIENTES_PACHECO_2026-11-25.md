# Pendientes para retomar desde la U el 25 de noviembre

Informe preparado el 24 de noviembre de 2026. Sitio: http://pacheco.chillan.ubiobio.cl:8008/.

**Actualización del despliegue:** `ceedbb3` ya fue subido y desplegado por Codex; arranque confirmado a las 21:23 de Santiago del 24 de noviembre. Ver [DESPLIEGUE_PACHECO_2026-11-24.md](DESPLIEGUE_PACHECO_2026-11-24.md) para respaldo, imágenes, extracción del ZIP y estado real. Las pruebas funcionales en Pacheco corresponden a Antigravity. El chatbot informa `gemini_api_configured: false`, pendiente configurar clave para validar Gemini real. Las referencias posteriores a `212535a` describen el estado anterior a esta actualización.

**Ampliación de pruebas por los cinco roles:** ver [la matriz funcional y los 14 hallazgos](PRUEBAS_FUNCIONALES_ROLES_2026-11-24.md). La versión `testing @ ceedbb3` ya fue desplegada y probada en Pacheco (`/assets/index-1m5II2gH.js`). **Los 14 hallazgos F01–F14 quedaron validados en Pacheco** (incluyendo Gemini real en chat y validador con `is_mock: false`, descarga ZIP y 4 tamaños de pantalla), pero se detectó un nuevo fallo de esquema MySQL (**F15 — Alta**): `POST /api/evaluaciones` con tipos profesionales nuevos (`TUTOR_ESP_1`, etc.) devuelve HTTP 500 en Pacheco (mientras `RUBRICA_INFORME` devuelve 200), porque la columna heredada `evaluaciones.type` en MySQL sigue como `ENUM` antiguo en lugar de `VARCHAR(50)`. También se observó que `/chatbot/api/chat-archivo` devolvió `is_mock: true`.

## Estado de la entrega

`testing @ ceedbb3` está desplegado en Pacheco (`~/pitagoras-testing-ceedbb3/pitagoras`). Se corrigieron en `testing` los tres puntos detectados durante la validación en servidor:
1. **F15:** nueva migración Flyway `V8__ampliar_columna_tipo_evaluacion.sql` (sincronizada también en `flyway-ubb/migrations/` por `Package-Pacheco.ps1` sin tocar `V1`–`V7`).
2. **`/chatbot/api/chat-archivo`:** integrado con Gemini real (`is_mock: false`) cuando hay clave configurada, conservando respaldo heurístico.
3. **Empaquetador ZIP POSIX:** `scripts/Package-Pacheco.ps1` genera entradas con `/` (sin barras invertidas de Windows).

Se generó un nuevo paquete listo para subir a Pacheco (reconstruir `backend` y `chatbot`, conservando `.env`, `.compose-project`, `.compose-compat` y volúmenes). `main` sigue pendiente de las pruebas manuales de Fabián. No publicar en `main` hasta desplegar esta actualización y verificar `TUTOR_ESP_1` y `/api/chat-archivo` en Pacheco.

El usuario informó que no puede acceder por **SSH** desde su conexión actual y que no dispone de VPN. La aclaración se refiere a SSH; no demuestra una caída del sitio ni del contenedor del chatbot. Se retomará el acceso desde el wifi universitario.

## Fallos reproducidos

| Prioridad | Caso y reproducción | Resultado observado | Corrección pendiente y criterio de aceptación |
| --- | --- | --- | --- |
| Alta | Iniciar sesión, abrir el asistente y enviar una consulta pedagógica genérica. | El indicador de salud carga la base documental, pero la consulta muestra un error de conexión. | `frontend/src/services/chatbotService.js`: `sendMessage` y `evaluateLessonPlan` omiten `Authorization`, requerido por el middleware del chatbot. Añadir autenticación como en `sendFileMessage`, probar ambos flujos y verificar respuesta completa en Pacheco. La omisión está confirmada en código; pueden existir otros problemas que solo se detecten después de corregirla. |
| Alta | Como coordinación, abrir un portafolio desde Reportes y consultar sus planificaciones. | Aparece «No tienes permisos para ver las planificaciones de esta práctica». | Revisar `LessonPlanServiceImpl.obtenerPlanificacionesPorAsignacion`: exige participación y no contempla el alcance de administración/coordinación utilizado por `AssignmentScope`. Aplicar la política de supervisión prevista y probar que tutor/estudiante ajenos siguen bloqueados. |
| Media | Abrir ese portafolio desde el enlace de Reportes o mediante enlace directo. | El encabezado puede quedar como «Estudiante», sin ficha de práctica; el regreso «Mis Estudiantes» lleva a una ruta de docencia y redirige a coordinación al inicio. | En `Portafolio.jsx`, cargar la ficha por identificador sin depender del estado de navegación ni de tener planificaciones. Mostrar estudiante, asignatura y centro correctos; regresar a una ruta accesible según rol/origen. Probar también recarga y portafolio vacío. |
| Media | Como tutor y como estudiante, consultar la evaluación histórica publicada y abrir el desglose. | Valores históricos como `1` y `0.8` se muestran sobre `/7 pts`, junto a una escala genérica de 2 a 7. | Revisar `RubricIndicators.jsx`, `EvaluationForm.jsx` y `StudentEvaluationsPanel.jsx`. Derivar escala del instrumento/versión; si falta, identificar el registro como histórico sin atribuirle la escala actual. Conservar la nota publicada y su modo de solo lectura. |

## Observaciones que requieren revisión

- Ciclo Inicial mostraba semestres IV a IX y Avanzado no tenía rango definido. Confirmar la configuración académica esperada antes de modificar datos reales.
- En una navegación del estudiante, la práctica apareció temporalmente como histórica; tras recargar, la vista vigente mostró la planificación y evaluación correctas. No se considera un fallo reproducido: repetir la navegación y registrar solicitudes antes de atribuir una causa.
- Ayuda no tenía contacto configurado. Completar la configuración si corresponde; no se verificó como fallo de implementación.

## Retomar, corregir y publicar

1. Desde la U, comprobar `ssh fabiansepulveda2301@pacheco.chillan.ubiobio.cl`. Las credenciales y frases de clave no se guardan en este documento.
2. Reproducir los fallos con las cuentas de prueba. Corregir en `testing`, ejecutar pruebas de autenticación del chatbot, permisos de supervisión y visualización de instrumentos históricos. Repetir las regresiones existentes de fechas y menú lateral. No publicar evaluaciones ni comunicados para probar.
3. Confirmar los cambios y generar un **nuevo** paquete con `scripts/Package-Pacheco.ps1`. El ZIP de `212535a` no contiene las correcciones pendientes. Verificar `BUILD.json` y los hashes antes de subirlo.
4. Respaldar según `docs/RESPALDOS.md`. Conservar la configuración del despliegue actual `~/pitagoras-testing-212535a/pitagoras`: `.env`, `.compose-project` y `.compose-compat`. El proyecto es `pitagoras` y utiliza `flyway-ubb/compose.yml` como archivo adicional. No imprimir secretos en registros.
5. Subir ZIP y checksum, verificar SHA-256 y extraer en un directorio nuevo. Usar Python `zipfile.ZipFile(...).extractall(...)`: el `unzip` del servidor alteró nombres de PDF con tildes durante esta entrega. Verificar `SHA256SUMS` después de extraer. Copiar la configuración antes de cambiar contenedores.
6. Validar Compose con el mismo proyecto y archivos adicionales; reconstruir y recrear únicamente los servicios modificados. La corrección de cabeceras del chatbot requiere reconstruir el frontend. Si cambia código Python, reconstruir también chatbot. Conservar `pitagoras_db_data` y `pitagoras_backend_uploads`.
7. Revisar estado y logs; probar login, chatbot completo, portafolio de coordinación, permisos de estudiante, evaluación histórica, fechas y cierre de sesión en escritorio y móvil. Confirmar que el navegador carga el nuevo archivo de frontend.
8. Mantener `main` pendiente hasta que Fabián termine sus pruebas manuales. Limpiar caché de construcción solo después de verificar el nuevo despliegue. No usar `down -v`, `volume prune` ni limpieza global de volúmenes.

## Referencias del despliegue actual

- Ruta: `~/pitagoras-testing-212535a/pitagoras`.
- Servicios: `practicas_db`, `practicas_backend`, `practicas_frontend`, `practicas_chatbot`; gestor real: Podman con proveedor Compose.
- Paquete local: `deploy/pitagoras-pacheco-2026-11-24-212535a.zip`.
- SHA-256 del ZIP: `ec0564a8745ca5bad6ab25cdfddf48fc2708f482abd3b497ef0398847d7f68b5`.
- Frontend observado: `/assets/index-BCwwASBW.js`.
- Etiquetas conservadas antes de actualizar: `localhost/pitagoras_backend:rollback-5078737` y `localhost/pitagoras_frontend:rollback-5078737`. Corresponden a la versión anterior; preparar además una reversión de `212535a` antes del próximo despliegue.

Validación local de las tres correcciones desplegadas: 132 pruebas frontend, 343 backend y cinco regresiones de navegador aprobadas. Estas cifras no validan los fallos pendientes de la tabla.
