package com.ubb.practicas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HU5: Datos editables de un ciclo de práctica. El código (INICIAL, INTERMEDIO, AVANZADO)
 * y el orden no se modifican, porque los tres ciclos son fijos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeCycleUpdateRequest {

    @NotBlank(message = "El nombre del ciclo es obligatorio")
    @Size(max = 100, message = "El nombre del ciclo no puede superar los 100 caracteres")
    private String name;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    private String description;

    @Min(value = 1, message = "El semestre de inicio debe estar entre 1 y 12")
    @Max(value = 12, message = "El semestre de inicio debe estar entre 1 y 12")
    private Integer startSemester;

    @Min(value = 1, message = "El semestre de término debe estar entre 1 y 12")
    @Max(value = 12, message = "El semestre de término debe estar entre 1 y 12")
    private Integer endSemester;
}
