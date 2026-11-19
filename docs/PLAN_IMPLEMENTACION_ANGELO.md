# Plan de implementación — Angelo

Fuentes de cliente y cruce Trello: disponibles en el espacio documental externo al repositorio.

Fecha: 18 de noviembre de 2026. Numeración **Trello**, no la del catálogo `.txt` consolidado.

Los acuerdos confirmados y el contrato objetivo están en [ACUERDOS_IMPLEMENTACION.md](ACUERDOS_IMPLEMENTACION.md). Ese documento fija escala, firma, privacidad, publicación y las nuevas asignaciones de HU43/HU47.

## 1. Alcance y asignación confirmada

La asignación se verificó contra el [cruce actualizado](02_Historias_de_Usuario/Cruce_Trello_vs_HU.md) y su exportación JSON. Refleja esa exportación, cuya última actividad es del 11 de noviembre; no es una consulta en vivo al tablero.

| Estado de Trello | HU de Angelo | Trabajo de este plan |
|---|---|---|
| En proceso | **HU46: evaluación finalizada y firmada** | Completar firma, cierre, publicación e integridad. |
| Por hacer; nueva asignación acordada | **HU43: pautas del tutor** | Incorporar acceso e instrumentos del tutor. |
| Por hacer; nueva asignación acordada | **HU47: resultados del estudiante** | Consultar las evaluaciones propias publicadas. |
| Hecho | HU1, HU3, HU8, HU10, HU11, HU12, HU14, HU21, HU25 y HU42 | Revisar regresiones y evidencia de cierre sobre el código corregido; no reconstruirlas desde cero. |

HU43 y HU47 figuraban sin responsable en la exportación; Fabián confirmó su asignación a Angelo el 18 de noviembre. La actualización está registrada localmente y todavía debe reflejarse en Trello. HU45 sigue sin asignar; HU30 y HU31 corresponden a Sebastián.

## 2. Punto de partida real

Ya existen captura de firma, guardado de borradores, bloqueo secuencial de una evaluación finalizada y validación del rol y participación del evaluador. Las cuatro pautas profesionales del colaborador tienen tipos distintos (`COLAB_ESP_1`, `COLAB_ESP_2`, `COLAB_ORI_1`, `COLAB_ORI_2`). Conservar esas correcciones descritas en [CORRECCIONES_HU_HECHAS.md](../docs/CORRECCIONES_HU_HECHAS.md).

La brecha de HU46 está en el cierre completo: actualmente se acepta una nota global del navegador, no se exige un conjunto completo de indicadores oficiales y no hay protección suficiente contra cierres/escrituras concurrentes. `digitalSignature` es un texto ingresado por el usuario; no confundir esa captura con una verificación criptográfica.

Archivos principales:

- [EvaluationServiceImpl.java](../src/main/java/com/ubb/practicas/service/impl/EvaluationServiceImpl.java).
- [EvaluationController.java](../src/main/java/com/ubb/practicas/controller/EvaluationController.java) y [EvaluationRepository.java](../src/main/java/com/ubb/practicas/repository/EvaluationRepository.java).
- [Evaluation.java](../src/main/java/com/ubb/practicas/model/Evaluation.java), [EvaluationCriteria.java](../src/main/java/com/ubb/practicas/model/EvaluationCriteria.java) y [EvaluationCreateDTO.java](../src/main/java/com/ubb/practicas/dto/EvaluationCreateDTO.java).
- [MisEstudiantes.jsx](../frontend/src/pages/MisEstudiantes.jsx) y [evaluationService.js](../frontend/src/services/evaluationService.js).

## Acceso del tutor HU43

