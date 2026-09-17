package com.ubb.practicas.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PracticeCycle {
    INICIAL, INTERMEDIO, AVANZADO;

    @JsonCreator
    public static PracticeCycle fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (PracticeCycle cycle : values()) {
            if (cycle.name().equals(normalized)) {
                return cycle;
            }
        }
        throw new IllegalArgumentException("Ciclo desconocido: " + value + ". Valores permitidos: INICIAL, INTERMEDIO, AVANZADO.");
    }
}

