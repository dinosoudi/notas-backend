package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request para POST /auth/login
 * identifier acepta email o teléfono (v2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "El correo es requerido")
    private String identifier;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}