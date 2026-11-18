#!/usr/bin/env bash
set -euo pipefail
umask 077
export MYSQL_PWD="${DB_PASSWORD:?Defina DB_PASSWORD}"
snapshot="snapshot-$(date -u +%Y%m%dT%H%M%SZ)-${RANDOM}"
pending="/backups/.${snapshot}"
mkdir -p /backups
mkdir "$pending"
# Solo se publica una copia cuando los dos archivos y sus hashes están completos.
mysqldump --host="${DB_HOST:-db}" --user="${DB_USER:-root}" \
  --single-transaction --routines --triggers --no-tablespaces \
  "${DB_NAME:-sistema_practicas}" > "$pending/database.sql"
tar -C /uploads -czf "$pending/uploads.tar.gz" .
(cd "$pending" && sha256sum database.sql uploads.tar.gz > SHA256SUMS)
mv "$pending" "/backups/$snapshot"
echo "Respaldo completado: $snapshot"
# Retención solo sobre snapshots completos; nunca sobre otros directorios.
find /backups -mindepth 1 -maxdepth 1 -type d -name 'snapshot-*' -mtime "+${BACKUP_RETENTION_DAYS:-30}" -print0 |
while IFS= read -r -d '' old; do
  if [[ -f "$old/SHA256SUMS" ]]; then
    rm -f -- "$old/database.sql" "$old/uploads.tar.gz" "$old/SHA256SUMS"
    rmdir -- "$old"
  fi
done
