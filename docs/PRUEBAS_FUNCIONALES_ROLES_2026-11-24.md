# Revisión funcional por roles: Pacheco y local

Fecha: 24 de noviembre de 2026, zona de referencia America/Santiago. Base de código: `testing`, commit `212535a`. No se modificó código de aplicación ni se hizo otro despliegue durante esta revisión.

## Entornos y alcance

- **Pacheco:** http://pacheco.chillan.ubiobio.cl:8008/. Navegación real con cuentas de demostración de coordinación, tutor, profesor de asignatura, profesor colaborador y estudiante. La primera revisión fue de consulta y validación. Después de que Fabián aclarara que el servidor contiene únicamente datos de prueba, se reprodujo también F14 mediante la API del servidor, creando una cuenta, una práctica y un PDF sintéticos. No se publicaron notas ni comunicados.
- **Local:** interfaz en `127.0.0.1:5173`, backend en `127.0.0.1:8080`, chatbot en `127.0.0.1:8000`. Backend ejecutado desde el JAR de la entrega, frontend desde el código del mismo commit, base H2 **en memoria** `qa_roles`, archivos de prueba en `.verification/roles-uploads`. No se conectó a MySQL ni a Pacheco. Las operaciones de escritura se ejercitaron aquí mediante interfaz y API.
- **IA local:** sin clave Gemini; las respuestas autenticadas son de respaldo y se identifican como `is_mock`. Esto permite contrastar autenticación y persistencia, pero no valida respuestas reales de Gemini ni sus cuotas.
- **Correo local:** servidor SMTP de prueba sin servicio configurado. Los errores de envío a localhost no se clasifican como fallos de Pacheco. No se verificó la entrega de correos ni se cambiaron contraseñas reales.
- **SSH:** el usuario informó que no tiene acceso desde su conexión actual; no se reintentó ni se desplegó nada. Continuar mañana desde el wifi de la U. `main` sigue pendiente de aprobación manual.

Se recorrieron las funciones accesibles por los cinco roles y se contrastaron los fallos reproducibles. Esta matriz distingue pruebas de interfaz, pruebas de API y comprobaciones pendientes; no certifica todas las combinaciones posibles de datos, dispositivos o servicios externos.

## Matriz de cobertura

