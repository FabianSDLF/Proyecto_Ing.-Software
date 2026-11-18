#!/usr/bin/env bash
set -euo pipefail
umask 077
snapshot="${1:?Indique el nombre del snapshot}"
[[ "$snapshot" =~ ^snapshot-[0-9]{8}T[0-9]{6}Z-[0-9]+$ ]] || { echo 'Snapshot inválido'; exit 1; }
cd "/backups/$snapshot"
sha256sum --check SHA256SUMS
[[ -z "$(find /uploads -mindepth 1 -print -quit)" ]] || { echo 'Use un volumen de uploads vacío para restaurar'; exit 1; }
export MYSQL_PWD="${DB_PASSWORD:?Defina DB_PASSWORD}"
mysql --host="${DB_HOST:-db}" --user="${DB_USER:-root}" "${DB_NAME:-sistema_practicas}" < database.sql
# La restauración debe usar un volumen de uploads vacío; evita mezclar versiones.

tar -C /uploads -xzf uploads.tar.gz
echo "Restauración completada: $snapshot"
