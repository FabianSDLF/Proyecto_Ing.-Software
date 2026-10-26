# Script para empaquetar la distribución liviana lista para el servidor de la Universidad del Bío-Bío
# Genera la carpeta autónoma "pitagoras"
$ErrorActionPreference = "Stop"

$RootDir = $PSScriptRoot
$DeployDir = Join-Path $RootDir "pitagoras"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Preparando Paquete de Despliegue para Servidor UBB" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Compilar Backend si no existe o para asegurar versión fresca
Write-Host "`n[1/5] Compilando Backend Spring Boot (.jar)..." -ForegroundColor Yellow
Set-Location $RootDir
& .\mvnw.cmd clean package -DskipTests

$JarFile = Get-ChildItem -Path (Join-Path $RootDir "target") -Filter "*.jar" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
if (-not $JarFile) {
    Write-Error "No se encontró el archivo .jar en target/"
    exit 1
}
Write-Host " -> Backend compilado exitosamente: $($JarFile.Name) ($([math]::Round($JarFile.Length / 1MB, 2)) MB)" -ForegroundColor Green

# 2. Compilar Frontend React + Vite
Write-Host "`n[2/5] Compilando Frontend React (dist/)..." -ForegroundColor Yellow
Set-Location (Join-Path $RootDir "frontend")
& npm.cmd run build
$DistDir = Join-Path $RootDir "frontend\dist"
if (-not (Test-Path $DistDir)) {
    Write-Error "No se encontró el directorio frontend/dist"
    exit 1
}
Write-Host " -> Frontend compilado exitosamente en frontend/dist" -ForegroundColor Green

# 3. Limpiar y recrear carpeta de despliegue
Write-Host "`n[3/4] Ensamblando carpeta 'pitagoras'..." -ForegroundColor Yellow
Set-Location $RootDir
if (Test-Path $DeployDir) {
    Remove-Item -Path $DeployDir -Recurse -Force
}
New-Item -ItemType Directory -Path $DeployDir | Out-Null
New-Item -ItemType Directory -Path (Join-Path $DeployDir "backend") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $DeployDir "frontend") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $DeployDir "chatbot") | Out-Null

# Copiar Backend (.jar y Dockerfile liviano)
Copy-Item -Path $JarFile.FullName -Destination (Join-Path $DeployDir "backend\app.jar")
@'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY app.jar app.jar
RUN mkdir -p uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
'@ | Set-Content -Path (Join-Path $DeployDir "backend\Dockerfile") -Encoding utf8

# Copiar Frontend (dist, Dockerfile liviano y nginx.conf)
Copy-Item -Path $DistDir -Destination (Join-Path $DeployDir "frontend\dist") -Recurse
@'
FROM nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
'@ | Set-Content -Path (Join-Path $DeployDir "frontend\Dockerfile") -Encoding utf8

@'
server {
    listen 80;
    client_max_body_size 50M;

    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /chat {
        proxy_pass http://chatbot:8000/chat;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
'@ | Set-Content -Path (Join-Path $DeployDir "frontend\nginx.conf") -Encoding utf8

# Copiar Chatbot
$ChatbotSrc = Join-Path $RootDir "chatbot"
Copy-Item -Path (Join-Path $ChatbotSrc "main.py") -Destination (Join-Path $DeployDir "chatbot\main.py")
Copy-Item -Path (Join-Path $ChatbotSrc "requirements.txt") -Destination (Join-Path $DeployDir "chatbot\requirements.txt")
Copy-Item -Path (Join-Path $ChatbotSrc "Dockerfile") -Destination (Join-Path $DeployDir "chatbot\Dockerfile")
if (Test-Path (Join-Path $ChatbotSrc "recursos")) {
    Copy-Item -Path (Join-Path $ChatbotSrc "recursos") -Destination (Join-Path $DeployDir "chatbot\recursos") -Recurse
}

# Crear docker-compose.yml con UN SOLO PUERTO EXPUESTO (80)
@'
services:
  db:
    image: mysql:8.4
    container_name: practicas_db
    environment:
      MYSQL_ROOT_PASSWORD: admin
      MYSQL_DATABASE: sistema_practicas
    volumes:
      - db_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-padmin"]
      interval: 3s
      timeout: 3s
      retries: 10
      start_period: 5s
    restart: unless-stopped

  backend:
    build: ./backend
    container_name: practicas_backend
    depends_on:
      db:
        condition: service_healthy
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/sistema_practicas?allowPublicKeyRetrieval=true&useSSL=false
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=admin
      - SPRING_JPA_HIBERNATE_DDL_AUTO=update
      - SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false
    volumes:
      - backend_uploads:/app/uploads
    restart: unless-stopped

  frontend:
    build: ./frontend
    container_name: practicas_frontend
    ports:
      - "8008:80"
    depends_on:
      - backend
      - chatbot
    restart: unless-stopped

  chatbot:
    build: ./chatbot
    container_name: practicas_chatbot
    environment:
      - GOOGLE_API_KEY=${GOOGLE_API_KEY:-}
    restart: unless-stopped

volumes:
  db_data:
  backend_uploads:
'@ | Set-Content -Path (Join-Path $DeployDir "docker-compose.yml") -Encoding utf8

# 4. Resumen final
Write-Host "`n[4/4] ¡Carpeta de despliegue generada con éxito!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Carpeta lista:  $DeployDir" -ForegroundColor White
Write-Host " Puerto expuesto: ÚNICAMENTE EL PUERTO 8008" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "INSTRUCCIONES PARA EL SERVIDOR:" -ForegroundColor Yellow
Write-Host " 1. Sube la carpeta directamente por SCP:"
Write-Host "      scp -r pitagoras usuario@servidor.ubiobio.cl:~/"
Write-Host " 2. En el servidor ejecuta:"
Write-Host "      cd pitagoras"
Write-Host "      docker compose up -d --build"
Write-Host "==========================================================" -ForegroundColor Cyan