| Rol / función | Pacheco, interfaz real | Local, interfaz o API real |
| --- | --- | --- |
| Cinco roles: iniciar sesión y mostrar inicio según rol | Accesos correctos; cada cuenta mostró su rol | Cinco accesos correctos; clave incorrecta y cuenta inactiva rechazadas |
| Cerrar sesión y cambiar de cuenta | Cambios entre coordinación y docentes correctos | Cambio entre los cinco roles correcto |
| Perfil | Formulario de coordinación carga sus datos | Lectura y actualización conservando valores, comprobadas en cinco cuentas |
| Ayuda y contacto | Guía del alumno accesible; contacto sin configurar | Endpoint accesible en cinco roles; no se inventó contacto |
| Notificaciones | Consulta del alumno, contador, diálogo y cierre correctos; no se marcaron leídas | Lectura y marcar todas leídas en cinco roles; modificar notificación ajena devuelve 403 |
| Coordinación: inicio, estadísticas y reportes | Datos institucionales; filtro por ciclo y CSV comprobados en la revisión previa | Estadísticas, CSV y datos publicados accesibles |
| Coordinación: usuarios y roles | Listado, paginación y formulario manual; roles disponibles | Crear, editar, cambiar rol y desactivar cuenta temporal; correo duplicado rechazado |
| Coordinación: carga masiva | Sin archivo muestra advertencia; formulario accesible | CSV y XLSX válidos importan usuarios, verificado en listado; errores F08 y F09 |
| Coordinación: ciclos | Tres ciclos disponibles; datos académicos anómalos se registran aparte | Consultar y editar ciclo; rango invertido y edición por alumno rechazados |
| Coordinación: asignaturas | Crear/editar ya no ofrece Básico, verificado previamente | Crear, editar y eliminar asignatura temporal; eliminación de una asignatura con prácticas rechazada |
| Coordinación: periodos | Fechas invertidas bloqueadas en formulario; validación API comprobada previamente | Crear, editar, archivar y eliminar periodo temporal; fechas invertidas y eliminación con prácticas rechazadas |
| Coordinación: asignaciones | Listado y pestañas accesibles | Crear, editar con datos completos y eliminar asignación temporal |
| Coordinación: centros | Seis centros cargados | Crear, editar y eliminar centro temporal |
| Coordinación: duplas | Dos duplas; creación bloqueada al no haber estudiantes disponibles | Crear dupla con dos alumnos temporales; consultar; rechazar duplicado, mismo alumno y eliminación de dupla definitiva |
| Coordinación: supervisión desde Reportes | Fallos F02, F03 y F04 | Reproducidos por API y por interfaz |
| Tutor: Mis Estudiantes y pautas | Tres estudiantes; cuatro pautas oficiales y pauta histórica | Alcance correcto, instrumentos y pauta oficial en solo lectura |
| Tutor: evaluar asignatura sin pautas profesionales | Botón disponible, pero al pulsarlo bloquea con mensaje: F10 | Misma condición confirmada en código |
| Tutor: borrador profesional y cambios sin guardar | No se guardó ni firmó en servidor | Guardar parcial, reabrir y recuperar indicador; diálogo de cambios sin guardar comprobado en interfaz |
| Tutor: completar y firmar pauta | Solo consulta de una nota ya publicada | API: completar, exigir confirmación, rechazar cierre incompleto, firmar con snapshot y rechazar modificaciones posteriores |
| Tutor: portafolio, planificación e historial | Consulta correcta; historial v1/v2; detalle y evaluación publicada | Endpoints y permisos correctos para participante |
| Profesor de asignatura: Mis Estudiantes | Cuatro estudiantes; borrador existente abre sin guardar | Alcance propio correcto; creación de borrador manual en interfaz y diálogo de firma con confirmación requerida |
| Profesor: planificaciones y comentarios | Planificación visible e hilo de comentarios consultable; publicación vacía deshabilitada | Comentario docente guardado; solicitar cambios y aprobar con estados válidos devuelve 200 |
| Profesor: ZIP de informes | No se observó evento de descarga; queda pendiente repetir | Descarga desde interfaz confirmada; API produce ZIP válido vacío y ZIP con informe temporal |
| Profesor: acceso administrativo | Gestión de Usuarios redirige a Inicio con acceso denegado | API de usuarios denegada a docentes |
| Colaborador: Mis Estudiantes | Dos estudiantes; cuatro instrumentos propios | Alcance propio correcto; pauta publicada en solo lectura y pauta de orientación visita 2 accesible |
| Colaborador: guardar y firmar pauta | Sin escritura en servidor | API: borrador privado, completar, firmar, lectura por alumno y bloqueo posterior correctos |
| Docentes: reportes y CSV | Tutor ve tres asignaciones; colaborador dos; selector de historial accesible | Exportación CSV permitida a los tres roles docentes |
| Alumno: inicio y ficha | Datos de práctica vigente y equipo docente correctos | Consultas propias correctas; fichas ajenas devuelven 403 |
| Alumno: evidencias | Listado vacío y selector accesible; sin subir datos | Subir PDF sintético, descargar, comentario docente y eliminar evidencia correctos; F13 en tipo no permitido |
| Alumno: informe final | Formulario y tipos accesibles; sin archivo muestra error claro | Subir, rechazar duplicado no confirmado, consultar y eliminar informe temporal correctos; TXT rechazado |
| Alumno: archivos mayores de 20 MB | No se cargaron archivos grandes | Rechazados con HTTP 400 y mensaje de tamaño máximo; protección funcional correcta |
| Alumno: nueva planificación | Formulario accesible; vacío no se guarda | Crear, consultar, versionar y consultar historial; F11 en etiquetas accesibles |
| Alumno: revisar y enviar a docente | Chat falla: F01. Se ejercitó el validador sobre planificación guardada sin guardar cambios | Interfaz reproduce 401; revisión backend autenticada se persiste y se recupera; respuesta de respaldo no permite certificar envío al docente |
| Alumno: adjuntar texto al chatbot | No se adjuntaron archivos al servidor | Endpoint autenticado recibe TXT sintético y devuelve respuesta de respaldo |
| Alumno: consultar evaluaciones | Nota oficial 6,8 en lectura; desglose histórico incorrecto F05 | Borradores ajenos privados; acceso a pautas tras publicación correcto; F05 reproducido |
| Alumno: acceder a otro portafolio | La ficha y las consultas muestran acceso denegado | Ficha, planificaciones y documentos finales ajenos devuelven 403 |
| Alumno: exportaciones y evaluaciones ajenas | Sin operaciones de escritura | CSV/ZIP y evaluación profesional por alumno denegados |
| Comunicados y destinatarios | No se enviaron mensajes de prueba en Pacheco | Comunicados permitidos a coordinación/profesor; fallos de autorización F06 y F07 |
| Recuperación de acceso | Enlace disponible | Correo inexistente recibe respuesta genérica; token inválido rechazado. Entrega SMTP y cambio real de contraseña pendientes |
| Alumno: eliminar planificación revisada con IA | F14 reproducido mediante API con cuenta y adjunto sintéticos nuevos; detalle abajo | F14 reproducido, incluida pérdida del adjunto sintético |

