package com.taskflow.notes.entity;

import com.taskflow.tags.entity.Tag;
import com.taskflow.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoteTest {

    private Note note;
    private User user;
    private Tag tag;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        tag = Tag.builder().id(UUID.randomUUID()).name("Work").notes(new ArrayList<>()).build();
        note = Note.builder()
                .content("Test content")
                .completed(false)
                .user(user)
                .tag(tag)
                .build();
    }

    @Test
    void onCreate_deberiaInicializarFechas() {
        note.onCreate(); // simula @PrePersist
        assertThat(note.getCreatedAt()).isNotNull();
        assertThat(note.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdate_deberiaActualizarUpdatedAt() throws InterruptedException {
        LocalDateTime fixedCreated = LocalDateTime.of(2025, 1, 1, 12, 0);
        note.setCreatedAt(fixedCreated);
        note.setUpdatedAt(fixedCreated);

        Thread.sleep(10);
        note.onUpdate();

        assertThat(note.getCreatedAt()).isEqualTo(fixedCreated);
        assertThat(note.getUpdatedAt()).isAfter(fixedCreated);
    }

    @Test
    void hasTag_deberiaRetornarTrueSiTagNotNull() {
        assertThat(note.hasTag()).isTrue();
        note.setTag(null);
        assertThat(note.hasTag()).isFalse();
    }

    @Test
    void isPending_deberiaRetornarTrueSiNoEstaCompletada() {
        assertThat(note.isPending()).isTrue();
        note.setCompleted(true);
        assertThat(note.isPending()).isFalse();
    }

    @Test
    void assignTag_deberiaActualizarRelacionBidireccional() {
        Tag newTag = Tag.builder().id(UUID.randomUUID()).name("Personal").notes(new ArrayList<>()).build();

        note.assignTag(newTag);

        assertThat(note.getTag()).isEqualTo(newTag);
        assertThat(newTag.getNotes()).contains(note);
    }

    @Test
    void assignTag_conNull_deberiaRemoverTagAnteriorYNoAgregarANuevaLista() {
        Tag oldTag = tag;
        oldTag.getNotes().add(note);

        note.assignTag(null);

        assertThat(note.getTag()).isNull();
        assertThat(oldTag.getNotes()).doesNotContain(note);
    }
}