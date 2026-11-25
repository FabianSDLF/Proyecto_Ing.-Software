package com.ubb.practicas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationFinalizeDTO {
    private Long expectedRevision;
    private Boolean confirmed;
}
