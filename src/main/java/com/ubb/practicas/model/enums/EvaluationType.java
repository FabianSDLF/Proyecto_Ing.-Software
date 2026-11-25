package com.ubb.practicas.model.enums;

public enum EvaluationType {
    COLAB_ESP_1, COLAB_ESP_2, COLAB_ORI_1, COLAB_ORI_2,
    TUTOR_ESP_1, TUTOR_ESP_2, TUTOR_ORI_1, TUTOR_ORI_2,
    PAUTA_OBSERVACION_COLABORADOR, PAUTA_OBSERVACION_TUTOR, AUTOEVALUACION, CO_EVALUACION_DUPLA, RUBRICA_INFORME, RUBRICA_TANEP, RUBRICA_PROYECTO_INTERVENCION;

    public boolean isCollaboratorInstrument() {
        return name().startsWith("COLAB_") || this == PAUTA_OBSERVACION_COLABORADOR;
    }

    public boolean isTutorInstrument() {
        return name().startsWith("TUTOR_") || this == PAUTA_OBSERVACION_TUTOR;
    }

    public boolean isProfessionalRubric() {
        return name().startsWith("COLAB_") || name().startsWith("TUTOR_");
    }
}
