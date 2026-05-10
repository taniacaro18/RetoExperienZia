package com.experienzia.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción única del sistema. El status HTTP determina la respuesta del
 * GlobalExceptionHandler. Por defecto se usa BAD_REQUEST (400).
 */
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
