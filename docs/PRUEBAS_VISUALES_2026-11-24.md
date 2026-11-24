# Revisión visual y adaptación a formatos

Fecha: 24 de noviembre de 2026. Rama: `testing`.

## Cobertura

Se generaron 120 capturas finales con Edge: 20 estados de pantalla en cada uno de estos formatos.

| Formato | Resolución |
| --- | --- |
| Teléfono compacto | 320 × 740 |
| Teléfono | 390 × 844 |
| Tablet vertical | 768 × 1024 |
| Tablet horizontal | 1024 × 768 |
| Escritorio | 1440 × 900 |
| Teléfono horizontal | 844 × 390 |

Se revisaron acceso, recuperación y cambio de contraseña, inicio de estudiante y coordinación, notificaciones, portafolio, usuarios y formulario de alta, reportes generales/docentes, perfil, carga masiva, asignaturas, ciclos, asignaciones, nueva planificación, RetroPlani y pauta de evaluación con sus acciones inferiores.

Los datos de prueba incluyen nombres, correos y enlaces largos, tablas con paginación, avisos sin leer y pautas oficiales. Los servicios API se simulan; estas pruebas no acreditan la configuración del correo, Gemini ni los datos reales de Pacheco.

## Problemas corregidos

- Las páginas móviles llegaban a ocupar 647 píxeles de ancho en una pantalla de 320 por correos y enlaces sin cortes. Ahora las tarjetas distribuyen el texto dentro de su ancho.
- La paginación ocultaba el botón siguiente: ahora se distribuye en varias filas y conserva los controles accesibles.
- La bandeja de notificaciones dejaba abierto el menú móvil con una segunda captura de foco. Ahora cierra el menú y mantiene el teclado dentro del diálogo.
- Los títulos, márgenes interiores, superficies, bordes y campos utilizan estilos compartidos. Se conserva la identidad azul y naranja; las tablas mantienen desplazamiento horizontal y palabras completas.
- Los formularios de acceso y recuperación comparten distribución, marca y tamaños de texto. La composición en dos columnas se reserva para pantallas amplias.
- Las pestañas y la cabecera del chat RetroPlani se distribuyen sin fragmentar sus etiquetas en móviles. Se normaliza su nombre visible.
- Botones móviles, cierre de modales y asignación rápida de puntajes ofrecen áreas táctiles de al menos 44 píxeles.

## Verificación y evidencia

`responsive-visual.spec.js` pasó en los seis formatos: sin desbordamiento de página ni errores JavaScript. También comprueba contención del mensaje del modal, foco de teclado, acceso a paginación y acciones de evaluación.

La suite de frontend pasó sus 129 pruebas (`npm test -- --maxWorkers=2`). La compilación de producción termina correctamente y el linter no presenta errores; conserva advertencias de hooks/memoización e imports en el proyecto.

Las capturas y mediciones quedan localmente en `.verification/visual/coherent/<ancho>x<alto>/`; las capturas previas están en `.verification/visual/before/`. Se inspeccionaron visualmente ejemplos de móvil, tablet, escritorio y horizontal, además de las comprobaciones geométricas automatizadas. Las capturas no se incorporan al código fuente ni al paquete del servidor.

Para repetir:

```powershell
cd frontend
$env:PLAYWRIGHT_CHANNEL = 'msedge'
$env:VISUAL_STAGE = 'coherent'
npx playwright test e2e/responsive-visual.spec.js --project=desktop-review --workers=2
```

La versión de `main` queda pendiente de la aceptación manual de esta integración.
