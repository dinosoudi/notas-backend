package com.taskflow.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza el manejo de errores de toda la aplicación.
 *
 * Sin esto, Spring devuelve su propio formato de error que no coincide
 * con el contrato YAML. Con @RestControllerAdvice interceptamos todas
 * las excepciones y devolvemos el formato ErrorResponse definido.
 *
 * Orden de handlers: de más específico a más genérico.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Validación de Bean Validation (@Valid) ───────────────
    // Se lanza cuando un @RequestBody falla las validaciones (@NotBlank, @Email, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Tomar el primer error de validación
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = fieldError != null
                ? fieldError.getDefaultMessage()
                : "Datos inválidos en el request";

        String field = fieldError != null ? fieldError.getField() : null;

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, field));
    }

    // ─── Excepciones de negocio ───────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), ex.getField()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        Map<String, Object> body = buildError(
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), null);
        if (ex.getAttemptsRemaining() != null) {
            body.put("attemptsRemaining", ex.getAttemptsRemaining());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        Map<String, Object> body = buildError(
                HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
        if (ex.getAffectedCount() != null) {
            body.put("noteCount", ex.getAffectedCount());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        Map<String, Object> body = buildError(
                HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", ex.getMessage(), null);
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    // ─── Fallback — cualquier excepción no manejada ───────────
    // Loguea el error completo para debugging pero devuelve mensaje genérico
    // Nunca exponer el stack trace al frontend
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error inesperado: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "Ocurrió un error inesperado. Intenta de nuevo.",
                        null));
    }

    // ─── Helper ───────────────────────────────────────────────
    private Map<String, Object> buildError(
            HttpStatus status, String error, String message, String field) {

        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("field", field);
        body.put("timestamp", LocalDateTime.now());
        return body;
    }
}