## Fallos confirmados y reproducción

### F01 — Alta — Chat y validador IA omiten autenticación

**Pacheco y local.** Como alumno, abrir RetroPlani y pulsar la consulta de fracciones: el chat informa que no pudo conectar. En local, cargar planificación, completar datos y revisar: «No se pudo conectar con el servicio de IA (401)».

`frontend/src/services/chatbotService.js`: `sendMessage` y `evaluateLessonPlan` envían JSON sin `Authorization`. El middleware del chatbot exige token. La API local devuelve 401 sin token y 200 con token válido; el adjunto autenticado funciona. Añadir cabeceras y mensajes que distingan sesión inválida de servicio caído; después verificar Gemini real en Pacheco. Su disponibilidad todavía no está certificada.

### F02 — Alta — Coordinación no puede leer planificaciones supervisadas

**Pacheco y local.** Desde Reportes, abrir un portafolio y Planificaciones: error de permisos. `GET /api/portafolio/asignacion/1/planificaciones` devuelve 403 al coordinador local, aunque ficha y otros apartados son accesibles.

`LessonPlanServiceImpl.obtenerPlanificacionesPorAsignacion` exige participación y no aplica el alcance de supervisión de `AssignmentScope`. Corregir según política de coordinación y conservar la denegación a alumnos y docentes ajenos.

### F03 — Media — Encabezado genérico del portafolio de coordinación

**Pacheco y local.** En el acceso desde Reportes se mantiene «Estudiante: Estudiante» y «Práctica • Centro de práctica», aun cuando la ficha inferior tiene los datos correctos. En el tutor y profesor con acceso a planificaciones el encabezado termina cargando correctamente; no es un fallo permanente para todos los roles.

En `Portafolio.jsx`, cargar la ficha por identificador para el encabezado, sin depender de documentos o del estado de navegación. Probar recarga y práctica sin planificaciones.

### F04 — Media — Regreso del coordinador a una ruta prohibida

**Pacheco y local.** Pulsar «Volver a Mis Estudiantes» en supervisión con coordinación: redirige a Inicio y muestra acceso denegado. Elegir destino por rol/origen; coordinación debe poder regresar a Reportes.

### F05 — Media — Escala actual aplicada a evaluaciones históricas

**Pacheco y local.** Consultar la nota histórica publicada y abrir el desglose: muestra `1 / 7`, `1 / 7`, `0.8 / 7`. Los componentes también utilizan leyendas de escala actual para registros históricos. La nota 6,8 y el bloqueo de edición se conservan.

Revisar `RubricIndicators.jsx`, `EvaluationForm.jsx` y `StudentEvaluationsPanel.jsx`. Derivar escala de instrumento/versión o indicar que el registro histórico no tiene escala documentada, sin recalcular ni alterar notas publicadas.

### F06 — Alta — Alumnos y colaboradores pueden enviar comunicados por API

