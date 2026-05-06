package com.taskflow.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando hay un conflicto con un recurso existente.
 * Devuelve HTTP 409.
 *
 * Ejemplos:
 * - Email ya registrado en /auth/register
 * - Tag con mismo nombre para el mismo usuario
 * - Intentar borrar tag con notas sin force=true
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    // Cuántas notas afectadas — solo para el caso de borrar tag con notas
    private final Integer affectedCount;

    public ConflictException(String message) {
        super(message);
        this.affectedCount = null;
    }

    public ConflictException(String message, Integer affectedCount) {
        super(message);
        this.affectedCount = affectedCount;
    }

    public Integer getAffectedCount() {
        return affectedCount;
    }
}
