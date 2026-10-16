-- =====================================================================
-- PITAGORAS - Inicialización de Base de Datos y Usuario Exclusivo
-- Laboratorio: Gestión de Dependencias y Configuración (Punto 4)
-- =====================================================================
-- Ejecutar este script una sola vez en MySQL con el usuario administrador (root).

-- 1. Crear la base de datos si no existe con codificación UTF-8
CREATE DATABASE IF NOT EXISTS sistema_practicas 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 2. Crear usuario exclusivo para la aplicación (evita el uso de root)
-- Nota: Cambiar 'practicas_password_segura' por la contraseña local elegida
CREATE USER IF NOT EXISTS 'practicas_user'@'localhost' 
    IDENTIFIED BY 'practicas_password_segura';

-- 3. Asignar privilegios únicamente sobre la base de datos del proyecto
GRANT ALL PRIVILEGES ON sistema_practicas.* TO 'practicas_user'@'localhost';

-- Para entornos Docker / red local (opcional si la BD y la app están en contenedores distintos):
CREATE USER IF NOT EXISTS 'practicas_user'@'%' 
    IDENTIFIED BY 'practicas_password_segura';
GRANT ALL PRIVILEGES ON sistema_practicas.* TO 'practicas_user'@'%';

-- 4. Aplicar cambios de privilegios
FLUSH PRIVILEGES;
