# Acuerdos de implementación

Fecha: 18 de noviembre de 2026. Numeración de HU de Trello.

Este documento fija las decisiones tomadas con Fabián para implementar los planes de [Angelo](PLAN_IMPLEMENTACION_ANGELO.md) y [Fabián](PLAN_IMPLEMENTACION_FABIAN.md). Registra acuerdos del equipo para el prototipo; no acredita una aprobación institucional ni modifica el tablero de Trello. Los contratos y endpoints descritos son el diseño objetivo y todavía deben implementarse.

## 1. Decisiones confirmadas con Fabián

| Tema | Acuerdo |
|---|---|
| Primera entrega de evaluaciones | Ocho pautas de práctica profesional: cuatro del colaborador y cuatro del tutor. |
| Firma | Cuenta autenticada y confirmación explícita de cierre; registrar identidad, fecha y contenido y bloquear cambios. |
| Cálculo de nota | Reproducir las tablas entregadas: escala 2,0–7,0, exigencia 60% y un decimal. Conservar que todos los indicadores en 2 producen una nota cercana a 3,0. |
| Publicación | Firmar, finalizar y publicar en una sola operación. El estudiante ve únicamente sus pautas publicadas. |
| Borradores | Privados al evaluador propietario; disponibles para retomar antes del cierre. |
| HU43 | Angelo asume el acceso del tutor y sus instrumentos. |
| HU47 | Angelo asume la consulta estudiantil de resultados publicados. |
| Ayuda de HU40 | Usar el contacto del coordinador registrado en la configuración de la plataforma. |

No se requiere un segundo ingreso de contraseña para este flujo. La identidad de la sesión y su autorización deben verificarse en servidor al guardar y al firmar.

## 2. Reparto y límites de la entrega

| Responsable | HU a completar | Responsabilidad concreta |
|---|---|---|
| Angelo | HU43 | Pautas del tutor, permisos y consulta de instrumentos asignados. |
| Angelo | HU46 | Cierre/firma/publicación, integridad, concurrencia y lectura oficial. |
| Angelo | HU47 | Pantalla del estudiante con nota, indicadores y retroalimentación publicada. |
| Fabián | HU44 | Formulario común para los ocho instrumentos, selección por indicador y cálculo en servidor. |
| Fabián | HU35 | Métricas del proceso con filtros y resultados publicados. |
| Fabián | HU40 | Navegación responsiva, ayuda y contacto de coordinación. |

HU43 y HU47 figuraban sin asignar en la exportación. Su asignación a Angelo es un acuerdo nuevo de esta conversación y debe reflejarse posteriormente en Trello.

La interfaz de evaluación reutilizará el comentario global existente. HU45 conserva su propia trazabilidad: no se considera completada por preservar ese campo. La nota final ponderada de una asignatura y la aprobación de planificaciones de HU30 pertenecen a otros alcances.

## 3. Instrumentos y escala documentada

Se inspeccionaron las imágenes de escala de notas dentro de los ocho DOCX de `04_Documentacion_Cliente/6_Práctica Profesional/6_Práctica Profesional/Pautas de evaluación Práctica Profesional 2025-2/Pautas de evaluación 2025-2/`.

Las tablas del tutor y del colaborador coinciden para la misma área y visita. Los puntajes por indicador son enteros 2, 3, 4, 5, 6 o 7. Las imágenes de conversión utilizan nota mínima 2,0, máxima 7,0, aprobación de referencia 4,0 y exigencia 60%. Este acuerdo adopta esos documentos para el prototipo; una pauta institucional posterior se incorpora como una versión nueva.

| Código de instrumento | Rol | Área | Visita | Indicadores | Puntaje máximo |
|---|---|---|---:|---:|---:|
| `COLAB_ESP_1` | Colaborador | Especialidad | 1 | 19 | 133 |
| `COLAB_ESP_2` | Colaborador | Especialidad | 2 | 23 | 161 |
| `COLAB_ORI_1` | Colaborador | Orientación | 1 | 18 | 126 |
| `COLAB_ORI_2` | Colaborador | Orientación | 2 | 23 | 161 |
| `TUTOR_ESP_1` | Tutor | Especialidad | 1 | 19 | 133 |
| `TUTOR_ESP_2` | Tutor | Especialidad | 2 | 23 | 161 |
| `TUTOR_ORI_1` | Tutor | Orientación | 1 | 18 | 126 |
| `TUTOR_ORI_2` | Tutor | Orientación | 2 | 23 | 161 |

