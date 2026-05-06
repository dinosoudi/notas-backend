package com.taskflow.users.dto;

import com.taskflow.users.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response para GET /users/me y todos los PATCH /users/me/*
 * Incluye preferences y deletionScheduledAt a diferencia del UserDTO básico.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {

    private UUID id;
    private String name;
    private String email;           // no editable en v1
    private String phone;
    private Boolean emailVerified;
    private User.AuthProvider authProvider;
    private PreferencesDTO preferences;
    private LocalDateTime createdAt;

    // Null si la cuenta no está marcada para eliminar
    // No null = mostrar banner de advertencia en el frontend
    private LocalDateTime deletionScheduledAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PreferencesDTO {
        private Boolean darkMode;
        private String language;
    }
}
