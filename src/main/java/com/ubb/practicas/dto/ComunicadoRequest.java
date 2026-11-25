package com.ubb.practicas.dto;

import lombok.Data;

@Data
public class ComunicadoRequest {
    private String mensaje;
    private String correoDestinatario;
}
