package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VerifyEmailRequest {

    @NotBlank(message = "El token es requerido")
    private String token;
}