**Confirmado solo en local; no se probó enviando mensajes en Pacheco.** Autenticarse como alumno o colaborador y llamar a `POST /api/notificaciones/comunicado` con un mensaje sintético dirigido al alumno de prueba: devuelve 200 y crea la notificación. La interfaz no ofrece esa función a esos roles.

`NotificationController`, `NotificationServiceImpl.publicarComunicadoSistema` y reglas de `SecurityConfig`: falta autorizar al remitente; el servicio recibe su correo pero no lo utiliza para restringir la operación. Alinear permisos de servidor con los de la interfaz. Cubrir mensaje individual y envío a todos con pruebas de denegación.

### F07 — Alta — Directorio completo de destinatarios accesible a roles sin esa función

**Confirmado solo en local.** `GET /api/notificaciones/destinatarios` devuelve 200 al alumno y colaborador, incluyendo nombres, correos, teléfonos y roles de usuarios activos. No se extrajeron directorios de Pacheco para esta prueba.

Restringir el endpoint y devolver únicamente los datos y destinatarios necesarios para el alcance del emisor autorizado. Añadir pruebas para alumno, colaborador y cuentas sin sesión.

### F08 — Media — Importación vacía informa éxito

**API e interfaz local; no se importaron usuarios en Pacheco.** CSV con cabecera `Nombre,Correo,Rol` y una fila sin la cuarta columna: HTTP 200; la interfaz muestra «¡Usuarios cargados con éxito!», pero la cuenta no existe después. Un TXT de una sola línea también obtuvo respuesta de éxito.

`CsvUserBatchParser` omite filas con menos de cuatro columnas; `UserBatchImportServiceImpl.procesarArchivoMasivo` no rechaza cero registros. Validar estructura y filas, rechazar importación sin registros válidos y mostrar el resultado real. El formato válido comprobado usa cuatro columnas: nombre, correo, contraseña, rol.

### F09 — Media — Errores parciales de importación se ocultan

**Local.** Importar CSV de cuatro columnas con una fila válida y otra con rol inexistente: HTTP 200 y mensaje genérico de éxito. La cuenta válida se guarda y la inválida no, comprobado consultando el listado.

El resultado interno tiene contadores y errores, pero el controlador devuelve únicamente éxito genérico. Exponer creados, actualizados, rechazados y errores por fila. Añadir plantilla o instrucciones de columnas al formulario.

### F10 — Baja — Acción de evaluación ofrecida cuando no puede realizarse

**Pacheco; condición confirmada en código local.** Tutor, estudiante de Intermedio, «Evaluar (Pauta)»: aparece «Esta asignatura no tiene configuradas pautas profesionales. Consulta a coordinación». El bloqueo es correcto, pero el botón induce a una acción imposible.

En `MisEstudiantes.jsx`, representar previamente el estado sin pautas y explicar la configuración faltante. Mantener disponibles los registros históricos que sí existan.

### F11 — Media — Campos de planificación sin etiquetas accesibles

**Pacheco y local.** Objetivo basal, objetivo específico, OAT, Inicio, Desarrollo y Cierre aparecen sin nombre de campo en el árbol de accesibilidad. En local se confirmó que las seis áreas no tienen etiquetas asociadas, `id` ni `aria-label`; la de inclusión sí está asociada.

En `NuevaPlanificacion.jsx`, enlazar cada etiqueta mediante `htmlFor`/`id` o envolver su control. Revisar también el validador de RetroPlani, que utiliza etiquetas visuales sin asociación. Verificar navegación con teclado y nombres accesibles.

### F12 — Media — Hora UTC se muestra como hora local sin conversión

**Confirmado en una firma sintética local; pendiente contraste horario en Pacheco.** Firma realizada a las 19:48 de Santiago: API devuelve `2026-11-24T22:48:10.473535` y la interfaz muestra 22:48. Interpretar ese instante con `Z` produce 19:48 en America/Santiago.

Backend registra `LocalDateTime.now(ZoneOffset.UTC)` sin offset en el JSON; `MisEstudiantes.jsx` y `StudentEvaluationsPanel.jsx` usan `new Date(valor).toLocaleString('es-CL')`. Definir contrato de instantes con zona (`Instant`/`OffsetDateTime`) y convertir en interfaz, distinguiendo registros históricos. No reescribir fechas existentes sin conocer su semántica.

