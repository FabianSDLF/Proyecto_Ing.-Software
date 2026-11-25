-- HU5: Registro de los tres ciclos de práctica (Inicial, Intermedio, Avanzado).
-- El código coincide con el enum PracticeCycle que ya usan asignaturas.cycle y las duplas.

CREATE TABLE IF NOT EXISTS ciclos_practica (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    display_order INT NOT NULL,
    start_semester INT,
    end_semester INT,
    updated_at DATETIME(6),
    CONSTRAINT uk_ciclos_practica_code UNIQUE (code)
);

INSERT IGNORE INTO ciclos_practica (code, name, description, display_order, start_semester, end_semester, updated_at) VALUES
('INICIAL', 'Ciclo Inicial',
 'Aproximación al sistema escolar y Ambientes de aprendizaje. Sin clases reales: simulaciones en la universidad, visitas de delegaciones y conversatorios.',
 1, 4, 5, NOW(6)),
('INTERMEDIO', 'Ciclo Intermedio',
 'Trabajo colaborativo en el aula e Investigación en el aula. Primer contacto real con el aula, en duplas que comparten centro de práctica.',
 2, 6, 7, NOW(6)),
('AVANZADO', 'Ciclo Avanzado',
 'Práctica pedagógica (6 semanas) y Práctica profesional (13 semanas). Individual: el estudiante tiene a cargo el curso y la jefatura.',
 3, NULL, NULL, NOW(6));
