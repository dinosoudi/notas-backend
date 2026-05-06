package com.taskflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request para POST /auth/reset-password
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "El token de recuperación es requerido")
    private String resetToken;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una mayúscula y un número"
    )
    private String newPassword;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmPassword;
}