### F13 — Media — Tipo de evidencia no permitido devuelve error interno

**Local.** Subir evidencia sintética `qa.exe`: HTTP 500 y «Error en el almacenamiento de archivos: Tipo de archivo no permitido». El archivo es rechazado, pero un error de validación se presenta como fallo del servidor.

`LocalStorageServiceImpl` y `GlobalExceptionHandler`: separar validaciones del usuario de fallas reales de disco y devolver 400/415 con el formato permitido. El informe final TXT sí se rechaza como error de cliente; el límite de tamaño también funciona.

### F14 — Alta — Eliminar planificación revisada falla y puede perder su adjunto

**Confirmado en local y posteriormente en Pacheco con archivos sintéticos.** Fabián confirmó que Pacheco sigue siendo un entorno de pruebas y no contiene datos reales. La reproducción remota utilizó registros nuevos, separados de las cuentas de demostración previas.

1. Crear planificación de prueba con PDF adjunto: 200; descargar el PDF: 200.
2. Solicitar revisión IA mediante el endpoint autenticado: 200; queda un registro en `REGISTROS_CHATBOT`.
3. Eliminar la planificación: 409 por clave foránea `REGISTROS_CHATBOT.LESSON_PLAN_ID`.
4. Consultar de nuevo la planificación: 200, sigue existiendo. Descargar su adjunto: **404**, el archivo físico ya no existe.

`LessonPlanServiceImpl.eliminarPlanificacion` borra el archivo antes de confirmar la eliminación de la entidad. La transacción de base de datos revierte, el sistema de archivos no. Definir qué se conserva de revisiones e historial y coordinar la eliminación física después de confirmar la transacción, o implementar borrado recuperable. Añadir regresión con revisión y adjunto; comprobar tanto entidad como descarga tras un fallo. Priorizar este caso junto con los permisos de comunicados.

**Resultado remoto:** crear planificación 200; descargar adjunto 200; revisar con IA 200; recuperar revisión 200; eliminar planificación 409; consultar planificación 200; descargar adjunto después 404. La revisión remota devolvió `is_mock: true`: este recorrido no certifica Gemini real. Registros de reproducción en Pacheco: usuario `22`, asignación `7`, planificación `5`, documento `7`. Se desactivó la cuenta QA y se canceló su asignación, conservando la planificación y la revisión como evidencia del fallo. El adjunto sintético se perdió durante la eliminación fallida; no se tocaron archivos de otros registros.

## Resultados que no se clasifican como fallos de aplicación

- Pacheco no produjo evento de descarga ZIP en el intento del profesor. Local sí lo produjo y la API genera ZIP válido, incluso con informe sintético. Falta repetir en el servidor y comprobar si hay documentos elegibles y si la descarga termina; no atribuir todavía una causa.
- Un intento de evaluación de alumno devolvió 400 por asignatura sin pautas antes de llegar a la denegación esperada 403. En asignatura profesional el alumno sí obtuvo 403. No se consiguió guardar una evaluación como alumno.
- La primera hoja Excel de prueba tenía tres columnas y fue rechazada con mensaje de rol obligatorio. Con las cuatro columnas correctas se importó y verificó la cuenta. El defecto es la falta de instrucciones y la respuesta distinta del CSV incompleto, no que Excel válido falle.
- Una prueba de estado envió `REQUIERE_CORRECCION`, que no existe. La interfaz utiliza `REQUIERE_CAMBIOS`; ese valor y la aprobación devolvieron 200 en local.
- Un archivo de 21 MB devolvió 400 en lugar del 413 esperado por la prueba, con mensaje correcto y sin guardarse. Es una diferencia de contrato HTTP; el control de tamaño funciona.
- Pulsaciones muy próximas a la navegación y algunos `selectOption` no actualizaban todavía el estado de React en la herramienta. Se contrastaron con lectura de estado y teclado; no se registran automáticamente como errores del producto. El diálogo de cambios sin guardar y la persistencia del borrador quedaron confirmados.
- Configuración de semestres de ciclos y falta de contacto de ayuda: requieren confirmar los datos esperados; no se modificaron datos del servidor.

## Evidencia local y límites de validación

