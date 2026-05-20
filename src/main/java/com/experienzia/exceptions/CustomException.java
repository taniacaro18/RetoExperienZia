package com.experienzia.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción personalizada que usamos en los servicios cuando algo sale mal
 * (usuario no encontrado, evento no aprobado, salón ocupado, etc.).
 * El mensaje se envía al frontend y el código HTTP indica el tipo de error.
 */
public class CustomException extends RuntimeException {

    // Por ejemplo 404, 403, 409 — lo usa GlobalExceptionHandler para responder bien.
    private final HttpStatus status;

    // Error típico con código 400 (petición incorrecta).
    public CustomException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    // Permite elegir otro código (404 Not Found, 409 Conflict, etc.).
    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
