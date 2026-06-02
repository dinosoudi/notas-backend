package com.taskflow.auth.dto;

import lombok.*;

/**
 * Response para POST /auth/verify-reset-code
 * Devuelve el resetToken temporal para el paso 3 del flujo de recuperación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResetCodeResponse {

    private String message;

    // Token UUID de un solo uso — expira en 15 minutos
    // El frontend lo guarda en memoria (no localStorage) y lo manda en /reset-password
    private String resetToken;

    private Integer expiresInMinutes;
}
