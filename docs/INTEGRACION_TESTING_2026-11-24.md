# Integración general en testing — 24 de noviembre de 2026

Estado: integración para pruebas manuales. La publicación de una nueva versión en `main` queda pendiente de la validación de Fabián.

## Ramas integradas

| Rama | Último commit integrado |
| --- | --- |
| `origin/Seba` | `43d5955` — aprobar o solicitar cambios con comentarios por sección |
| `origin/Luis` | `cf8aaaf` — HU45, HU32 y HU37 |
| `origin/Angelo` | `919e0e8` — pautas del tutor, cierre y consulta estudiantil |
| `fabian` / `origin/fabian` | `2ffdf7a` — estadísticas, indicadores y adaptación a teléfono/tablet |

La rama de Angelo ya está incluida en la de Fabián. Se comprobó con `git merge-base --is-ancestor` que los cuatro commits pertenecen al historial final de `testing`.

## Resolución de conflictos

- Se conserva la exportación ZIP a través de `PortfolioExportService`, con sus filtros y controles de acceso.
- La navegación adaptable y la ayuda conviven con la bandeja de notificaciones y los comunicados.
- Se conservan las pautas oficiales y la firma con control de revisión. Las observaciones cualitativas de Luis se capturan por indicador, se envían como `justification` y se muestran en modo lectura tras firmar.
- Las rutas de aprobación directa y revisión por estado comparten la misma lógica: guardan la retroalimentación y notifican al estudiante al pasar a aprobado. Repetir una aprobación no duplica el aviso.
- El panel de estadísticas conserva los filtros y la ficha de práctica existentes; se eliminan las consultas antiguas cuyos resultados ya no se muestran.

## Verificación automatizada

- Backend: `mvnw.cmd test`, 336 pruebas aprobadas, sin fallos ni omisiones. Incluye regresión para ambas rutas de aprobación y aviso único.
- Frontend: `npm test`, 129 pruebas aprobadas. La prueba de firma también verifica el envío de la observación cualitativa del indicador.
- `npm run build`: correcto; advertencia por tamaño del bundle.
- `npm run lint`: sin errores; hay advertencias de hooks, memoización e imports en el proyecto.
- Después de limpiar consultas sin uso del dashboard: build correcto y 8 pruebas de estadísticas/evaluaciones aprobadas.
- Playwright con Edge (`PLAYWRIGHT_CHANNEL=msedge`): 23 pruebas aprobadas en móvil, tablet vertical, tablet horizontal y escritorio; 1 omitida por ser exclusiva de pantallas pequeñas. Se ejecutó `e2e/fabian-hu.spec.js` con los cuatro proyectos de revisión. Estas pruebas usan respuestas simuladas de API; las notificaciones, el correo y la base real requieren la validación manual indicada abajo.

## Pruebas manuales antes de publicar en main

1. Entrar como estudiante, tutor, colaborador, profesor y coordinación; comprobar navegación en teléfono, tablet y escritorio.
2. Revisar una planificación por secciones, solicitar cambios, editarla como estudiante y aprobarla como docente. Confirmar historial, aviso en la bandeja y correo con un servidor de correo configurado.
3. Guardar una pauta parcial con observaciones por indicador, reabrirla, completarla y firmarla. Confirmar que el estudiante ve solamente evaluaciones publicadas y que la pauta firmada no se puede modificar.
4. Publicar un comunicado desde una cuenta autorizada y marcar avisos como leídos.
5. Revisar estadísticas, filtros y CSV; descargar ZIP dentro del alcance de cada rol.
6. Probar con la base de datos de testing las migraciones V6 y V7, las rutas del chatbot y la persistencia de archivos.

Tras la aceptación manual, integrar `testing` en `main` y definir la siguiente versión a partir de `v1.1.0`. Esta integración no crea un tag de lanzamiento ni modifica `main`.
