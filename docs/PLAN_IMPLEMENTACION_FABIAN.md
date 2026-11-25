# Plan de implementación — Fabián

Fuentes de cliente y cruce Trello: disponibles en el espacio documental externo al repositorio.

Fecha: 18 de noviembre de 2026. Numeración **Trello**, no la del catálogo `.txt` consolidado.

**Actualización de implementación:** HU44, HU35 y HU40 implementadas localmente sobre el código de Angelo (`919e0e8`). Ver [implementación y verificación](../docs/IMPLEMENTACION_HU_FABIAN.md) para cambios, configuración y pruebas. Quedan la configuración del contacto real y la comprobación en tablet física; no se han modificado estados de Trello. Los apartados de punto de partida siguientes describen la situación previa a esta implementación.

Los acuerdos confirmados y el contrato objetivo están en [ACUERDOS_IMPLEMENTACION.md](ACUERDOS_IMPLEMENTACION.md). Aplicar sus reglas de cálculo, firma/publicación y privacidad en las entregas de este plan.

## 1. Alcance y asignación confirmada

La asignación se verificó contra el [cruce actualizado](02_Historias_de_Usuario/Cruce_Trello_vs_HU.md) y su exportación JSON. Refleja esa exportación, cuya última actividad es del 11 de noviembre; no es una consulta en vivo al tablero.

| Estado de Trello | HU de Fabián | Trabajo de este plan |
|---|---|---|
| En proceso | **HU44** | Captura y cálculo por indicador. Primera prioridad por su dependencia con HU46. |
| En proceso | **HU35** | Panel estadístico para seguimiento y acreditación. |
| En proceso | **HU40** | Interfaz responsiva, tutoriales y soporte. |
| Hecho | HU2, HU5, HU6, HU9, HU19, HU24, HU28 y HU38 | Revisiones de cierre y validación operativa; reutilizar las correcciones existentes. |

Fabián confirmó que Angelo toma HU43 y HU47, además de HU46. HU43/HU47 figuraban sin asignar en la exportación; el nuevo reparto está documentado localmente y debe actualizarse en Trello. HU45 sigue sin asignar. HU30/HU31/HU39 corresponden a Sebastián.

## 2. Orden recomendado

1. Aplicar el contrato HU44–HU46–HU47 ya fijado y coordinar su migración común con Angelo.
2. Implementar HU44 y entregar el validador/cálculo que Angelo necesita para cerrar evaluaciones.
3. Completar HU35 usando datos reales del sistema; añadir resultados de HU44/HU46 cuando estén integrados.
4. Cerrar HU40 sobre las pantallas definitivas. Aplicar las mejoras de tablet al formulario de HU44 desde el principio.
5. Validar las HU hechas y completar la puesta en operación de HU38 en el servidor autorizado.

Consultar el estado ya implementado en [CORRECCIONES_HU_HECHAS.md](../docs/CORRECCIONES_HU_HECHAS.md). Los resultados anteriores de pruebas sirven como referencia de regresión, no certifican los cambios que se desarrollen con este plan.

## 3. HU44 — Evaluación numérica por indicador

### Punto de partida

Existen `EvaluationCriteria`, los DTO de criterios y la persistencia básica. El catálogo actual contiene cuatro pautas del colaborador con 19, 18, 23 y 23 indicadores. `MisEstudiantes.jsx` permite consultarlas, pero sigue capturando una nota global y enviando `criteria: []`. La API de lectura devuelve evaluaciones sin un DTO completo de indicadores.

Archivos principales:

- [MisEstudiantes.jsx](../frontend/src/pages/MisEstudiantes.jsx) y [evaluationService.js](../frontend/src/services/evaluationService.js).
- [collaborator.json](../src/main/resources/rubrics/collaborator.json) y [EvaluationRubricController.java](../src/main/java/com/ubb/practicas/controller/EvaluationRubricController.java).
- [EvaluationCriteria.java](../src/main/java/com/ubb/practicas/model/EvaluationCriteria.java), [EvaluationCriteriaDTO.java](../src/main/java/com/ubb/practicas/dto/EvaluationCriteriaDTO.java) y [EvaluationCreateDTO.java](../src/main/java/com/ubb/practicas/dto/EvaluationCreateDTO.java).

