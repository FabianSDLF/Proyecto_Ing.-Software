# Correcciones de HU marcadas como Hecho

Fecha: 18 de noviembre de 2026. Numeración de Trello según la exportación identificada en `../../02_Historias_de_Usuario/Cruce_Trello_vs_HU.md`. Este documento registra cambios locales; no modifica tarjetas ni acredita un despliegue en el servidor de la universidad.

| HU | Corrección implementada |
|---|---|
| HU7 | Vista vigente por defecto e historial explícito en asignaciones, listados docentes, portafolio, ficha y reportes. Los periodos cerrados no aparecen como prácticas vigentes aunque la asignación conserve estado ACTIVA. |
| HU13 / HU14 | Ficha reutilizable en dashboard y portafolio con centro, RBD, dirección, comuna, contacto, profesor de asignatura, colaborador y tutor. Usa `fullName`. Consultas propias y ficha protegidas por participación; los endpoints antiguos de listados docentes también validan al solicitante. |
| HU25 | Campo explícito de inclusión/adaptaciones PIE/NEE/DUA en creación, edición y lectura. Recursos y evaluación obligatorios en el formulario. La revisión de una versión guardada verifica los mínimos en backend; los borradores siguen separados de la revisión docente. |
| HU27 | Revisión de IA sobre el contenido guardado, persistida en `ChatbotLog`, vinculada a versión y huella de contenido. Puede recuperarse al recargar. Envío al docente mediante comando separado que verifica la revisión vigente. Editar genera un borrador nuevo; comentar no equivale a enviar. El resultado local/fallback no certifica validación IA. |
| HU34 | ZIP acotado a cursos del profesor o alcance administrativo, con filtro opcional de asignatura/periodo e historial explícito. Usa el servicio de almacenamiento configurado, no rutas físicas arbitrarias ni todos los informes del sistema. |
| HU36 | CSV de notas publicadas, resultados por indicador y enlaces a evidencias, limitado a prácticas visibles para el docente/autoridad. Filtros por periodo y asignatura; campos entre comillas y protección contra fórmulas de hoja de cálculo. Acceso a reportes para docentes y administradores. |
| HU38 | Auditoría persistente de operaciones de escritura y login, consulta administrativa paginada y sin API de modificación. Respaldo de MySQL y archivos con hashes y retención; scripts de ejecución, restauración e instalación explícita de una tarea diaria. Prueba real de restauración completada. |
| HU41 | Chatbot autenticado mediante validación de sesión en el backend. Conversaciones separadas por usuario y sesión; reinicio de conversación efectivo. Configuración de clave limitada al Administrador. Mantiene los cambios locales previos de fallback de modelos. |
| HU42 | Acceso a las cuatro pautas profesionales del colaborador, separadas por área y visita, con los indicadores originales: 19, 18, 23 y 23. Persistencia diferenciada de las cuatro pautas y autorización según rol/participación. Se dejaron de enviar puntajes constantes ficticios desde el formulario. |

## Límites de alcance

Las pautas se pueden consultar y registrar mediante el flujo existente de nota global. La captura interactiva por indicador y su cálculo corresponden a HU44, que sigue En proceso. La escala y conversión definitiva de cada instrumento debe confirmarse con el cliente; no se agregó una fórmula universal ni nota final ponderada.

Tampoco se dieron por cerrados el diálogo reflexivo obligatorio con validación de respuestas, la aprobación humana de HU30, comentarios por sección de HU31, alertas HU29/HU32, las pautas de tutor de HU43 ni la vista estudiantil completa de HU47. El diálogo reflexivo carece de tarjeta específica inequívoca en el cruce y no se considera implementado por el solo envío a IA. La revisión de HU27 automatiza la comprobación inicial y su trazabilidad, no autoriza dictar la clase.

No se usaron claves reales de Gemini durante la validación. El acceso al proveedor en modo real conserva su configuración y la ruta de fallback, pero la evaluación pedagógica real requiere una prueba con la cuenta institucional configurada.

## Verificación

- Backend con Java 21: `./mvnw.cmd -o test`: **300 pruebas, sin fallos ni errores**. Se añadió precarga explícita del agente Mockito para evitar el bloqueo de auto conexión de la JVM observado en este equipo.
- Frontend: Vitest **121 pruebas aprobadas**, compilación de producción correcta. Se actualizaron selectores antiguos de pruebas para roles de pestaña, encabezados, importación de toast y respuestas simuladas consistentes. Lint termina con advertencias existentes; la compilación advierte del tamaño del bundle.
- Chatbot, dentro del contenedor aislado: **12 pruebas aprobadas**, incluidas autenticación, denegación de configuración a estudiantes, aislamiento de historial y fallback.
- MySQL 8.4: **cinco migraciones Flyway aplicadas** desde una base vacía y validación Hibernate correcta.
- Recorrido HTTP: **14 comprobaciones aprobadas**, incluidas ficha, rechazo de consultas ajenas, exportación, pautas coexistentes, revisión persistida, bloqueo de envío con fallback y consulta administrativa de auditoría.
- Respaldo/restauración: snapshot `snapshot-20261118T121845Z-30893` restaurado en un segundo MySQL vacío. Coincidieron **21 usuarios, 3 documentos, 3 evaluaciones y 17 eventos de auditoría** al comparar las bases. Los archivos coinciden por SHA-256. Se inició el backend sobre la base restaurada y se verificaron login y descarga del archivo ficticio del portafolio.

Las pruebas utilizaron contenedores, puertos y archivos aislados; no se ejecutaron sobre los datos habituales del proyecto. Los contenedores de prueba se detienen al terminar y se conservan los snapshots para revisión.

## Puesta en marcha

Consultar `RESPALDOS.md` antes de actualizar una instalación existente: se resolvió la duplicación de versión Flyway V2 conservando feedback en V2 y moviendo ciclos a V4. Si el servidor registró la antigua V2 de ciclos, su historial requiere reconciliación explícita; no se hizo un `repair` automático. Docker ahora valida migraciones y esquema.

La tarea de respaldo diario está preparada en `scripts/Install-BackupSchedule.ps1`; no se instaló en este computador ni en el servidor de producción. Debe ejecutarse y verificarse bajo la cuenta operativa del servidor en una ventana de mantenimiento.
