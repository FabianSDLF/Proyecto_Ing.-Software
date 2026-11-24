-- HU43, HU46, HU47: Metadatos de cierre/firma, versión de rúbrica, control de revisión e instantánea inmutable.

ALTER TABLE evaluaciones ADD COLUMN rubric_version VARCHAR(50);
ALTER TABLE evaluaciones ADD COLUMN revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE evaluaciones ADD COLUMN total_points INT;
ALTER TABLE evaluaciones ADD COLUMN max_points INT;
ALTER TABLE evaluaciones ADD COLUMN finalized_at DATETIME(6);
ALTER TABLE evaluaciones ADD COLUMN finalized_by_id BIGINT;
ALTER TABLE evaluaciones ADD COLUMN finalized_by_email VARCHAR(255);
ALTER TABLE evaluaciones ADD COLUMN finalized_by_name VARCHAR(255);
ALTER TABLE evaluaciones ADD COLUMN finalized_by_role VARCHAR(100);
ALTER TABLE evaluaciones ADD COLUMN snapshot_json TEXT;
ALTER TABLE evaluaciones ADD COLUMN snapshot_hash VARCHAR(64);

ALTER TABLE criterios_evaluacion ADD COLUMN criterion_code VARCHAR(50);
ALTER TABLE criterios_evaluacion ADD COLUMN dimension VARCHAR(50);
ALTER TABLE criterios_evaluacion ADD COLUMN display_order INT;

-- Conservar evaluaciones históricas finalizadas sin inventar indicadores ni recalcular notas.
UPDATE evaluaciones
SET finalized_at = COALESCE(evaluation_date, created_at),
    finalized_by_id = evaluator_id,
    finalized_by_name = digital_signature
WHERE is_finalized = TRUE AND finalized_at IS NULL;

-- Restricción de unicidad por asignación + tipo de instrumento.
ALTER TABLE evaluaciones
ADD CONSTRAINT uk_evaluaciones_assignment_type UNIQUE (practice_assignment_id, type);
