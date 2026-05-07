package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LogoutRequest {

    @NotBlank(message = "El refresh token es requerido")
    private String refreshToken;
}
