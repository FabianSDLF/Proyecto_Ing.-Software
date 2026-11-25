# Despliegue de testing ceedbb3 en Pacheco

Realizado el 24 de noviembre de 2026 (America/Santiago). Arranque confirmado a las `2026-11-25T00:23:43Z` / 21:23 de Santiago.

## Estado para Antigravity

**La versión ya está subida y desplegada.** Continuar con las pruebas funcionales en navegador y API que Fabián encargó a Antigravity. Codex comprobó únicamente el arranque, la publicación del frontend, los volúmenes y el estado de configuración del chatbot después del despliegue; no repitió las pruebas F01–F14 ni los recorridos por roles.

- Sitio: http://pacheco.chillan.ubiobio.cl:8008/.
- Commit de aplicación: `ceedbb319ba2ccbde285a0b5a93596a6feb899aa`, rama `testing`.
- Directorio remoto: `~/pitagoras-testing-ceedbb3/pitagoras`.
- ZIP: `pitagoras-pacheco-2026-11-24-ceedbb3.zip`.
- SHA-256 verificado antes y después de subir: `b73ea1f47dcc837c434a1224517d5c8e50bf8324362bbcfeb5bd56d83e9c3989`.
- Los 111 hashes del manifiesto interno se verificaron correctamente en el servidor.
- Frontend publicado: `/assets/index-1m5II2gH.js`.
- Backend iniciado correctamente con Java 21; Flyway informó que el esquema estaba actualizado y no requería migraciones.
- Se reconstruyeron y recrearon únicamente backend y frontend. El código, Dockerfile y requisitos del chatbot coinciden por SHA-256 con el paquete anterior; su contenedor se conservó.

## Configuración y recuperación

Se conservaron `.env`, `.compose-project` y `.compose-compat`, con permisos 600. Compose utiliza proyecto `pitagoras`, `docker-compose.yml` y `flyway-ubb/compose.yml`.

Volúmenes conservados y comprobados: `pitagoras_db_data:/var/lib/mysql` y `pitagoras_backend_uploads:/app/uploads`. La base permaneció en su contenedor original y saludable. No se ejecutó limpieza de volúmenes ni `down -v`.

Respaldo completo previo, con SQL, archivos y configuración privada:

`~/pitagoras-backups/snapshot-20261125T002234Z-before-ceedbb3`

Los hashes de `database.sql` y `uploads.tar.gz` se verificaron correctamente. El backend se detuvo durante el respaldo y se reanudó antes de recrearlo con la nueva imagen.

Imágenes anteriores conservadas para reversión:

- `localhost/pitagoras_backend:rollback-212535a` — `96a0ff89c209`.
- `localhost/pitagoras_frontend:rollback-212535a` — `3dbfb2511fb0`.
- `localhost/pitagoras_chatbot:rollback-212535a` — `dc59a907fc03`.

Imágenes desplegadas:

- Backend: `bbbb213066794efca04565cb868f4e25ce697c507fea79ffa52edf3d18d581ee`.
- Frontend: `23391b68c231c4fb60896c4c17f6cd78ce3dbfeeec1f60a81db15f1399afdd5c`.

## Observaciones para las pruebas

1. **Gemini no tiene clave configurada:** `/chatbot/health` respondió HTTP 200, `status: healthy`, `gemini_api_configured: false`. Una respuesta de respaldo no valida Gemini real. Configurar una clave válida mediante el mecanismo autorizado antes de certificar ese flujo. No incluir secretos en informes, capturas ni logs.
2. **Separadores del ZIP:** el archivo generado en Windows contiene rutas con barras inversas. `ZipFile.extractall` por sí solo generó nombres incorrectos en Linux. Se extrajo normalizando cada nombre con `replace(chr(92), '/')`, comprobando que los destinos permanecieran dentro del staging. Se quitaron únicamente las copias de nombres malformados cuyo contenido coincidía con el archivo normalizado. Los 111 hashes internos verificaron correctamente después. Corregir el empaquetador para que genere rutas POSIX en futuros ZIP; no cambió el contenido de aplicación desplegado.
3. La sesión SSH se cerró al terminar. No se instaló ni configuró `ssh-agent`, y no se modificaron servicios de Windows.
4. No se publicó una versión en `main`. Las pruebas de cinco roles, F01–F14, descarga ZIP, formatos de pantalla y Gemini real fueron ejecutadas por Antigravity sobre `ceedbb3`:
   - **Aprobado en Pacheco:** bundle `/assets/index-1m5II2gH.js`, F01–F14 (incluyendo Gemini real `is_mock: false` en `/chatbot/api/chat` y `/chatbot/api/evaluar-planificacion`, F14 con revisión IA real y con versiones compartiendo adjunto), descarga ZIP del profesor y vistas responsivas (`1440×900`, `768×1024`, `844×390`, `320×740`).
   - **Nuevo hallazgo bloqueante para pautas profesionales (F15) y `/chatbot/api/chat-archivo` — CORREGIDO Y VALIDADO EN PACHECO (`testing @ 237c25a`):**
     - Desplegado en `~/pitagoras-testing-237c25a/pitagoras` (ZIP POSIX `pitagoras-pacheco-2026-11-24-237c25a.zip`, SHA-256 `8a3d22a5f87e732506cbb5a696ed45e93edf6e603f3a72825729d088a915d583`, con `flyway-ubb/migrations/V8__ampliar_columna_tipo_evaluacion.sql`).
     - Reconstruidos `backend` (`5434843f4aa8`) y `chatbot` (`0a812e3138d3`), reiniciado `practicas_frontend` para refrescar DNS interno, conservando `.env`, `.compose-project`, `.compose-compat`, `rollback-ceedbb3` y los volúmenes `pitagoras_db_data` y `pitagoras_backend_uploads`.
     - **F15 validado en Pacheco:** `POST /api/evaluaciones` con `TUTOR_ESP_1` sobre asignación QA `9` devolvió `200` (`evalId: 4`), firma digital `200` (`score: 5.9`, `finalizedAt: 2026-11-25T00:58:23.204390926Z`), y re-edición post-firma bloqueada con `409`.
     - **`/chatbot/api/chat-archivo` validado en Pacheco:** devolvió `200` con **Gemini real (`is_mock: false`)** y fuentes RAG oficiales (`answerLen: 3522`).

Usar como matriz [PRUEBAS_FUNCIONALES_ROLES_2026-11-24.md](PRUEBAS_FUNCIONALES_ROLES_2026-11-24.md). Los resultados distinguiendo comprobación local y validación efectiva en Pacheco ya fueron incorporados en dicho informe y en [PENDIENTES_PACHECO_2026-11-25.md](PENDIENTES_PACHECO_2026-11-25.md).