Versión inicial del catálogo: `2025-2-v1`. Cada indicador tendrá código estable dentro de su pauta, por ejemplo `A1`, además de dimensión, orden y texto oficial. Las ocho pautas corresponden a práctica profesional; no deben asignarse automáticamente a todos los ciclos ni confundirse con TANEP o informes finales.

Un indicador sin observar queda sin puntaje en el borrador. No se convierte en 0 ni se elimina del denominador. Todos los indicadores deben tener un valor permitido para firmar. Incorporar una alternativa «No aplica» requeriría una regla académica distinta y una nueva decisión.

### Cálculo que debe reproducir las tablas

Sean `P` la suma de puntajes, `M` el máximo de la pauta y `T = 0,60 × M`:

```text
Si P ≤ T: nota sin redondear = 2 + 2 × P / T
Si P > T: nota sin redondear = 4 + 3 × (P − T) / (M − T)
```

Esta fórmula por tramos reproduce los valores cotejados en las imágenes; la referencia de aceptación son las tablas entregadas. Implementar con decimales exactos, redondeando solo el resultado final a un decimal mediante HALF_UP. Conservar también puntaje obtenido y máximo para trazabilidad.

| Máximo | Puntaje obtenido | Nota esperada |
|---:|---:|---:|
| 133 | 38 — todos los indicadores en 2 | 3,0 |
| 133 | 78 | 4,0 |
| 133 | 80 | 4,0 |
| 133 | 133 | 7,0 |
| 126 | 36 — todos los indicadores en 2 | 3,0 |
| 126 | 74 | 4,0 |
| 126 | 76 | 4,0 |
| 126 | 126 | 7,0 |
| 161 | 46 — todos los indicadores en 2 | 3,0 |
| 161 | 95 | 4,0 |
| 161 | 97 | 4,0 |
| 161 | 161 | 7,0 |

La tabla de conversión empieza en 0 puntos, pero un formulario completo con indicadores de mínimo 2 no puede sumar 0. No restar los mínimos ni usar el promedio de indicadores como sustituto de la tabla.

Una nota redondeada 4,0 puede corresponder a un puntaje levemente inferior al 60%. Conservar nota y porcentaje como datos distintos; este flujo registra evaluaciones de aula y no decide la aprobación final de toda la práctica o asignatura.

Los borradores incompletos muestran progreso y puntaje parcial; `score` será `null` hasta disponer de todos los indicadores. Una pauta completa todavía sin firmar puede mostrar una nota preliminar, identificada como tal.

## 4. Permisos y estados

| Operación | Quién puede realizarla |
|---|---|
| Crear, consultar y editar un borrador | Evaluador propietario, con rol y asignación correspondientes al instrumento. |
| Firmar/publicar | Ese evaluador, autenticado y actualmente autorizado. |
| Consultar una pauta publicada | Estudiante de la práctica, equipo docente asignado y autoridades con alcance autorizado. |
| Consultar el borrador de otro evaluador | Denegado, también al profesor de asignatura por el solo hecho de supervisar la práctica. |
| Modificar una pauta firmada | Denegado por las APIs de esta entrega. |
| Cambiar asignaciones o roles | Según las funciones administrativas existentes; no transfiere automáticamente la propiedad de un borrador. |

`isFinalized=false` significa borrador y `isFinalized=true` significa firmada/publicada. No se añade un segundo estado de publicación que pueda contradecirlo. La interfaz deriva las etiquetas «Borrador» y «Firmada» de ese campo.

Guardar un borrador no publica ni firma. Firmar es una transición atómica y definitiva dentro de este alcance. No se implementa reapertura ni corrección posterior mediante sobrescritura; si el cliente necesita rectificaciones, se definirá un procedimiento adicional con trazabilidad.

## 5. Contrato técnico HU44–HU46–HU47

