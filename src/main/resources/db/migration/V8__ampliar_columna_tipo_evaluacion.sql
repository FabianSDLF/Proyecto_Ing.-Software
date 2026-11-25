-- F15: Garantizar que evaluaciones.type y criterios_evaluacion.achievement_level admitan
-- los 8 instrumentos profesionales (TUTOR_ESP_1..2, TUTOR_ORI_1..2, COLAB_ESP_1..2, COLAB_ORI_1..2)
-- en bases MySQL creadas previamente con columnas ENUM heredadas.

ALTER TABLE evaluaciones MODIFY COLUMN type VARCHAR(50);
ALTER TABLE criterios_evaluacion MODIFY COLUMN achievement_level VARCHAR(50);
