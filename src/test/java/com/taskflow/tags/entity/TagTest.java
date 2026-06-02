package com.taskflow.tags.entity;

import com.taskflow.notes.entity.Note;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entidad Tag")
class TagTest {

    @Test
    @DisplayName("getNoteCount() debe retornar el tamaño de la lista de notas")
    void getNoteCountShouldReturnSize() {
        Tag tag = Tag.builder()
                .notes(new ArrayList<>())
                .build();
        tag.getNotes().add(Note.builder().build());
        assertThat(tag.getNoteCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getNoteCount() debe retornar 0 cuando la lista es null")
    void getNoteCountShouldReturnZeroWhenListIsNull() {
        Tag tag = Tag.builder().notes(null).build();
        assertThat(tag.getNoteCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getNoteCount() debe retornar 0 cuando la lista está vacía")
    void getNoteCountShouldReturnZeroForEmptyList() {
        Tag tag = Tag.builder().notes(new ArrayList<>()).build();
        assertThat(tag.getNoteCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Builder.Default debe asignar color por defecto #4ECDC4")
    void builderShouldDefaultColor() {
        Tag tag = Tag.builder().name("Importante").build();
        assertThat(tag.getColor()).isEqualTo("#4ECDC4");
    }

    @Test
    @DisplayName("Se debe poder sobrescribir el color con el builder")
    void colorCanBeOverridden() {
        Tag tag = Tag.builder().name("Urgente").color("#FF0000").build();
        assertThat(tag.getColor()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("onCreate() debe establecer createdAt y updatedAt")
    void onCreateShouldSetBothTimestamps() {
        Tag tag = new Tag();
        tag.onCreate();

        assertThat(tag.getCreatedAt()).isNotNull();
        assertThat(tag.getUpdatedAt()).isNotNull();
        assertThat(tag.getCreatedAt()).isEqualTo(tag.getUpdatedAt());
    }

    @Test
    @DisplayName("onUpdate() debe refrescar solo updatedAt")
    void onUpdateShouldRefreshUpdatedAtOnly() {
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        Tag tag = Tag.builder()
                .createdAt(before)
                .updatedAt(before)
                .build();
        tag.onUpdate();

        assertThat(tag.getUpdatedAt()).isAfter(before);
        assertThat(tag.getCreatedAt()).isEqualTo(before);
    }

    @Test
    @DisplayName("Builder.Default debe inicializar la lista de notas vacía")
    void builderShouldInitializeNotesListEmpty() {
        Tag tag = Tag.builder().build();
        assertThat(tag.getNotes()).isNotNull().isEmpty();
    }
}