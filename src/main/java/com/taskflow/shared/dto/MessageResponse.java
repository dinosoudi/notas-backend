package com.taskflow.shared.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response genérico para endpoints que solo devuelven un mensaje.
 * Usado en forgot-password, resend-verification, logout, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String message;

    // Solo presente en DELETE /users/me y cancel-deletion
    private LocalDateTime deletionScheduledAt;

    // Solo presente en logout-all y change-password con closeOtherSessions=true
    private Integer sessionsClosedCount;

    // Solo presente en forgot-password
    private Integer expiresInMinutes;

    // Constructor simple para cuando solo necesitas el mensaje
    public MessageResponse(String message) {
        this.message = message;
    }
}
