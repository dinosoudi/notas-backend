package com.taskflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request para POST /auth/verify-reset-code
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResetCodeRequest {

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Ingresa un correo electrónico válido")
    private String email;

    @NotBlank(message = "El código es requerido")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe ser de 6 dígitos")
    private String code;
}
