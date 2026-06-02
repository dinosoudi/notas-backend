package com.taskflow.notes.mapper;

import com.taskflow.notes.dto.NoteRequest;
import com.taskflow.notes.dto.NoteResponse;
import com.taskflow.notes.entity.Note;
import com.taskflow.tags.entity.Tag;
import com.taskflow.users.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoteMapperTest {

    private final NoteMapper mapper = Mappers.getMapper(NoteMapper.class);

    @Test
    void toResponse_deberiaMapearNoteANoteResponse() {
        Tag tag = Tag.builder()
                .id(UUID.randomUUID())
                .name("Personal")
                .color("#FF0000")
                .build();
        Note note = Note.builder()
                .id(UUID.randomUUID())
                .content("Hola mundo")
                .completed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tag(tag)
                .build();

        NoteResponse response = mapper.toResponse(note);

        assertThat(response.getId()).isEqualTo(note.getId());
        assertThat(response.getContent()).isEqualTo("Hola mundo");
        assertThat(response.getCompleted()).isTrue();
        assertThat(response.getTag()).isNotNull();
        assertThat(response.getTag().getId()).isEqualTo(tag.getId());
        assertThat(response.getTag().getName()).isEqualTo("Personal");
        assertThat(response.getTag().getColor()).isEqualTo("#FF0000");
    }

    @Test
    void toEntity_deberiaIgnorarCamposDeRelacion() {
        NoteRequest request = NoteRequest.builder()
                .content("Nueva nota")
                .completed(false)
                .tagId(UUID.randomUUID()) // tagId se ignora en el mapeo directo, se resuelve en service
                .build();

        Note note = mapper.toEntity(request);

        assertThat(note.getId()).isNull();
        assertThat(note.getContent()).isEqualTo("Nueva nota");
        assertThat(note.getCompleted()).isFalse();
        assertThat(note.getUser()).isNull();
        assertThat(note.getTag()).isNull();
        assertThat(note.getCreatedAt()).isNull();
        assertThat(note.getUpdatedAt()).isNull();
    }

    @Test
    void updateEntityFromRequest_deberiaActualizarSoloCamposPermitidos() {
        Note existingNote = Note.builder()
                .id(UUID.randomUUID())
                .content("Contenido viejo")
                .completed(false)
                .build();

        NoteRequest request = NoteRequest.builder()
                .content("Nuevo contenido")
                .completed(true)
                .tagId(UUID.randomUUID())
                .build();

        mapper.updateEntityFromRequest(request, existingNote);

        assertThat(existingNote.getContent()).isEqualTo("Nuevo contenido");
        assertThat(existingNote.getCompleted()).isTrue();
        assertThat(existingNote.getId()).isNotNull(); // no debe cambiar
        // tag, user, createdAt, updatedAt se mantienen igual (no se tocan)
    }
}