1. Incorporar `TUTOR_ESP_1`, `TUTOR_ESP_2`, `TUTOR_ORI_1` y `TUTOR_ORI_2` con las definiciones oficiales y versión `2025-2-v1`.
2. Servir las pautas aplicables a la práctica profesional y al tutor autenticado. Reutilizar el catálogo y sus metadatos con HU44; no copiar una rúbrica a mano en el frontend.
3. Comprobar asignación, rol y propiedad tanto en consulta como en escritura. Mantener los borradores privados a su propietario y las pautas publicadas accesibles según el contrato.
4. Conectar el listado del tutor con el formulario común de Fabián; permitir elegir área/visita sin sobrescribir otra pauta.
5. Conservar los tipos genéricos antiguos como históricos, sin asignarles automáticamente un área o visita.

**Aceptación:** el tutor asignado accede a sus cuatro instrumentos profesionales; otra cuenta no puede acceder a sus borradores ni evaluar estudiantes ajenos. La primera/segunda observación utiliza sus indicadores y máximos correctos.

## 3. Orden de implementación de HU46

### A0. Acordar el contrato con Fabián

Aplicar el contrato ya fijado en [ACUERDOS_IMPLEMENTACION.md](ACUERDOS_IMPLEMENTACION.md) antes de modificar en paralelo los DTO, la entidad o `MisEstudiantes.jsx`. HU44 de Fabián proveerá los resultados por indicador y el cálculo validado por servidor; HU46 los utilizará para firmar y publicar en una sola operación.

| Dato | Regla fijada |
|---|---|
| Identidad de pauta | Asignación + tipo de instrumento. Cada área/visita ocupa su registro. |
| Plantilla | Código y versión de rúbrica, con indicadores y escala definidos por servidor. |
| Indicadores | Código estable, puntaje y campos existentes que deban preservarse. El texto del criterio no funciona como identificador. |
| Calificación | `score` calculado por servidor con la conversión confirmada del instrumento. El cliente no puede imponer la nota oficial. |
| Estado | Mantener `isFinalized` como fuente de verdad; la UI puede derivar «Borrador»/«Firmada». |
| Concurrencia | Revisión del registro y error 409 para una actualización desactualizada o un intento de alterar una pauta cerrada. |
| Firma | Identidad autenticada del evaluador, declaración/confirmación, fecha de cierre y contenido firmado. |
| Respuesta | DTO con identidad, versión de plantilla, indicadores, nota, estado y metadatos de cierre. |

**Decisiones confirmadas con Fabián:** ocho pautas profesionales; puntajes enteros 2–7; tablas 2,0–7,0 con exigencia 60% y un decimal; firma mediante cuenta autenticada y confirmación; publicación al firmar; borradores privados al evaluador. Se inspeccionaron las tablas de los DOCX originales y se acordó reproducirlas para el prototipo. La nota final ponderada de la asignatura conserva trazabilidad pendiente y no se agrega dentro de HU46.

**Salida de A0:** DTO y migración conformes al contrato fijado: `rubricVersion`, `revision`, `criterionCode`, `points`, nota calculada, `isFinalized` y metadatos de cierre. Guardar usa `expectedRevision`; finalizar recibe `expectedRevision` y `confirmed`. No añadir una publicación separada ni aceptar una nota o identidad oficial impuestas por el navegador.

### A1. Persistencia e integridad del cierre

1. Añadir los metadatos acordados: versión de rúbrica y su definición usada, identidad del firmante, fecha de cierre y revisión del registro. Guardar una instantánea suficiente para que editar una plantilla futura no cambie la evaluación firmada.
2. Añadir unicidad para asignación + tipo de pauta, coherente con los tipos actuales. Antes de aplicar la restricción, detectar duplicados y presentar un informe; no eliminar ni fusionar calificaciones automáticamente.
3. Serializar el guardado/cierre de una pauta mediante bloqueo transaccional de su asignación, incluyendo la creación del primer borrador. Añadir control de revisión para detectar formularios abiertos con datos antiguos.
4. Crear una migración nueva después de las existentes. Revisar la secuencia en la rama integrada; no reutilizar ni editar una migración ya aplicada.
5. Conservar las evaluaciones históricas finalizadas y sus notas originales. Los registros antiguos sin plantilla deben identificarse como históricos/manuales, sin inventar indicadores ni recalcularlos.

