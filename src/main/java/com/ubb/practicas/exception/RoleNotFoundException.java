package com.ubb.practicas.exception;

public class RoleNotFoundException extends ResourceNotFoundException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}
