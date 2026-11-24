# Implementación de HU44, HU35 y HU40

Fecha: 18 de noviembre de 2026. Se incorporó el código del commit `919e0e8` de `origin/Angelo` (HU43, HU46 y HU47) al árbol de trabajo antes de completar estas historias. Los acuerdos funcionales están en [ACUERDOS_IMPLEMENTACION.md](ACUERDOS_IMPLEMENTACION.md). La rama local conserva los cambios anteriores y la configuración local del usuario.

## HU44: captura común de evaluaciones

- Se reutilizan el catálogo versionado, la persistencia de criterios, la escala de servidor y la firma/publicación aportados por Angelo.
- `EvaluationForm` y `RubricIndicators` separan la captura común del listado de estudiantes. Agrupan indicadores por dimensión, incluyendo la dimensión E de Orientación, y permiten seleccionar enteros de 2 a 7 o dejar pendientes.
- Se contrastaron los textos, códigos, orden, cantidad y máximos de las ocho pautas con los archivos originales transcritos. Se corrigieron dos textos del tutor y dos nombres de archivo fuente del catálogo. Especialidad primera visita tiene 19 indicadores; Orientación primera visita, 18; ambas segundas visitas, 23.
- Un borrador incompleto conserva puntajes y observaciones, muestra progreso y no tiene nota. El servidor calcula la nota al completar; el cliente no puede imponerla. Todos los indicadores en 2 producen 3,0 conforme al acuerdo.
- Se rechazan valores decimales, códigos inválidos y códigos duplicados incluso cuando la primera aparición está pendiente. Las observaciones admiten hasta 1000 caracteres.
- Se avisa antes de descartar cambios o abandonar el documento del navegador. Al guardar y reabrir se recuperan puntajes y revisión.
- Se mantiene el cierre explícito, privado hasta la firma y con instantánea de Angelo. Se corrigió un conflicto de concurrencia observado en MySQL: las escrituras usan `READ_COMMITTED`, bloqueo de la asignación y actualización de la entidad después de esperar el bloqueo de cierre. Dos solicitudes simultáneas producen un éxito y un 409.

### Aplicabilidad explícita

En **Gestión de Asignaturas**, creación o edición, activar «Aplicar las ocho pautas de práctica profesional (2025-2)» únicamente para las asignaturas correspondientes. El catálogo por asignación y el servidor de guardado aplican esta configuración. No se deduce por pertenecer a AVANZADO.

La migración `V7__pautas_profesionales_por_asignatura.sql` agrega `professional_rubrics`, desactivado por defecto. Solo reconoce automáticamente la asignatura de demostración PEM-310 con los nombres profesionales conocidos. El resto requiere configuración explícita. Los instrumentos genéricos históricos y las notas firmadas se conservan.

## HU35: estadísticas institucionales

`GET /api/reportes/estadisticas` admite `periodId`, `history`, `cycle` y `subjectId`. Solo Administrador y Coordinador tienen acceso al panel global; el servidor comprueba el usuario y su alcance.

El mismo panel aparece en Inicio y Reportes para estos roles:

- Estudiantes distintos, asignaciones, centros utilizados y evaluaciones publicadas por identificador único.
- Instrumentos esperados y pendientes únicamente en asignaturas profesionales configuradas; un borrador no equivale a publicación.
- Promedio simple y tamaño de muestra por instrumento y versión, con gráfico de escala 2–7. Las evaluaciones históricas/manuales se identifican por separado.
- Serie por periodo e instrumento/version, sin mezclar periodos de igual nombre con distinto ID.
- Estado de la última versión de cada planificación lógica. Se distingue aprobación IA de aprobación docente.
- Detalle por asignación con acceso al portafolio. Carga, vacíos, errores y reintento.
- CSV con los mismos filtros del panel. Se corrigió el exportador para respetar `history` y se incorporó el filtro de ciclo. El CSV contiene detalle por indicador: sus filas no representan un conteo de estudiantes.

Los docentes conservan su reporte y exportación dentro de su alcance. No se añade un rol Director ni una nota final ponderada.

## HU40: navegación, formularios y ayuda

