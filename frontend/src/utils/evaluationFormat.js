const OFFICIAL_RUBRIC_TYPES = new Set([
    'COLAB_ESP_1',
    'COLAB_ESP_2',
    'COLAB_ORI_1',
    'COLAB_ORI_2',
    'TUTOR_ESP_1',
    'TUTOR_ESP_2',
    'TUTOR_ORI_1',
    'TUTOR_ORI_2',
]);

const HAS_TIMEZONE_SUFFIX = /(?:Z|[+-]\d{2}:?\d{2})$/i;

/**
 * Determina si una evaluación utiliza la escala oficial vigente (2 a 7 puntos enteros).
 * Los registros históricos (sin rubricVersion o marcados como historicalManual)
 * no deben mostrarse con la escala 2-7 ni "/ 7 pts".
 */
export function hasOfficialRubricScale(evaluation, activeRubric = null) {
    if (!evaluation) {
        return Boolean(activeRubric);
    }
    if (evaluation.historicalManual) {
        return false;
    }
    if (evaluation.rubricVersion) {
        return true;
    }
    if (activeRubric && OFFICIAL_RUBRIC_TYPES.has(evaluation.type)) {
        return true;
    }
    return false;
}

/**
 * Formatea una fecha de evaluación en formato local chileno.
 * Si el valor incluye zona horaria explícita (por ejemplo UTC con 'Z'),
 * lo convierte a America/Santiago. Si corresponde a un registro histórico
 * sin zona horaria, preserva su hora tal como fue registrada.
 */
export function formatEvaluationTimestamp(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value);
    }
    if (typeof value === 'string' && HAS_TIMEZONE_SUFFIX.test(value.trim())) {
        return date.toLocaleString('es-CL', { timeZone: 'America/Santiago', hour12: false });
    }
    return date.toLocaleString('es-CL', { hour12: false });
}
