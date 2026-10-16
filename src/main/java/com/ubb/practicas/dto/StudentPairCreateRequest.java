package com.ubb.practicas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPairCreateRequest {

    @NotNull(message = "El identificador de la primera asignación es obligatorio")
    private Long assignment1Id;

    @NotNull(message = "El identificador de la segunda asignación es obligatorio")
    private Long assignment2Id;

    @NotNull(message = "El identificador del período académico es obligatorio")
    private Long periodId;
}
