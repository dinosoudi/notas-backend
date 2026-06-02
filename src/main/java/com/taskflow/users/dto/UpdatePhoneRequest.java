package com.taskflow.users.dto;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdatePhoneRequest {

    // Nullable — enviar null para eliminar el teléfono del perfil
    // En v2 este campo desencadenará verificación por SMS
    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Ingresa un número de teléfono válido en formato internacional. Ejemplo: +52 55 1234 5678"
    )
    private String phone;
}