- Navegación móvil desplegable, cierre por Escape, foco contenido y retorno al disparador, enlace para saltar al contenido y menú de escritorio.
- Modales con identificadores accesibles únicos, foco contenido, retorno de foco, scroll propio y Escape limitado al modal superior.
- Pautas con controles táctiles, tablas con scroll interno y chatbot ajustado al ancho/alto disponible. Se corrigió el desbordamiento del selector de pauta en móvil.
- `/ayuda` contiene tutoriales por rol sobre acceso, recuperación, carga de archivos, planificación/IA, borradores, firma/publicación y reportes.
- La ayuda informa los formatos realmente aceptados por las pantallas: evidencias PDF/Word/PowerPoint; documentos finales PDF/Word, máximo 20 MB.

### Contacto de coordinación

Configurar explícitamente en `.env` y reiniciar el backend:

```dotenv
SUPPORT_COORDINATOR_NAME=
SUPPORT_COORDINATOR_EMAIL=
SUPPORT_COORDINATOR_PHONE=
```

Nombre y correo válidos son necesarios; el teléfono es opcional. Docker Compose transmite estas variables al backend. `GET /api/ayuda/contacto` requiere autenticación. Si falta configuración, la ayuda indica que el contacto no está disponible y conserva los tutoriales. No se elige una cuenta arbitraria ni se inventa un correo.

## Verificación reproducible

Desde la raíz del repositorio, con Java 21:

```powershell
.\mvnw.cmd -o test
python scripts/verify-rubric-sources.py
```

El verificador de fuentes requiere conservar `../04_Documentacion_Cliente`.

Desde `frontend`:

```powershell
npm test
npm run build
npm run lint
$env:PLAYWRIGHT_CHANNEL = 'msedge'
npm run test:e2e -- fabian-hu.spec.js --project=mobile --project=tablet --project=tablet-landscape --project=desktop-review --workers=2
```

Puede omitirse `PLAYWRIGHT_CHANNEL` cuando está instalado el Chromium de Playwright. Los perfiles comprueban 360×800, 768×1024, 1024×768 y 1366×768. Usan cuentas y API simuladas para verificar interacción, accesibilidad y desbordamientos; no prueban persistencia del servidor. Guardan capturas en `frontend/test-results` y trazas cuando falla un caso.

`node scripts/verify-fabian-api.mjs` comprueba persistencia, permisos, ocho pautas, escala, dos guardados simultáneos, dos firmas simultáneas, consulta estudiantil, métricas y CSV contra un backend **aislado** en `127.0.0.1:18080`. Requiere una base nueva con datos de demostración y escribe/firma evaluaciones de prueba. No ejecutar contra una base con trabajo real. Los resultados se guardan en `.verification/fabian-api-results.json`.

### Migraciones y datos existentes

Se probaron las migraciones hasta V7 con MySQL 8.4 en Docker y almacenamiento de prueba separado. Antes de aplicar V6 a datos existentes, revisar duplicados sin eliminarlos:

```sql
SELECT practice_assignment_id, type, COUNT(*) AS cantidad
FROM evaluaciones
GROUP BY practice_assignment_id, type
HAVING COUNT(*) > 1;
```

La unicidad de V6 detendrá la migración si encuentra duplicados. Resolverlos con sus responsables conservando las evaluaciones oficiales; no borrar automáticamente. Conservar un respaldo antes de migrar el entorno definitivo.

## Límites de cierre

Resultados comprobados: **331 pruebas de backend, 129 de frontend y 16 recorridos Playwright aprobados**. Build de producción y configuración de Docker Compose correctos. Lint sin errores, con advertencias de hooks/variables existentes y del patrón de carga asíncrona; el build conserva el aviso de bundle superior a 500 kB.

MySQL 8.4 aplicó correctamente las siete migraciones desde una base vacía. La prueba HTTP real completó las ocho pautas, mantuvo sus borradores privados y firmó/publicó una de ellas. Se comprobaron siete grupos de casos, incluyendo concurrencia de guardado y firma (200 + 409), filtros inválidos (400), permisos institucionales, cálculo, CSV y contacto vacío. Evidencia local en `.verification/fabian-api-results.json`, `.verification/fabian-mysql-final.log` y los logs `fabian-*`; capturas de navegador en `frontend/test-results`.

La implementación local no modifica estados en Trello. El contacto real permanece pendiente de configuración. Las pruebas de tablet son emuladas con navegador de escritorio; falta comprobar el recorrido con una tablet física cuando esté disponible. HU45 y la identidad visual/calendario de HU39 mantienen sus responsables y alcance.
