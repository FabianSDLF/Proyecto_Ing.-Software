# Etapa de construcción
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copiar solo pom.xml y descargar dependencias (se cachea mientras no cambie pom.xml)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copiar código fuente y compilar (solo recompila, no re-descarga)
COPY src ./src
RUN mvn clean package -DskipTests -o

# Etapa de ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Directorio para subir archivos según application.properties
RUN mkdir -p uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
