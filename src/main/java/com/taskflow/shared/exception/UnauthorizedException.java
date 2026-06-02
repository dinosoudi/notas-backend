package com.taskflow.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando las credenciales son incorrectas o el token es inválido.
 * Devuelve HTTP 401.
 *
 * Ejemplos:
 * - Email o contraseña incorrectos en /auth/login
 * - Código de reset incorrecto o expirado
 * - RefreshToken inválido o expirado
 * - Contraseña actual incorrecta en changePassword
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    private final Integer attemptsRemaining;

    public UnauthorizedException(String message) {
        super(message);
        this.attemptsRemaining = null;
    }

    // Para el caso de código de reset incorrecto — informar intentos restantes
    public UnauthorizedException(String message, Integer attemptsRemaining) {
        super(message);
        this.attemptsRemaining = attemptsRemaining;
    }

    public Integer getAttemptsRemaining() {
        return attemptsRemaining;
    }
}
