package com.taskflow.users.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdatePreferencesRequest {

    @NotNull(message = "El modo oscuro es requerido")
    private Boolean darkMode;

    @NotNull(message = "El idioma es requerido")
    @Pattern(
            regexp = "^(es|en)$",
            message = "Idioma no soportado. Opciones disponibles: es, en"
    )
    private String language;
}
