package com.posgrado.notes.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Manejo centralizado de excepciones.
 * Convierte las excepciones de seguridad y negocio en respuestas HTTP claras.
 * Importante: AccessDeniedException también es manejada por Spring Security
 * (retorna 403 si no se captura aquí), pero al capturarla podemos personalizar el body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Cuando el usuario no tiene permiso sobre un recurso específico
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Acceso denegado",
                        "detalle", ex.getMessage()
                ));
    }

    // Errores de negocio (usuario no encontrado, estudiante inválido, etc.)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
