# Pruebas en Pacheco y correcciones reportadas

Fecha: 24 de noviembre de 2026. Sitio revisado: `http://pacheco.chillan.ubiobio.cl:8008/`. Las pruebas iniciales se realizaron con `5078737`; posteriormente se desplegó y verificó el commit `212535a` de `testing`.

## Comprobaciones en el servidor real

- Acceso con las cuentas de demostración de coordinación, tutor y estudiante.
- Estadísticas: el filtro Avanzado pasó de seis asignaciones a una. Se descargó el CSV y se comprobó que contenía únicamente la asignación del ciclo seleccionado.
- Usuarios: paginación de 21 resultados y acceso a la segunda página en un ancho de 320 píxeles.
- Navegación y carga de ciclos, asignaciones, dos duplas y seis centros.
- Notificaciones: al abrir el modal desde el menú compacto, el menú se cerró y el foco quedó en el diálogo.
- Consulta de una evaluación histórica publicada y de su desglose como tutor y estudiante. Los controles de la evaluación firmada estaban deshabilitados o en modo lectura, sin acciones de guardado.
- El estudiante fue redirigido a su inicio al intentar acceder a Gestión de Usuarios.
- Se revisó la distribución con anchos de 320, 390, 768, 844, 1024 y 1440 píxeles; las mediciones realizadas no mostraron desbordamiento horizontal de página. Las tablas anchas mantienen desplazamiento interno.

En la revisión inicial no se crearon usuarios, asignaturas o periodos ni se publicaron evaluaciones o comunicados en el servidor. Se envió una consulta genérica de prueba al chatbot; mostró un error de conexión. Posteriormente, tras aclarar Fabián que Pacheco es un entorno sin datos reales, se crearon una cuenta y práctica QA para reproducir F14: la eliminación de una planificación revisada devolvió 409, dejó la entidad y perdió su adjunto sintético (404). La cuenta quedó desactivada y su práctica cancelada. Ver el informe funcional para identificadores y evidencias.

## Tres correcciones solicitadas por Fabián

1. **Ciclo inexistente:** se eliminó `BASICO` de las opciones compartidas de creación y edición de asignaturas. Permanecen Inicial, Intermedio y Avanzado, de acuerdo con el enum del backend.
2. **Fechas invertidas:** el formulario establece límites entre inicio y término, muestra el error y evita enviar la solicitud. El backend rechaza también la creación y actualización con término anterior al inicio, antes de guardar cambios o cerrar otros periodos. Se mantienen las fechas opcionales y se permite que coincidan.
3. **Menú lateral:** en escritorio se mantiene a la altura de la ventana y acompaña el desplazamiento del contenido; únicamente la navegación interior tiene scroll. La sección de usuario y cierre de sesión permanece visible. En el menú móvil se conserva ese mismo comportamiento.

Validación local: 132 pruebas de frontend y 343 de backend aprobadas. Cinco pruebas de navegador verificaron visibilidad y funcionamiento del cierre de sesión en 320×740, 768×1024, 844×390, 1280×720 y 1440×900, incluyendo desplazamiento de páginas largas y menús pequeños.

Estas tres correcciones ya están desplegadas en Pacheco con `212535a`. Se recrearon backend y frontend, conservando la base de datos, los archivos subidos y el contenedor del chatbot.

Revalidación sobre el sitio actualizado:

- Crear y editar asignaturas muestran únicamente Inicial, Intermedio y Avanzado.
- Un borrador con inicio `2027-07-02` y término `2027-03-02` muestra «La fecha de término no puede ser anterior a la fecha de inicio», límites cruzados y campos inválidos. No se guardó el borrador.
- La API rechazó con HTTP 400 una actualización con fechas invertidas sobre el identificador inexistente `-1`, sin modificar periodos reales.
- En Inicio, con ventana de 1440×900 y desplazamiento vertical de 1326 píxeles, el menú permaneció sticky y el botón de cierre de sesión quedó visible entre las coordenadas verticales 842 y 878.
- El frontend cargó el archivo `index-BCwwASBW.js`, correspondiente al paquete desplegado.

El acceso SSH dejó de estar disponible desde la conexión actual del usuario, según lo informado por él. Esto se registra por separado del fallo del chatbot. No se realizaron nuevos intentos de despliegue después de esa indicación. El plan para retomar desde la red universitaria está en [PENDIENTES_PACHECO_2026-11-25.md](PENDIENTES_PACHECO_2026-11-25.md).

## Otros hallazgos: corregidos en local, pendientes de despliegue en Pacheco

La revisión posterior por coordinación, tutor, profesor de asignatura, profesor colaborador y alumno, con contraste local, está detallada en [PRUEBAS_FUNCIONALES_ROLES_2026-11-24.md](PRUEBAS_FUNCIONALES_ROLES_2026-11-24.md). Todos los hallazgos funcionales (F01–F14) fueron corregidos en la rama `testing` y validados localmente con **352 pruebas backend** y **140 pruebas frontend** aprobadas; el despliegue remoto `212535a` en Pacheco conserva dichos fallos hasta subir y aplicar el nuevo paquete.

- **Chatbot (F01):** corregido en `testing` (`sendMessage` y `evaluateLessonPlan` envían `Authorization` y distinguen 401/403). Pendiente validar respuesta real de Gemini en Pacheco tras desplegar.
- **Supervisión desde Reportes (F02–F04):** corregido en `testing` (coordinación puede leer planificaciones supervisadas, el encabezado carga la ficha por ID y el botón regresa a `/reportes`).
- **Escala de evaluaciones históricas y zona horaria (F05, F12):** corregido en `testing` (registros históricos sin `rubricVersion` no muestran escala 2–7 ni `/ 7 pts`; `finalizedAt` serializa en UTC `Z` y la UI convierte a `America/Santiago`).
- **Permisos de comunicados/destinatarios, importación masiva, evidencias, etiquetas y eliminación F14 (F06–F11, F13, F14):** corregidos y cubiertos con regresiones en `testing`.
- **Datos académicos:** Ciclo Inicial mostraba semestres IV a IX y Ciclo Avanzado no tenía rango definido. Se registró lo observado; no se modificaron esos datos del servidor.

Durante una navegación del estudiante, la práctica apareció temporalmente como histórica y luego como vigente tras volver a cargar la vista. La última comprobación mostró correctamente la planificación y la evaluación publicada en la vista vigente; no se atribuye una causa sin una reproducción controlada.

La publicación de una nueva versión en `main` continúa pendiente de las pruebas manuales y de resolver los hallazgos restantes.
