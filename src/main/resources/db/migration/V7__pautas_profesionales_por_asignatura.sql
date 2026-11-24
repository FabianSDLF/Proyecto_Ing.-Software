-- La aplicabilidad se configura por asignatura, nunca por todo el ciclo AVANZADO.
ALTER TABLE asignaturas ADD COLUMN professional_rubrics BOOLEAN NOT NULL DEFAULT FALSE;
-- Identidad de la asignatura profesional incluida en los datos de demostración.
UPDATE asignaturas SET professional_rubrics = TRUE
WHERE code = 'PEM-310' AND name IN ('Práctica Profesional y Síntesis Curricular', 'Práctica Profesional');