### F44-0. Aplicar los instrumentos y reglas acordados

1. La primera entrega incluye las ocho pautas profesionales: cuatro del colaborador y cuatro del tutor. Angelo incorpora HU43 y Fabián entrega el formulario/cálculo común de HU44.
2. Usar puntajes enteros 2–7 y los máximos 133, 126 y 161 según el instrumento. La versión inicial del catálogo es `2025-2-v1`.
3. Reproducir las tablas inspeccionadas en los DOCX: nota 2,0–7,0, exigencia 60%, redondeo final a un decimal mediante HALF_UP. La fórmula por tramos y los casos de prueba están en [ACUERDOS_IMPLEMENTACION.md](ACUERDOS_IMPLEMENTACION.md).
4. Conservar el resultado documentado: todos los indicadores en 2 producen aproximadamente 3,0; no sustituir la tabla por el promedio ni normalizar restando el mínimo.
5. Sin observar = pendiente en borrador. No guardar 0 ni excluir indicadores del máximo; completar todos antes de firmar. El borrador incompleto tiene `score=null`.
6. Aplicar el contrato de revisión y lectura compartido. Los borradores son privados al evaluador; la firma publica en una operación y Angelo implementa la consulta del estudiante de HU47.

**Salida:** catálogo de los ocho instrumentos y DTO conformes al acuerdo, con códigos estables, versión, escala y cálculo reproducible. La vigencia institucional de nuevas pautas se gestiona como otra versión, sin cambiar evaluaciones firmadas.

### F44-1. Modelo, lectura y cálculo en servidor

1. Añadir a los criterios un código estable y vincular la evaluación con la versión/instantánea de la rúbrica. Coordinar la migración con Angelo; no crear dos campos equivalentes para versión o estado.
2. Crear un DTO de lectura con los indicadores guardados, puntajes, nota, estado y revisión. Recuperar los borradores sin perder campos existentes de comentarios o firma.
3. Implementar un servicio propuesto `EvaluationScoringService` que cargue la definición desde el catálogo del servidor, valide los códigos, escalas y duplicados y calcule la nota según la regla confirmada.
4. El navegador envía elecciones por indicador, no una nota oficial confiable. Una modificación manual de `score` no puede alterar el cálculo del servidor.
5. Permitir borradores incompletos, con puntaje/nota aún no disponibles. La validación de completitud corresponde al cierre; compartirla con la operación HU46 de Angelo.
6. Preservar registros históricos de nota global sin fabricarles indicadores ni recalcular calificaciones ya firmadas.

**Aceptación:** se recupera exactamente lo guardado; un código o puntaje inválido es rechazado; la nota oficial es reproducible desde la versión de pauta y sus resultados.

### F44-2. Formulario por dimensiones e indicadores

1. Extraer componentes propuestos `EvaluationForm.jsx` y `RubricIndicators.jsx` para que Angelo integre la confirmación de firma sin duplicar la captura.
2. Mostrar estudiante, periodo, instrumento, área, visita y escala aplicable.
3. Organizar los indicadores por dimensión con selectores de nivel/puntaje. Mostrar una leyenda de niveles junto al formulario.
4. Mostrar progreso —por ejemplo, 15 de 19 indicadores completos—, puntaje obtenido y nota calculada cuando corresponda. Diferenciar una nota preliminar de una oficial.
5. Guardar borrador, recargarlo y continuar sin pérdida de datos. Avisar si se intenta salir con cambios no guardados.
6. Las evaluaciones firmadas son de lectura. Entregar el borrador actualizado y su revisión al diálogo HU46 de Angelo.

