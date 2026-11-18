# Auditoría y respaldos — HU38

La API registra las operaciones POST, PUT, PATCH y DELETE bajo `/api/`, con usuario autenticado cuando existe, método/acción, ruta, resultado HTTP, IP y fecha. No almacena contraseñas, tokens, cuerpos ni parámetros de consulta. Incluye intentos fallidos de login, identificados como `LOGIN_FALLIDO` sin almacenar el correo del formulario.

Solo el Administrador consulta `GET /api/admin/auditoria`, con filtros `usuario`, `accion`, `desde`, `hasta`, `page` y `size` (máximo 100). No se exponen modificación ni borrado. Una falla al persistir auditoría genera un error de servidor en logs sin revertir una operación ya completada. Las operaciones de respaldo se acreditan mediante su registro de ejecución y los hashes del snapshot; no se simula un evento HTTP para ellas.

## Respaldo manual y periódico

Ejecutar en Windows con Docker Desktop iniciado, desde la raíz del proyecto:

```powershell
.\scripts\Backup-Pitagoras.ps1
```

El script conserva el estado de ejecución del backend, lo detiene antes de tomar la copia y lo inicia de nuevo si estaba activo. Esto produce una interrupción breve y evita escrituras mientras se respaldan MySQL y los archivos. El servicio `backup`, habilitado únicamente con el perfil `maintenance`, genera `database.sql`, `uploads.tar.gz` y `SHA256SUMS` dentro de `backups/snapshot-*`. Una copia fallida queda oculta y no se publica como completa. La retención por defecto es 30 días. Copiar también snapshots a otro equipo/disco para cubrir pérdida del servidor.

Para instalar una tarea diaria bajo la cuenta del operador, ejecutar explícitamente `./scripts/Install-BackupSchedule.ps1 -At 03:00` con permisos para registrar tareas y Docker disponible en esa sesión. El script usa `IgnoreNew` para evitar solapamientos. Alternativamente, configurar el Programador de tareas de Windows para ejecutar diariamente en una ventana de mantenimiento:

```text
powershell.exe -NoProfile -NonInteractive -File "RUTA_ABSOLUTA\scripts\Backup-Pitagoras.ps1"
```

En Linux, configurar el equivalente en cron: detener el backend, ejecutar `docker compose --profile maintenance run --rm --no-deps backup` y arrancar el backend mediante un `trap` incluso si falla el respaldo. No ejecutar directamente el servicio con el backend escribiendo. La instalación de la tarea periódica queda a cargo del operador: este cambio no registra tareas ni interrumpe servicios en el equipo del usuario.

## Restauración y prueba aislada

La restauración sustituye datos: realizar primero una prueba en un proyecto Compose aislado y conservar intactos los volúmenes originales. Usar una base de datos y un volumen de uploads vacíos, las mismas credenciales y una copia de los snapshots. El directorio `backup` debe acompañar el despliegue.

Con backend detenido y la base destino en ejecución:

```powershell
docker compose --profile maintenance run --rm --no-deps backup /scripts/restore.sh snapshot-AAAAMMDDTHHMMSSZ-NUMERO
```

El servicio tiene `/bin/bash` como entrypoint. Verifica los hashes antes de importar SQL y extraer archivos. No elimina archivos existentes: exige un volumen de uploads vacío. Tras restaurar, iniciar backend y comprobar: login, recuento de usuarios/asignaciones/documentos/evaluaciones, consulta de auditoría y descarga de varios documentos. Guardar fecha, snapshot utilizado y resultados de esas comprobaciones.

El 18 de noviembre de 2026 se generó `snapshot-20261118T121845Z-30893` en un entorno aislado y se restauró en un segundo MySQL 8.4 vacío. Los hashes de SQL y archivo comprimido fueron correctos; coincidieron 21 usuarios, 3 documentos, 3 evaluaciones y 17 registros de auditoría. Los archivos extraídos también coincidieron por SHA-256. La programación periódica en el servidor definitivo aún requiere instalar el planificador y verificar su primera ejecución; no se instaló una tarea en el equipo del usuario.

## Migraciones

Había dos archivos con versión Flyway `V2`. Se conserva `V2__agregar_feedback_portafolio_y_planificaciones.sql` y se mueve la creación idempotente de ciclos a `V4__ciclos_practica.sql`. `V5` agrega ficha, revisión de IA y auditoría. En una instalación que haya registrado la antigua V2 de ciclos, revisar `flyway_schema_history` y reconciliar explícitamente las versiones con el operador antes de migrar; no ejecutar `repair` automáticamente ni desactivar la validación para ocultar divergencias.
