package com.taskflow.tags.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TagResponse {

    private UUID id;
    private String name;
    private String color;
    private Integer noteCount;
    private LocalDateTime createdAt;
}
