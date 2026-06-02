package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request para POST /auth/refresh
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshRequest {

    @NotBlank(message = "El refresh token es requerido")
    private String refreshToken;
}
