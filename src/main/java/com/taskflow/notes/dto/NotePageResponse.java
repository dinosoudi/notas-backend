package com.taskflow.notes.dto;

import lombok.*;

import java.util.List;

/**
 * Response paginado — estructura estándar de Spring Page
 * Coincide exactamente con el contrato YAML.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotePageResponse {

    private List<NoteResponse> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean first;
    private Boolean last;
}
