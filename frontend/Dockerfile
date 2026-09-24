# Etapa de construcción
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa de servidor web
FROM nginx:alpine
# Copiamos la build de React al directorio de nginx
COPY --from=build /app/dist /usr/share/nginx/html
# Copiar archivo de conf de Nginx si es necesario para react-router (para que no de error 404 al recargar)
RUN echo 'server { listen 80; location / { root /usr/share/nginx/html; index index.html index.htm; try_files $uri $uri/ /index.html; } }' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