Conservar el comentario global existente. La nueva captura de observaciones por indicador de HU45 y la vista completa del estudiante de HU47 conservan su propia trazabilidad.

### F44-3. Pruebas y cierre

| Caso | Resultado esperado |
|---|---|
| Cada una de las ocho pautas | Texto, número de indicadores, máximo y escala coinciden con la versión adoptada. |
| Primera y segunda visita | Se usan sus definiciones respectivas; no se sobrescriben. |
| Borrador parcial y recarga | Se conservan las elecciones; no aparece una nota oficial inventada. |
| Todos los indicadores mínimos/máximos y casos intermedios | Resultado conforme a la tabla aprobada, incluido redondeo. |
| Código duplicado, ajeno, decimal prohibido o valor fuera de rango | Rechazo del servidor. |
| Nota global manipulada | No modifica la calificación oficial. |
| Pauta firmada | El formulario no permite editar y la API mantiene el bloqueo. |
| Formulario en tablet | Se pueden seleccionar todos los indicadores y guardar sin perder contexto. |

**Cierre de HU44:** captura completa, guardado/lectura y cálculo verificados para todos los instrumentos y evaluadores incluidos en el alcance acordado. La nota final ponderada de la asignatura no forma parte de esta entrega salvo validación expresa.

**Estimación orientativa:** 12–20 horas efectivas. Las reglas ya están acordadas para el prototipo; coordinar la entrega de HU43 con Angelo para integrar y probar el flujo completo del tutor.

## 4. HU35 — Panel estadístico

### Punto de partida

[Reportes.jsx](../frontend/src/pages/Reportes.jsx) muestra asignaciones y exporta notas/evidencias mediante la funcionalidad corregida de HU36. [Dashboard.jsx](../frontend/src/pages/Dashboard.jsx) tiene dos contadores básicos para el coordinador. Todavía falta un panel con métricas y definiciones de cálculo coherentes.

### F35-1. Definir métricas y alcance de consulta

| Métrica inicial propuesta | Definición verificable |
|---|---|
| Estudiantes con práctica | Cantidad de estudiantes distintos en el alcance y periodo seleccionados. No confundir con número de asignaciones. |
| Asignaciones | Número de prácticas, agrupables por ciclo, asignatura y estado. |
| Centros utilizados | Centros distintos con asignaciones del conjunto filtrado. Evitar llamarlos «activos» si el modelo no registra ese estado. |
| Evaluaciones publicadas | Evaluaciones finalizadas, contadas por ID, sin multiplicarlas al unir indicadores. |
| Instrumentos pendientes | Solo cuando se conoce el conjunto aplicable a cada práctica. No asumir ocho instrumentos en todos los ciclos. |
| Promedio de evaluaciones publicadas | Promedio simple identificado como tal, con número de observaciones. No presentarlo como nota final ponderada. |
| Planificaciones por estado | Última versión de cada planificación lógica; distinguir `APROBADO_IA` de `APROBADO` docente. |
| Seguimiento entre periodos | Series por instrumento/versión y ciclo; evitar comparar como equivalentes rúbricas de distinta escala o complejidad. |

La entrega inicial utiliza Administrador/Coordinador existentes y las métricas del cuadro anterior, conforme a [ACUERDOS_IMPLEMENTACION.md](ACUERDOS_IMPLEMENTACION.md). No se crea un rol Director en esta entrega. Una solicitud institucional posterior de métricas o roles adicionales se incorpora explícitamente, sin conceder acceso global a docentes como efecto secundario.

### F35-2. Agregación y visualización

