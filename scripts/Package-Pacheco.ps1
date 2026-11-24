param([string]$CompatibilityDirectory)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$startingDirectory = Get-Location
$previousApi = $env:VITE_API_URL
$previousChatbot = $env:VITE_CHATBOT_URL
try {
    Set-Location -LiteralPath $projectRoot
    $sourceCommit = (git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo identificar el commit.' }
    if (git status --porcelain) { throw 'Confirma los cambios antes de empaquetar para identificar exactamente el código.' }
    $sourceBranch = (git branch --show-current).Trim()
    $packageName = 'pitagoras-pacheco-' + (Get-Date -Format 'yyyy-MM-dd') + '-' + $sourceCommit.Substring(0, 7)
    $packageRoot = Join-Path $projectRoot ('deploy/' + $packageName)
    if (Test-Path -LiteralPath $packageRoot) { throw "El paquete ya existe: $packageRoot" }

    & (Join-Path $projectRoot 'mvnw.cmd') clean package
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o las pruebas del backend.' }
    [xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw
    $jarPath = Join-Path $projectRoot ('target/' + $pom.project.artifactId + '-' + $pom.project.version + '.jar')
    if (-not (Test-Path -LiteralPath $jarPath)) { throw 'No se encontró el JAR ejecutable esperado.' }

    Set-Location -LiteralPath (Join-Path $projectRoot 'frontend')
    $env:VITE_API_URL = '/'
    $env:VITE_CHATBOT_URL = '/chatbot'
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del frontend.' }
    Set-Location -LiteralPath $projectRoot

    $appRoot = Join-Path $packageRoot 'pitagoras'
    foreach ($folder in @('backend', 'frontend', 'chatbot', 'docs')) {
        New-Item -ItemType Directory -Path (Join-Path $appRoot $folder) -Force | Out-Null
    }
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $appRoot 'backend/app.jar')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'frontend/dist') -Destination (Join-Path $appRoot 'frontend/dist') -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot 'frontend/nginx.conf') -Destination (Join-Path $appRoot 'frontend/nginx.conf')
    $backendDockerfile = @'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY app.jar app.jar
RUN mkdir -p uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
'@
    $frontendDockerfile = @'
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
'@
    [IO.File]::WriteAllText((Join-Path $appRoot 'backend/Dockerfile'), $backendDockerfile + "`n", [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $appRoot 'frontend/Dockerfile'), $frontendDockerfile + "`n", [Text.UTF8Encoding]::new($false))
    foreach ($name in @('main.py', 'requirements.txt', 'Dockerfile', 'recursos')) {
        Copy-Item -LiteralPath (Join-Path $projectRoot ('chatbot/' + $name)) -Destination (Join-Path $appRoot ('chatbot/' + $name)) -Recurse
    }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'backup') -Destination (Join-Path $appRoot 'backup') -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot 'scripts') -Destination (Join-Path $appRoot 'scripts') -Recurse
    foreach ($name in @('docker-compose.yml', '.env.example', 'LEEME-PACHECO.md')) {
        Copy-Item -LiteralPath (Join-Path $projectRoot ('packaging/pacheco/' + $name)) -Destination (Join-Path $appRoot $name)
    }
    foreach ($name in @('RESPALDOS.md', 'INTEGRACION_TESTING_2026-11-24.md', 'PRUEBAS_VISUALES_2026-11-24.md')) {
        Copy-Item -LiteralPath (Join-Path $projectRoot ('docs/' + $name)) -Destination (Join-Path $appRoot ('docs/' + $name))
    }
    if ($CompatibilityDirectory) {
        Copy-Item -LiteralPath $CompatibilityDirectory -Destination (Join-Path $appRoot 'flyway-ubb') -Recurse
    }
    $metadata = [ordered]@{
        commit = $sourceCommit; branch = $sourceBranch; release = $false
        generatedAtUtc = [DateTime]::UtcNow.ToString('o'); backendVersion = [string]$pom.project.version
        javaTarget = 21; port = 8008; frontendApi = '/api'; chatbotApi = '/chatbot'
        compatibilityIncluded = [bool]$CompatibilityDirectory
    }
    $metadata | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $appRoot 'BUILD.json') -Encoding utf8
    $hashLines = Get-ChildItem -LiteralPath $appRoot -Recurse -File -Force | Sort-Object FullName | ForEach-Object {
        $relative = $_.FullName.Substring($appRoot.Length + 1).Replace('\', '/')
        (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + $relative
    }
    [IO.File]::WriteAllText((Join-Path $appRoot 'SHA256SUMS'), ($hashLines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
    if (Get-ChildItem -LiteralPath $appRoot -Recurse -File -Force | Where-Object Name -eq '.env') { throw 'El paquete contiene un .env inesperado.' }
    $zipPath = $packageRoot + '.zip'
    Compress-Archive -LiteralPath $appRoot -DestinationPath $zipPath -CompressionLevel Optimal
    ((Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + [IO.Path]::GetFileName($zipPath)) | Set-Content -LiteralPath ($zipPath + '.sha256') -Encoding ascii
    Write-Output "Carpeta: $appRoot"
    Write-Output "ZIP: $zipPath"
} finally {
    $env:VITE_API_URL = $previousApi
    $env:VITE_CHATBOT_URL = $previousChatbot
    Set-Location -LiteralPath $startingDirectory
}
