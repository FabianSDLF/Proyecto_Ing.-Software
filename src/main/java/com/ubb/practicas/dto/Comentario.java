package com.ubb.practicas.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comentario {

    @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
    private String feedback;
}