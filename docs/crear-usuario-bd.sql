-- =====================================================================
-- PITAGORAS - Inicialización de Base de Datos y Usuario Exclusivo
-- =====================================================================
-- Ejecutar una vez en MySQL con el usuario administrador (root):

CREATE DATABASE IF NOT EXISTS sistema_practicas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY '12345';
GRANT ALL PRIVILEGES ON sistema_practicas.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;
