package com.taskflow.tags.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TagRequest {

    @NotBlank(message = "El nombre del tag es requerido")
    @Size(min = 1, max = 50, message = "El nombre debe tener entre 1 y 50 caracteres")
    private String name;

    @NotBlank(message = "El color es requerido")
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "El color debe ser un valor hexadecimal válido. Ejemplo: #FF6B6B"
    )
    private String color;
}