Suites ejecutadas de nuevo en esta revisión: **343 pruebas backend, 132 frontend y 12 del chatbot aprobadas**. No prueban por sí solas que todos los flujos integrados funcionen; varios fallos anteriores aparecen al combinar módulos.

Se registraron **210 comprobaciones HTTP** en cuatro recorridos locales, además del contraste de importación/persistencia y de la secuencia de pérdida del adjunto. Los resultados conservan las discrepancias originales, incluidas expectativas de prueba que luego se descartaron como defectos. Un HTTP 200 de importación no se consideró suficiente: se consultó si la cuenta había quedado guardada.

Archivos locales de evidencia, excluidos de Git bajo `.verification/`:

Durante la limpieza solicitada se detuvieron los tres servicios locales QA y se eliminaron la base H2 en memoria, los archivos sintéticos de subida, las dependencias Python temporales y las compilaciones reproducibles. Se conservaron los resultados, scripts y registros enumerados abajo; para repetirlos hay que iniciar nuevamente los servicios locales e instalar sus dependencias. El despliegue remoto y su evidencia de F14 permanecen sin cambios.

- `roles-api-results.json`: cinco roles, alcance, CRUD y permisos; `roles-api.mjs` permite reproducir el recorrido exclusivamente en localhost.
- `roles-extra-results.json`: pautas completas, firma, privacidad, concurrencia y permisos de comunicados.
- `roles-final-results.json`: dupla válida, estados e importaciones iniciales.
- `roles-imports-results.json`: CSV/XLSX correctos, importación incompleta/parcial y verificación de cuentas persistidas.
- `roles-files-results.json`: tipos, tamaños, adjunto del chatbot y revisión persistida.
- `roles-delete-attachment-results.json`: secuencia 200 → revisión 200 → eliminación 409 → entidad 200 → adjunto 404.
- `pacheco-delete-attachment-results.json`: misma secuencia reproducida en el servidor y registro de desactivación de cuenta/cancelación de práctica QA; no contiene contraseña ni token.
- `roles-backend-tests.log`, `roles-frontend-tests.log`, `roles-chatbot-tests.log`: suites; `roles-backend.log` incluye la clave foránea de F14 y los errores de validación.

No se comprobó entrega de correo real, Gemini con clave del servidor, restauración de respaldos ni despliegue SSH en esta revisión. Las firmas e importaciones se hicieron únicamente sobre la base local desechable. La excepción remota fue el recorrido sintético de F14 descrito arriba, tras la aclaración de Fabián. No se guardaron credenciales en el informe.

## Estado de correcciones y validación local (actualización posterior a `212535a`)

Los 14 hallazgos documentados (F01 a F14) fueron corregidos en la rama `testing` y validados mediante pruebas locales y nuevas pruebas de regresión automatizadas (**352 pruebas backend**, **140 pruebas frontend** y lint sin errores):

