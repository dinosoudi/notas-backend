package com.taskflow.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando un recurso no existe o no pertenece al usuario.
 * Devuelve HTTP 404.
 *
 * Nota de seguridad: también se usa cuando el recurso existe pero
 * pertenece a otro usuario — así no revelamos si el recurso existe.
 * Ejemplo: GET /notes/{id} con id de otro usuario → 404, no 403.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s no encontrado con %s: %s", resource, field, value));
    }
}
