package com.taskflow.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando los datos del request son inválidos a nivel de lógica.
 * Devuelve HTTP 400.
 *
 * Ejemplos:
 * - Contraseñas no coinciden en /auth/register
 * - Nueva contraseña igual a la anterior en /auth/reset-password
 * - Token de reset expirado
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String message) {
        super(message);
        this.field = null;
    }

    public BadRequestException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
