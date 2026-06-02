package com.taskflow.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoteRequest {

    @NotBlank(message = "El contenido de la nota no puede estar vacío")
    @Size(min = 1, max = 5000, message = "El contenido debe tener entre 1 y 5000 caracteres")
    private String content;

    // false por default — nota nueva empieza sin completar
    private Boolean completed;

    // Opcional — null = nota sin tag
    private UUID tagId;
}