**Aceptación:** no pueden existir dos evaluaciones activas para el mismo instrumento y asignación; una plantilla nueva no altera una pauta firmada; la migración conserva el historial.

### A2. Unificar la operación de finalizar

Diseño propuesto: mantener el guardado de borrador y añadir `POST /api/evaluaciones/{id}/finalizar`. La ruta nueva es una propuesta de implementación, no un endpoint existente.

El servicio de cierre debe realizar dentro de una misma transacción:

1. Bloquear/cargar la pauta y comprobar la revisión esperada.
2. Obtener el evaluador desde la sesión y validar rol, participación y correspondencia del instrumento. No tomar la identidad del firmante desde un ID enviado por el navegador.
3. Rechazar si está finalizada o pertenece a otro evaluador sin una regla explícita de transferencia.
4. Validar indicadores completos, no duplicados, pertenecientes a la versión oficial y dentro de su escala. Reutilizar el validador/cálculo de HU44.
5. Calcular en servidor la nota oficial y comprobar que la tabla de conversión está configurada.
6. Registrar firma, identidad y fecha; cerrar y publicar el registro en la misma transacción; devolver el DTO completo.

Migrar el frontend al endpoint de finalizar y rechazar `isFinalized=true` en el `POST` de borradores. **No puede quedar como una segunda vía que salte estas validaciones.** Un error debe revertir también los cambios de indicadores; no dejar una pauta parcialmente cerrada.

**Aceptación:** manipular la nota, cambiar el ID o usar la ruta antigua no permite firmar contenido inválido ni modificar una evaluación finalizada.

### A3. Confirmación y vista de pauta firmada

1. Extraer un componente propuesto `FinalizeEvaluationDialog.jsx` para mostrar instrumento, estudiante, puntajes y nota calculada antes de firmar.
2. Identificar claramente al firmante autenticado y exigir confirmación de cierre. El nombre escrito no reemplaza la identidad registrada por el servidor.
3. Deshabilitar el botón mientras se procesa y gestionar un 409 mostrando que la pauta cambió o ya fue finalizada; recuperar su estado actual.
4. Mostrar la pauta cerrada en modo lectura con fecha, firmante y desglose. Quitar las acciones de edición.
5. Mantener separadas firma/cierre y aprobación de planificaciones: HU30 pertenece a otro flujo y responsable.

**Aceptación:** el usuario ve qué está firmando, una doble pulsación no duplica el cierre y al recargar se conserva el estado oficial. La misma transacción de firma publica la pauta: el estudiante ya puede consultarla mediante HU47.

### A4. Pruebas de cierre

| Caso | Resultado esperado |
|---|---|
| Pauta completa, evaluador asignado y revisión vigente | Cierre y nota oficial correctos. |
| Indicador faltante, repetido, ajeno o fuera de escala | Rechazo sin cambios parciales. |
| Nota enviada por el cliente distinta del cálculo | El cliente no altera la nota oficial. |
| Firma/confirmación ausente | Rechazo del cierre. |
| Usuario ajeno, estudiante o rol incorrecto | Acceso denegado. |
| Guardar sobre una evaluación finalizada | 409 y contenido intacto. |
| Dos cierres simultáneos | Un solo cierre efectivo; sin duplicados ni cambios posteriores. |
| Guardado de borrador concurrente con firma | El borrador no sobrescribe la pauta oficial. |
| Cambio posterior de plantilla | Lectura histórica sin cambios. |

Probar la concurrencia y las restricciones SQL con MySQL 8.4, además de JUnit/Mockito. Reutilizar el entorno aislado; no probar una migración destructiva sobre datos habituales. Registrar evidencia de la UI y del contenido persistido después del cierre.