### Identidad y persistencia

- Una evaluación por `practiceAssignmentId` + `type`. Los tipos distinguen rol, área y visita.
- `rubricVersion` identifica la definición utilizada. El servidor selecciona y valida la plantilla aplicable.
- `revision` es la revisión de concurrencia del registro, distinta de la versión de rúbrica.
- `criteria` contiene `criterionCode` y `points`; la lectura incorpora también texto, dimensión y orden oficiales. Se preservan los campos de observaciones ya existentes.
- `totalPoints`, `maxPoints` y `score` son calculados por servidor.
- `qualitativeComments` conserva el comentario global existente.
- `isFinalized`, `finalizedAt` y la identidad del firmante describen el cierre. Fechas de API en UTC e ISO 8601; la interfaz las presenta en la zona del usuario.
- La pauta firmada guarda una instantánea de rúbrica, elecciones, configuración de cálculo y resultado. Cambios posteriores del catálogo no alteran el historial.

### Endpoints objetivo

| Método y ruta | Uso |
|---|---|
| `GET /api/evaluaciones/pautas?assignmentId={id}` | Catálogo aplicable al rol y práctica autorizados. |
| `POST /api/evaluaciones` | Crear o actualizar únicamente un borrador. |
| `GET /api/evaluaciones/{id}` | DTO completo, sujeto a privacidad de borradores y publicación. |
| `GET /api/evaluaciones/asignacion/{id}` | Listado filtrado por solicitante y estado visible. |
| `POST /api/evaluaciones/{id}/finalizar` | Firmar, cerrar y publicar atómicamente. |

El guardado recibe asignación, tipo, versión de rúbrica, indicadores, comentarios y `expectedRevision`. Para crear un registro, `expectedRevision` es `null`; para actualizar debe coincidir con la revisión vigente. El servidor devuelve el ID y la nueva revisión.

Ejemplo de borrador parcial:

```json
{
  "practiceAssignmentId": 123,
  "type": "COLAB_ESP_1",
  "rubricVersion": "2025-2-v1",
  "expectedRevision": null,
  "criteria": [
    { "criterionCode": "A1", "points": 5 },
    { "criterionCode": "A2", "points": 6 }
  ],
  "qualitativeComments": "Observación general del evaluador"
}
```

Ejemplo de confirmación de cierre:

```json
{
  "expectedRevision": 3,
  "confirmed": true
}
```

La confirmación no recibe identidad de firmante ni una nota oficial confiable del cliente. El servidor obtiene la cuenta, recalcula, verifica completitud y firma la versión persistida que el usuario revisó. La UI guarda primero los cambios pendientes y muestra ese resultado antes de confirmar.

El campo histórico `digitalSignature` puede mantenerse en respuestas de compatibilidad, pero el servidor registra la identidad de la cuenta y no acepta un nombre libre como prueba de quién firmó. El antiguo `POST` con `isFinalized=true` debe dejar de ser una vía alternativa de cierre; migrar su consumo y rechazarlo con validación clara.

### Concurrencia y errores

Unicidad SQL por asignación/tipo; bloqueo transaccional de la asignación al crear/guardar/cerrar; comprobación de `expectedRevision` antes de aplicar cambios. Los borradores y sus criterios se guardan en una transacción.

| Código HTTP | Situación |
|---|---|
| 400 | Indicadores inválidos/incompletos para cierre, escala incorrecta, confirmación ausente o combinación de instrumento inválida. |
| 401 | Sesión ausente o inválida. |
| 403 | Rol, participación o propiedad insuficientes. |
| 404 | Recurso inexistente. |
| 409 | Revisión desactualizada, registro ya firmado o conflicto de unicidad concurrente. |

Una repetición de firma sobre un registro cerrado devuelve 409 y no genera otro cierre. Ante un conflicto, la UI recupera la versión actual para que el usuario la revise; no reintenta sobrescribirla automáticamente.

La instantánea de cierre tendrá huella SHA-256 calculada sobre una representación canónica de sus datos. Auditoría debe identificar la finalización/publicación efectiva; los intentos rechazados conservan su resultado de acceso sin registrar contraseñas ni tokens.