1. Crear un DTO estadístico y un servicio de consultas. Ruta propuesta: `GET /api/reportes/estadisticas`, protegida explícitamente para las autoridades acordadas.
2. Aplicar periodo, historial, ciclo y asignatura en servidor. Reutilizar las reglas de [AssignmentScope.java](../src/main/java/com/ubb/practicas/service/AssignmentScope.java); la identidad no llega como un ID arbitrario del navegador.
3. Devolver filtros aplicados, contadores, agrupaciones y tamaño de las muestras. Resolver agregaciones con consultas adecuadas, evitando una petición por estudiante o recorrer entidades completas innecesariamente.
4. Añadir tarjetas de resumen, gráficos sencillos y una tabla consultable con acceso autorizado a evidencias. Definir estados de carga, vacío y error.
5. Reutilizar el CSV de HU36 y mantener sus filtros alineados con el panel. La interfaz debe identificar claramente qué exporta cada acción; no prometer un archivo Excel si la salida es CSV.

### F35-3. Aceptación y pruebas

- Un conjunto pequeño de datos conocido produce exactamente los contadores esperados.
- Un estudiante con dos asignaciones cuenta como un estudiante y dos asignaciones.
- Los borradores no entran en las notas publicadas; añadir indicadores no multiplica el número de evaluaciones.
- Un historial con tres versiones no cuenta como tres planificaciones vigentes.
- Los filtros son consistentes entre API, gráficos, tabla y exportación.
- Sin datos se muestra un estado vacío y los promedios no producen divisiones por cero.
- Una persona sin permisos no obtiene estadísticas globales, aunque cambie parámetros o invoque directamente la API.

**Estimación orientativa:** 8–12 horas efectivas después de acordar las métricas. El panel de asignaciones puede avanzar mientras se integra HU44/HU46; los datos de calificaciones se validan al integrarlas.

## 5. HU40 — Interfaz responsiva, tutoriales y soporte

### Punto de partida

Hay componentes comunes y clases responsivas, pero el menú lateral de [MainLayout.jsx](../frontend/src/layouts/MainLayout.jsx) mantiene ancho fijo. [Modal.jsx](../frontend/src/components/ui/Modal.jsx) soporta Escape y bloqueo de scroll; falta verificar foco, retorno al disparador y uso conjunto con formularios extensos. La configuración [Playwright](../frontend/playwright.config.js) actual solo define perfiles de escritorio.

### F40-1. Navegación y formularios en pantallas pequeñas

1. Convertir el menú lateral en una navegación adaptable, con apertura/cierre accesibles en móvil y persistencia del layout de escritorio.
2. Priorizar login, ficha/portafolio, formulario de planificación, pauta de evaluación y reportes.
3. Adaptar tablas mediante scroll dentro de su contenedor o tarjetas, manteniendo visibles las acciones. Evitar desbordamiento horizontal de toda la página.
4. Dar a las pautas controles táctiles utilizables y dimensiones/secciones claras; comprobar teclado virtual, botones de guardado y mensajes de error.
5. Revisar etiquetas, foco visible, navegación por teclado, foco dentro del modal y retorno al cerrarlo. El cierre no debe descartar silenciosamente un formulario modificado.

### F40-2. Ayuda y soporte

1. Crear una sección de ayuda breve, accesible desde la navegación, con recorridos según rol.
2. Cubrir acceso, recuperación de cuenta, carga de documentos, planificación/IA, consulta de pautas y cierre de evaluación según las funciones realmente disponibles.
3. Explicar formatos/tamaños aceptados y el significado de borrador, revisión IA y pauta firmada.
4. Mostrar el contacto del coordinador seleccionado explícitamente en la configuración: nombre y correo, con teléfono opcional. No tomar arbitrariamente la primera cuenta Coordinador. Si falta configuración, indicar que el contacto aún no está disponible; los tutoriales siguen accesibles.
5. Actualizar los tutoriales después de integrar HU44/HU46. Calendario e identidad visual de HU39 siguen siendo trabajo de Sebastián.

### F40-3. Matriz de verificación

| Tamaño de prueba | Recorrido mínimo |
|---|---|
| Móvil, 360 × 800 | Abrir menú, navegar, consultar ficha y completar campos sin perder controles. |
| Tablet, 768 × 1024, y orientación horizontal | Consultar indicadores, registrar selección, guardar y revisar la confirmación de cierre. |
| Escritorio, 1366 × 768 | Tablas, filtros, modales y ayuda completos. |
| Teclado y zoom de navegador | Foco visible, orden lógico, apertura/cierre de modal y acciones accesibles. |

