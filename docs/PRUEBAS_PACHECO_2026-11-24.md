# Pruebas en Pacheco y correcciones reportadas

Fecha: 24 de noviembre de 2026. Sitio revisado: `http://pacheco.chillan.ubiobio.cl:8008/`. Versión desplegada durante las pruebas: commit `5078737` de `testing`.

## Comprobaciones en el servidor real

- Acceso con las cuentas de demostración de coordinación, tutor y estudiante.
- Estadísticas: el filtro Avanzado pasó de seis asignaciones a una. Se descargó el CSV y se comprobó que contenía únicamente la asignación del ciclo seleccionado.
- Usuarios: paginación de 21 resultados y acceso a la segunda página en un ancho de 320 píxeles.
- Navegación y carga de ciclos, asignaciones, dos duplas y seis centros.
- Notificaciones: al abrir el modal desde el menú compacto, el menú se cerró y el foco quedó en el diálogo.
- Consulta de una evaluación histórica publicada y de su desglose como tutor y estudiante. Los controles de la evaluación firmada estaban deshabilitados o en modo lectura, sin acciones de guardado.
- El estudiante fue redirigido a su inicio al intentar acceder a Gestión de Usuarios.
- Se revisó la distribución con anchos de 320, 390, 768, 844, 1024 y 1440 píxeles; las mediciones realizadas no mostraron desbordamiento horizontal de página. Las tablas anchas mantienen desplazamiento interno.

No se crearon usuarios, asignaturas o periodos ni se publicaron evaluaciones o comunicados en el servidor. Se envió una consulta genérica de prueba al chatbot; mostró un error de conexión.

## Tres correcciones solicitadas por Fabián

1. **Ciclo inexistente:** se eliminó `BASICO` de las opciones compartidas de creación y edición de asignaturas. Permanecen Inicial, Intermedio y Avanzado, de acuerdo con el enum del backend.
2. **Fechas invertidas:** el formulario establece límites entre inicio y término, muestra el error y evita enviar la solicitud. El backend rechaza también la creación y actualización con término anterior al inicio, antes de guardar cambios o cerrar otros periodos. Se mantienen las fechas opcionales y se permite que coincidan.
3. **Menú lateral:** en escritorio se mantiene a la altura de la ventana y acompaña el desplazamiento del contenido; únicamente la navegación interior tiene scroll. La sección de usuario y cierre de sesión permanece visible. En el menú móvil se conserva ese mismo comportamiento.

Validación local: 132 pruebas de frontend y 343 de backend aprobadas. Cinco pruebas de navegador verificaron visibilidad y funcionamiento del cierre de sesión en 320×740, 768×1024, 844×390, 1280×720 y 1440×900, incluyendo desplazamiento de páginas largas y menús pequeños.

Estas correcciones pertenecen al paquete posterior a `5078737`; requieren subir ese paquete y reconstruir los servicios para observarlas en Pacheco.

## Otros hallazgos pendientes de corrección

- **Chatbot:** el indicador de salud carga la base documental, pero enviar una consulta desde RetroPlani falla. Al revisar el cliente se comprobó que `sendMessage` y `evaluateLessonPlan` no envían `Authorization`, aunque los endpoints del chatbot requieren autenticación. Hace falta corregir y volver a verificar el flujo completo de respuesta.
- **Supervisión desde Reportes:** coordinación accede al portafolio, pero el endpoint de planificaciones rechaza la consulta con «No tienes permisos para ver las planificaciones de esta práctica». El encabezado depende del estado de navegación o de documentos y puede quedar como «Estudiante». El botón de regreso apunta a Mis Estudiantes, una ruta de docencia que redirige a coordinación al inicio.
- **Escala de evaluaciones históricas:** el desglose muestra valores antiguos como `1 / 7 pts` y la leyenda de escala 2 a 7. Esos registros históricos no deben presentarse como si utilizaran la escala oficial actual. La nota publicada permanece en modo lectura.
- **Datos académicos:** Ciclo Inicial mostraba semestres IV a IX y Ciclo Avanzado no tenía rango definido. Se registró lo observado; no se modificaron esos datos del servidor.

Durante una navegación del estudiante, la práctica apareció temporalmente como histórica y luego como vigente tras volver a cargar la vista. La última comprobación mostró correctamente la planificación y la evaluación publicada en la vista vigente; no se atribuye una causa sin una reproducción controlada.

La publicación de una nueva versión en `main` continúa pendiente de las pruebas manuales y de resolver los hallazgos restantes.
