package com.taskflow.auth.dto;

import com.taskflow.users.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request para POST /auth/register
 * Validaciones con Bean Validation — @Valid en el Controller las activa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una mayúscula y un número"
    )
    private String password;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmPassword;

    // Opcional — v2: autenticación por SMS
    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Ingresa un número de teléfono válido en formato internacional"
    )
    private String phone;

    // Preparado para v2 — en v1 el backend asigna EMAIL por defecto
    private User.AuthProvider authProvider;
}
