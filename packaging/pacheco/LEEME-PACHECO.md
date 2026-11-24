# PITAGORAS — paquete de testing para Pacheco

El paquete incluye el backend ejecutable para Java 21, el frontend compilado, el chatbot y Docker Compose. La aplicación se publica por el puerto **8008**. Los detalles del código y los hashes están en `BUILD.json` y `SHA256SUMS`.

## Instalación o actualización

1. Sube y extrae el ZIP en un directorio de revisión. La carpeta ejecutable es `pitagoras/`.
2. Si actualizas una instalación existente, conserva su `.env`, su nombre de proyecto Compose y sus volúmenes. Respalda primero MySQL y los archivos según `docs/RESPALDOS.md`. No ejecutes `docker compose down -v`.
3. En una instalación nueva, copia `.env.example` a `.env`. Completa `MYSQL_ROOT_PASSWORD` y `JWT_SECRET` (Base64 de al menos 32 bytes, por ejemplo con `openssl rand -base64 32`). Completa Gemini, correo y contacto según corresponda. El paquete no contiene credenciales ni datos de la instalación local.
4. Desde `pitagoras/`, valida la configuración con `docker compose config --quiet`.
5. Cuando corresponda publicar esta versión de testing, ejecuta `docker compose up -d --build`. Comprueba `docker compose ps` y `docker compose logs --tail=100 backend chatbot frontend`.
6. Accede a `http://HOST_DE_PACHECO:8008`. Revisa login, permisos, planificaciones, notificaciones, evaluaciones, exportaciones y chatbot con los datos del servidor.

El frontend usa `/api` y `/chatbot` del mismo servidor mediante Nginx. El servidor construye imágenes Docker a partir de los binarios ya compilados; necesita acceso a las imágenes base y a PyPI para el chatbot.

## Base de datos y compatibilidad

El JAR incluye las migraciones V1 a V7 y mantiene su validación. Consulta `docs/RESPALDOS.md` para los historiales antiguos con versiones V2 duplicadas. No ejecutes `repair` ni desactives validaciones automáticamente.

Si aparece `flyway-ubb/`, contiene la compatibilidad conservada del paquete UBB anterior. **No se activa por defecto**. En un servidor que ya emplee esa configuración, conserva el mismo archivo Compose adicional y contrasta el historial Flyway antes de continuar. Para esa instalación, los comandos deben incluir `-f docker-compose.yml -f flyway-ubb/compose.yml`, como en la entrega anterior.

La aplicación conserva el inicializador de cuentas de demostración; revisar esas cuentas al habilitar acceso externo. Este paquete pertenece a `testing`; no constituye una nueva versión publicada en `main`.
