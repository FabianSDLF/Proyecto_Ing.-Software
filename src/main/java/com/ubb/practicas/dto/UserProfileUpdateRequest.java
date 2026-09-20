package com.ubb.practicas.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password; // Opcional, por si quiere cambiar su clave
}