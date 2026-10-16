# syntax=docker/dockerfile:1
# Etapa de construcción
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copiar solo pom.xml y pre-descargar dependencias usando cache de BuildKit
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B || true

# 2. Copiar código fuente y compilar reutilizando la cache de /root/.m2
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests && \
    cp target/*.jar app.jar

# Etapa de ejecución liviana
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
RUN mkdir -p uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