Añadir perfiles móvil/tablet y pruebas de los recorridos críticos a Playwright, con datos de prueba y sin cuentas reales. Conservar capturas/trazas de los casos relevantes. La emulación debe complementarse con una comprobación en tablet real cuando esté disponible.

**Cierre de HU40:** recorridos críticos utilizables en los tamaños acordados y ayuda/soporte accesibles. **Estimación orientativa:** 8–12 horas efectivas, más la comprobación con el dispositivo disponible.

## 6. Revisión de las HU ya hechas de Fabián

| HU | Comprobación de cierre |
|---|---|
| HU2 | Importaciones CSV/Excel válidas y con errores; conservar validación de filas y permisos. |
| HU5 / HU6 | Ciclos y asignaturas persistentes; asociación correcta y restricciones al borrar datos referenciados. |
| HU9 | Duplas del periodo, dos estudiantes distintos y reglas de integridad conservadas. |
| HU19 / HU24 | Subida, descarga y reemplazo de documentos en formatos permitidos; límites y acceso ajeno denegado. |
| HU28 | Historial legible y versiones previas conservadas al editar. |
| HU38 | Conservar auditoría y copia/restauración ya probadas; reconciliar migraciones en el servidor, instalar la tarea periódica y registrar una ejecución automática real. |

Para HU38, seguir [RESPALDOS.md](../docs/RESPALDOS.md). La prueba aislada ya existe; la instalación del planificador y la comprobación en el servidor definitivo son actividades operativas pendientes, no una razón para reconstruir el respaldo. Realizarlas en el entorno y ventana autorizados. Reservar 1–3 horas de trabajo operativo, excluyendo incidencias de infraestructura.

## 7. Integración con Angelo y entregas

| Orden | Fabián | Angelo | Condición de integración |
|---|---|---|---|
| 1 | Catálogo, escala y contrato de indicadores | Contrato y metadatos del cierre | Nombres de campos, permisos y errores acordados. |
| 2 | Validador/cálculo de HU44 y DTO de lectura | HU43 y transacción, unicidad y cierre HU46 | Los ocho instrumentos comparten el cálculo autorizado del servidor. |
| 3 | Formulario por indicador | Confirmación, publicación y consulta HU47 | Guardar → recargar → completar → firmar/publicar → consultar como estudiante. |
| 4 | Estadísticas HU35 | Comprobar que el cierre alimenta los resultados oficiales | Solo evaluaciones publicadas en las métricas correspondientes. |
| 5 | Responsividad y ayuda HU40 | Revisar el recorrido de firma en tablet | Evidencia funcional y visual completa. |

Fabián lidera catálogo, cálculo y componentes de captura; Angelo lidera entidad/repositorio y operación de cierre. Integrar `EvaluationServiceImpl.java`, DTO compartidos y `MisEstudiantes.jsx` en ese orden, con cambios pequeños y revisión mutua para evitar conflictos. Las migraciones deben tener versiones únicas en la rama integrada.

Comandos de verificación desde `Proyecto_Ing.-Software/`: `./mvnw.cmd test` con Java 21; desde `frontend/`: `npm test`, `npm run build`, `npm run lint` y las pruebas Playwright que correspondan al cambio. Probar migraciones/concurrencia con MySQL, además de H2.

Cada entrega debe incluir código, criterios de aceptación comprobados, pruebas pertinentes y una demostración reproducible. Mantener como parcial cualquier alcance cuya definición o dependencia externa siga pendiente; no cambiar el estado de Trello solo por la existencia de una pantalla.

Plan complementario: [PLAN_IMPLEMENTACION_ANGELO.md](PLAN_IMPLEMENTACION_ANGELO.md).