## 6. Consulta estudiantil HU47

Angelo añadirá «Mis evaluaciones» al portafolio. Mostrará instrumento, área/visita, evaluador, fecha de publicación, nota, puntaje y observaciones, con acceso al desglose oficial en lectura.

Las evaluaciones no publicadas no muestran notas parciales ni comentarios de borrador al estudiante. Puede indicarse que un instrumento esperado aún no está publicado, sin revelar contenido privado. El filtro se aplica en servidor y también al invocar directamente el detalle por ID.

El historial usa la instantánea de la evaluación firmada, no una plantilla actual que pueda haber cambiado. No se añade un botón de publicación a cargo del profesor: la firma del evaluador es el evento de publicación acordado.

## 7. Reglas técnicas para HU35 y HU40

HU35 usará Administrador y Coordinador existentes para el panel global. No se crea un rol Director de Carrera dentro de esta entrega. Las consultas respetan el alcance del solicitante y filtros aplicados.

Métricas iniciales: estudiantes distintos, asignaciones, centros utilizados, evaluaciones publicadas, instrumentos configurados pendientes, promedio simple de evaluaciones publicadas por instrumento/versión y planificaciones por su última versión. No confundir promedio simple con nota final ponderada ni validación IA con aprobación docente. Las ocho pautas esperadas se contabilizan solo en las prácticas profesionales donde sean aplicables.

HU40 cubrirá móvil de 360 × 800, tablet de 768 × 1024 y orientación horizontal, y escritorio de 1366 × 768. Incluirá navegación, formularios, modales, tablas, ayuda por rol y contacto del coordinador configurado.

La configuración debe identificar explícitamente el contacto de coordinación para ayuda: nombre y correo, con teléfono opcional. No seleccionar arbitrariamente la primera cuenta con rol Coordinador ni inventar un correo. Si falta configuración, indicar que el contacto aún no está disponible y conservar el acceso a los tutoriales. Este contacto atiende la coordinación del prototipo; no implica que exista un servicio técnico institucional.

## 8. Compatibilidad e integración

Conservar evaluaciones históricas y sus notas. Los tipos genéricos anteriores no se convierten automáticamente a «primera visita» ni se les agregan indicadores ficticios. Una migración informa duplicados antes de imponer la unicidad; no los borra automáticamente.

Orden de entregas:

1. Angelo y Fabián cierran el DTO y la migración común con estos nombres y reglas.
2. Angelo incorpora el acceso/catálogo del tutor de HU43; Fabián implementa el cálculo y formulario de los ocho instrumentos de HU44.
3. Angelo integra el cierre/publicación de HU46 y la consulta de HU47.
4. Fabián completa estadísticas y valida el recorrido final en móvil/tablet, con ayuda y contacto configurado.

Cada persona mantiene la propiedad de sus componentes. En archivos compartidos se integran primero contrato/modelo, después captura/cálculo y finalmente firma/consulta. Verificar versiones únicas de migración en la rama integrada.

## 9. Criterios de comprobación del acuerdo

- Ocho instrumentos aplicables con sus indicadores, máximos y escala correctos.
- Valores de nota de la tabla anterior reproducidos y casos de borde cotejados contra las imágenes originales.
- Borrador parcial recuperable; datos privados al evaluador.
- Pauta incompleta, ajena o con puntajes manipulados no puede firmarse.
- Firmar publica exactamente la revisión confirmada, con identidad y fecha de servidor.
- Dos solicitudes concurrentes no crean duplicados ni alteran una pauta firmada.
- El estudiante consulta sus resultados publicados y no los de otro estudiante ni borradores propios o ajenos.
- Cambiar la plantilla no cambia lo ya firmado.
- Estadísticas usan resultados publicados y datos del periodo seleccionado.
- La ayuda muestra el contacto explícitamente configurado y el flujo de evaluación funciona en tablet.

Estos acuerdos dejan definidos el alcance, cálculo, firma, publicación, privacidad, reparto y contrato. La aceptación institucional de las pautas usadas, la instalación operativa y cualquier alcance adicional se documentarán por separado; no cambian silenciosamente las reglas de esta entrega.
