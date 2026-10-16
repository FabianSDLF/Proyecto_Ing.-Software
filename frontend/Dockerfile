# syntax=docker/dockerfile:1
# Etapa de construcción
FROM node:20-alpine AS build
WORKDIR /app

# Copiar manifiestos de dependencias
COPY package.json package-lock.json ./

# Instalar usando package-lock.json y cache de BuildKit (NO borrar package-lock.json)
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit

# Copiar código fuente y compilar
COPY . .
RUN npm run build

# Etapa de servidor web liviana
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN echo 'server { listen 80; location / { root /usr/share/nginx/html; index index.html index.htm; try_files $uri $uri/ /index.html; } }' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
