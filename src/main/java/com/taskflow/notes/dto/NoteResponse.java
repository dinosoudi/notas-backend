package com.taskflow.notes.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoteResponse {

    private UUID id;
    private String content;
    private Boolean completed;
    private TagSummary tag;         // null si la nota no tiene tag
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Resumen del tag — solo los campos necesarios para el frontend
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TagSummary {
        private UUID id;
        private String name;
        private String color;
    }
}
