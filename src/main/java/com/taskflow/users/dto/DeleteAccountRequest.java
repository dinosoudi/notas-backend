package com.taskflow.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeleteAccountRequest {

    @NotBlank(message = "La contraseña es requerida")
    private String password;

    // El usuario debe escribir exactamente "ELIMINAR" para confirmar
    // Previene eliminaciones accidentales — patrón usado por GitHub, Vercel, etc.
    @NotBlank(message = "La confirmación es requerida")
    @Pattern(
            regexp = "^ELIMINAR$",
            message = "Debes escribir ELIMINAR para confirmar"
    )
    private String confirmation;
}