## Consulta de resultados HU47

1. Añadir «Mis evaluaciones» al portafolio del estudiante con filtros de práctica/periodo.
2. Usar un listado y detalle filtrados en servidor: solo sus evaluaciones firmadas/publicadas, nunca notas parciales o comentarios de borrador.
3. Mostrar instrumento, área, visita, evaluador, fecha de publicación, nota, puntaje obtenido/máximo y observaciones.
4. Abrir el desglose por dimensiones e indicadores desde la instantánea firmada, en modo lectura.
5. Distinguir «pendiente de publicación» sin revelar contenido privado. No añadir otro paso de publicación por el profesor de asignatura.
6. Probar consulta por ID ajeno, borrador propio, pauta recién firmada e historial cuya plantilla haya cambiado.

**Aceptación:** firmar una pauta la hace visible al estudiante correcto; los borradores y las evaluaciones de otros estudiantes siguen protegidos incluso al llamar directamente a la API.

## 4. Revisión de las HU ya hechas de Angelo

| HU | Comprobación de cierre |
|---|---|
| HU1 / HU3 | Crear, editar y desactivar usuarios; preservar restricciones de rol y acceso. |
| HU8 | Asignaciones correctas por estudiante y periodo; rechazo de referencias inválidas. |
| HU10 / HU11 / HU12 | Listado propio, semestre vigente e historial; consultas por IDs ajenos denegadas. Verificar los horarios solicitados por HU12: la corrección de listados no acredita ese dato por sí sola. |
| HU14 | Nombres y correos correctos en ficha; acceso propio y manejo de personas aún no asignadas. |
| HU21 | Descarga y eliminación dentro del portafolio autorizado; documentos de otros estudiantes protegidos. |
| HU25 | Inclusión, recursos y evaluación preservados al crear/editar; mínimos verificados al pasar a revisión IA. |
| HU42 | Cuatro instrumentos del colaborador coexistentes, por área y visita; conservar sus indicadores originales y el alcance acordado por asignatura. |

Las correcciones ya implementadas no implican repetir trabajo. Si esta revisión descubre un incumplimiento adicional, registrarlo como incidencia de su HU hecha, separado del desarrollo de HU46.

## 5. Coordinación y entregas

| Entrega | Contenido | Dependencia |
|---|---|---|
| A-1 | Contrato, modelo y migración de cierre | Acuerdo A0 con Fabián. |
| A-2 | HU43: acceso y catálogo del tutor; servicio de firma, permisos y concurrencia | Catálogo común y validador/cálculo de HU44. |
| A-3 | Diálogo de cierre y HU47 de consulta estudiantil | Formulario por indicador de Fabián; firma/publicación única. |
| A-4 | Evidencia de cierre y regresión de HU hechas | Integración sobre MySQL y frontend. |

Angelo lidera entidad/repositorio y operación de cierre. Fabián lidera catálogo, validación/cálculo de indicadores y componentes de captura. Para `EvaluationServiceImpl.java`, DTO compartidos y `MisEstudiantes.jsx`, integrar cambios en ese orden y evitar dos refactorizaciones simultáneas. Conservar los cambios locales existentes al iniciar cada entrega.

**Estimación orientativa:** HU43, 4–6 horas; HU46, 12–20 horas; HU47, 4–6 horas; revisión de HU hechas, 3–5 horas. Los acuerdos ya están fijados para el prototipo. No incluye HU45 ni espera de validaciones institucionales adicionales.

**Cierre de HU46:** migración comprobada, pruebas anteriores aprobadas, recorrido de firma verificable y contenido oficial inalterable por las APIs. Si faltan instrumentos o reglas externas del alcance acordado, registrar la entrega como parcial y mantener visible la dependencia.

Plan complementario: [PLAN_IMPLEMENTACION_FABIAN.md](PLAN_IMPLEMENTACION_FABIAN.md).
