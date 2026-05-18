package com.experienzia.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustom(CustomException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> onDataIntegrityViolation(DataIntegrityViolationException ex) {
        String detalle = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (detalle != null && detalle.contains("comprobante_url")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se pudo actualizar el pago: el comprobante debe poder quedar vacío hasta que el organizador suba uno nuevo. "
                            + "Reinicia la aplicación para aplicar la actualización del esquema de base de datos.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("No se pudo guardar por una restricción de datos. Verifica la información e inténtalo de nuevo.");
    }

    @ExceptionHandler({ UnexpectedRollbackException.class, TransactionSystemException.class })
    public ResponseEntity<String> onTransactionRollback(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("No se pudo confirmar la operación en base de datos. Revisa el estado del evento y del pago; si el error continúa, consulta los logs del servidor.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