| ID | Estado | Resumen de corrección y regresión |
| --- | --- | --- |
| **F14** | Corregido y validado en local | `LessonPlanServiceImpl.eliminarPlanificacion` y `PortfolioServiceImpl` eliminan registros `ChatbotMessage`/`ChatbotLog`, re-vinculan versiones hijas y ejecutan el borrado físico del adjunto únicamente en `afterCommit` (solo si no existe otra referencia al mismo archivo). Regresiones de borrado con revisión IA y de preservación del archivo tras rollback (`FunctionalFindingsRegressionTest`). |
| **F06** | Corregido y validado en local | `SecurityConfig` y `NotificationServiceImpl` restringen `POST /api/notificaciones/comunicado` a `Administrador`, `Coordinador de practicas` y `Profesor de Asignatura` (con alcance acotado para profesor). Regresiones 401/403 para anónimo, alumno, colaborador y tutor. |
| **F07** | Corregido y validado en local | `GET /api/notificaciones/destinatarios` restringido a emisores autorizados y migrado a `NotificationRecipientDTO` (solo `id`, `fullName`, `email`, `role.name`, sin teléfonos ni datos sensibles) y filtrado por alcance para profesor de asignatura. |
| **F01** | Corregido y validado en local | `chatbotService.js` envía `Authorization: Bearer <token>` en `sendMessage` y `evaluateLessonPlan` y distingue errores 401/403 de fallos de conexión. Pendiente certificar Gemini real con la clave del servidor en Pacheco. |
| **F02** | Corregido y validado en local | `LessonPlanServiceImpl.obtenerPlanificacionesPorAsignacion` autoriza coordinación/administración y conserva 403 para alumnos y tutores ajenos. |
| **F03** | Corregido y validado en local | `Portafolio.jsx` carga `/api/asignaciones/{id}/ficha` en modo supervisión para el encabezado sin depender de documentos ni estado de navegación. |
| **F04** | Corregido y validado en local | `Portafolio.jsx` dirige el botón de regreso de coordinación a `/reportes` («Volver a Reportes») y mantiene `/mis-estudiantes` para docentes. |
| **F05** | Corregido y validado en local | `RubricIndicators.jsx`, `EvaluationForm.jsx` y `StudentEvaluationsPanel.jsx` identifican evaluaciones históricas sin `rubricVersion` y omiten la leyenda de escala 2–7 y el sufijo `/ 7 pts`, manteniendo nota publicada y solo lectura. |
| **F08** | Corregido y validado en local | `CsvUserBatchParser` rechaza archivos `.txt` y conserva filas incompletas para validación; `UserBatchImportServiceImpl` y `UserController` rechazan con HTTP 400 las importaciones sin registros válidos. |
| **F09** | Corregido y validado en local | `POST /api/usuarios/carga-masiva` devuelve `totalRecords`, `createdCount`, `updatedCount`, `failureCount` y `errors` por fila; `CargaMasiva.jsx` muestra instrucciones de 4 columnas, descarga de plantilla CSV y resumen detallado con errores parciales. |
| **F10** | Corregido y validado en local | `MisEstudiantes.jsx` muestra el estado deshabilitado «Sin pautas configuradas» cuando la asignatura no posee pautas profesionales, conservando acceso a evaluaciones históricas existentes. |
| **F11** | Corregido y validado en local | `NuevaPlanificacion.jsx` y `RetroPlani.jsx` enlazan todas las etiquetas de planificación con sus controles mediante `htmlFor`/`id`. |
| **F12** | Corregido y validado en local | `Evaluation.java` serializa `finalizedAt` en formato ISO-8601 UTC (`Z`); `evaluationFormat.js` convierte instantes con zona a `America/Santiago` (formato 24 h) sin alterar registros históricos sin zona. |
| **F13** | Corregido y validado en local | `LocalStorageServiceImpl` lanza `UnsupportedFileFormatException` / `BadRequestException` (HTTP 400) para extensiones prohibidas, archivos vacíos y path traversal, reservando `StorageException` (500) para fallos reales de disco. |

**Pendiente en Pacheco (tras desplegar el nuevo paquete):** verificación con Gemini real del servidor, descarga ZIP del profesor y pruebas manuales de Fabián antes de publicar en `main`. El despliegue actual `212535a` en Pacheco sigue conservando los fallos documentados hasta aplicar el nuevo paquete.

## Orden para mañana

1. ~~Corregir F14 y añadir regresión que impida perder adjuntos cuando falle una transacción.~~ **(Completado y validado en local)**
2. ~~Restringir comunicados y destinatarios (F06/F07) con pruebas de permisos por rol.~~ **(Completado y validado en local)**
3. ~~Corregir chatbot y supervisión de coordinación (F01–F04).~~ **(Completado y validado en local)**
4. ~~Corregir importación y presentación de evaluaciones (F05, F08/F09, F12).~~ **(Completado y validado en local)**
5. ~~Revisar F10/F11/F13;~~ **(Completado y validado en local)**; repetir ZIP, Gemini real y flujos afectados en Pacheco desde la red universitaria.
6. ~~Ejecutar regresiones y generar un paquete nuevo~~ **(Completado)**; subir y desplegar siguiendo [el procedimiento para Pacheco](PENDIENTES_PACHECO_2026-11-25.md), conservando `.env`, `.compose-project`, `.compose-compat` y los volúmenes existentes. Mantener `main` sin publicar hasta las pruebas manuales.
