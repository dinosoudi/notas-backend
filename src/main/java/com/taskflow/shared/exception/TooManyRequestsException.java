package com.taskflow.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando se excede el límite de intentos.
 * Devuelve HTTP 429.
 *
 * Ejemplos:
 * - Más de 5 intentos fallidos de login
 * - Más de 3 reenvíos de código de recuperación por hora
 * - Más de 5 intentos de código de reset incorrecto
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends RuntimeException {

    // Segundos que debe esperar antes de reintentar
    private final Integer retryAfterSeconds;

    public TooManyRequestsException(String message, Integer retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
