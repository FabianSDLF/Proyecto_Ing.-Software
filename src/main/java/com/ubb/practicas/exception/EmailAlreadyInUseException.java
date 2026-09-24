package com.ubb.practicas.exception;

public class EmailAlreadyInUseException extends ResourceConflictException {
    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}
