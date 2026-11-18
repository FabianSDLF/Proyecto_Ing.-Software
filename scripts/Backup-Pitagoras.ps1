param([string]$ComposeFile = (Join-Path $PSScriptRoot '..\docker-compose.yml'))
$ErrorActionPreference = 'Stop'
$ComposeFile = (Resolve-Path -LiteralPath $ComposeFile).Path
# Detener escrituras para que BD y documentos correspondan al mismo estado.
$running = @(& docker compose -f $ComposeFile ps --status running --services)
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar Docker Compose' }
$restartBackend = $running -contains 'backend'
try {
    if ($restartBackend) {
        & docker compose -f $ComposeFile stop backend
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener el backend; no se generó el respaldo' }
    }
    & docker compose -f $ComposeFile --profile maintenance run --rm --no-deps backup
    if ($LASTEXITCODE -ne 0) { throw 'El respaldo no terminó correctamente' }
} finally {
    if ($restartBackend) {
        & docker compose -f $ComposeFile start backend
        if ($LASTEXITCODE -ne 0) { Write-Error 'El backend requiere reinicio manual' }
    }
}
