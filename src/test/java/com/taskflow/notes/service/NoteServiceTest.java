package com.taskflow.notes.service;

import com.taskflow.notes.dto.NoteRequest;
import com.taskflow.notes.dto.NoteResponse;
import com.taskflow.notes.dto.NotePageResponse;
import com.taskflow.notes.entity.Note;
import com.taskflow.notes.repository.NoteRepository;
import com.taskflow.shared.exception.ResourceNotFoundException;
import com.taskflow.tags.entity.Tag;
import com.taskflow.tags.repository.TagRepository;
import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteRepository noteRepository;
    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private NoteService noteService;

    private UUID userId;
    private UUID noteId;
    private UUID tagId;
    private User user;
    private Tag tag;
    private Note note;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        noteId = UUID.randomUUID();
        tagId = UUID.randomUUID();

        user = User.builder().id(userId).build();

        tag = Tag.builder()
                .id(tagId)
                .name("Importante")
                .color("#FF0000")
                .user(user)
                .build();

        note = Note.builder()
                .id(noteId)
                .user(user)
                .tag(tag)
                .content("Comprar pan")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── GET ALL NOTES ────────────────────────────────────────
    @Nested
    @DisplayName("getAllNotes")
    class GetAllNotesTests {

        @Test
        @DisplayName("Debe retornar página de notas con orden por defecto DESC")
        void shouldReturnPageWithDefaultSort() {
            Page<Note> page = new PageImpl<>(List.of(note), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")), 1);
            when(noteRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

            NotePageResponse response = noteService.getAllNotes(userId, 0, 20, "updatedAt", "DESC");

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(noteId);
            assertThat(response.getTotalElements()).isEqualTo(1);
            verify(noteRepository).findByUserId(userId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));
        }

        @Test
        @DisplayName("Debe interpretar ASC correctamente")
        void shouldUseAscendingSortWhenAscProvided() {
            Page<Note> page = new PageImpl<>(List.of(note));
            when(noteRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

            noteService.getAllNotes(userId, 0, 10, "createdAt", "ASC");

            verify(noteRepository).findByUserId(userId, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt")));
        }

        @Test
        @DisplayName("Debe tratar cualquier dirección desconocida como DESC")
        void shouldDefaultToDescIfUnknownDirection() {
            Page<Note> page = new PageImpl<>(List.of(note));
            when(noteRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

            noteService.getAllNotes(userId, 0, 10, "content", "INVALID");

            verify(noteRepository).findByUserId(userId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "content")));
        }
    }

    // ─── GET NOTE BY ID ───────────────────────────────────────
    @Nested
    @DisplayName("getNoteById")
    class GetNoteByIdTests {

        @Test
        @DisplayName("Debe retornar la nota si existe")
        void shouldReturnNoteWhenFound() {
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

            NoteResponse response = noteService.getNoteById(userId, noteId);

            assertThat(response.getId()).isEqualTo(noteId);
            assertThat(response.getContent()).isEqualTo("Comprar pan");
            assertThat(response.getTag().getId()).isEqualTo(tagId);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si no existe")
        void shouldThrowExceptionWhenNotFound() {
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.getNoteById(userId, noteId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Nota no encontrada");
        }
    }

    // ─── GET NOTES BY TAG ─────────────────────────────────────
    @Nested
    @DisplayName("getNotesByTag")
    class GetNotesByTagTests {

        @Test
        @DisplayName("Debe retornar página de notas cuando el tag existe")
        void shouldReturnNotesForValidTag() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            Page<Note> page = new PageImpl<>(List.of(note));
            when(noteRepository.findByUserIdAndTagId(eq(userId), eq(tagId), any(Pageable.class))).thenReturn(page);

            NotePageResponse response = noteService.getNotesByTag(userId, tagId, 0, 10);

            assertThat(response.getContent()).hasSize(1);
            verify(tagRepository).findByIdAndUserId(tagId, userId);
            verify(noteRepository).findByUserIdAndTagId(userId, tagId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt")));
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el tag no existe")
        void shouldThrowExceptionWhenTagNotFound() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.getNotesByTag(userId, tagId, 0, 10))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Tag no encontrado");
        }
    }

    // ─── CREATE NOTE ──────────────────────────────────────────
    @Nested
    @DisplayName("createNote")
    class CreateNoteTests {

        @Test
        @DisplayName("Debe crear nota con usuario existente y sin tag")
        void shouldCreateNoteWithoutTag() {
            NoteRequest request = NoteRequest.builder()
                    .content("  Nota nueva  ")
                    .completed(false)
                    .tagId(null)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            NoteResponse response = noteService.createNote(userId, request);

            assertThat(response.getContent()).isEqualTo("Nota nueva"); // contenido trimmeado
            assertThat(response.getCompleted()).isFalse();
            assertThat(response.getTag()).isNull();

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            Note savedNote = noteCaptor.getValue();
            assertThat(savedNote.getContent()).isEqualTo("Nota nueva");
            assertThat(savedNote.getUser()).isEqualTo(user);
            assertThat(savedNote.getTag()).isNull();
        }

        @Test
        @DisplayName("Debe crear nota con tag existente")
        void shouldCreateNoteWithTag() {
            NoteRequest request = NoteRequest.builder()
                    .content("Otra nota")
                    .completed(true)
                    .tagId(tagId)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

            NoteResponse response = noteService.createNote(userId, request);

            assertThat(response.getCompleted()).isTrue();
            assertThat(response.getTag().getId()).isEqualTo(tagId);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe")
        void shouldThrowExceptionWhenUserNotFound() {
            NoteRequest request = NoteRequest.builder().content("Contenido").build();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.createNote(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuario no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el tag enviado no existe")
        void shouldThrowExceptionWhenTagNotFound() {
            NoteRequest request = NoteRequest.builder()
                    .content("Contenido")
                    .tagId(tagId)
                    .build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.createNote(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── UPDATE NOTE ──────────────────────────────────────────
    @Nested
    @DisplayName("updateNote")
    class UpdateNoteTests {

        @Test
        @DisplayName("Debe actualizar contenido y completado, y desvincular el tag si tagId es null")
        void shouldUpdateNoteFieldsAndRemoveTag() {
            NoteRequest request = NoteRequest.builder()
                    .content("Nuevo contenido")
                    .completed(true)
                    .tagId(null)  // esto desvincula el tag
                    .build();

            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenReturn(note);

            // Simular que el servicio desvincula el tag
            note.setTag(null); // opcional, si tu mock lo permite

            NoteResponse response = noteService.updateNote(userId, noteId, request);

            assertThat(response.getContent()).isEqualTo("Nuevo contenido");
            assertThat(response.getCompleted()).isTrue();
            assertThat(response.getTag()).isNull(); // tag debe ser null
        }

        @Test
        @DisplayName("Debe cambiar el tag de la nota")
        void shouldChangeTag() {
            UUID newTagId = UUID.randomUUID();
            Tag newTag = Tag.builder().id(newTagId).name("Trabajo").color("#00FF00").user(user).build();

            NoteRequest request = NoteRequest.builder()
                    .content("Nota con nuevo tag")
                    .completed(false)
                    .tagId(newTagId)
                    .build();

            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));
            when(tagRepository.findByIdAndUserId(newTagId, userId)).thenReturn(Optional.of(newTag));
            when(noteRepository.save(any(Note.class))).thenReturn(note);

            NoteResponse response = noteService.updateNote(userId, noteId, request);

            assertThat(response.getTag().getId()).isEqualTo(newTagId);
        }

        @Test
        @DisplayName("Debe desvincular el tag si tagId es null explícitamente")
        void shouldRemoveTagWhenTagIdNull() {
            // creamos una nota que originalmente tenía tag
            Note noteWithTag = Note.builder()
                    .id(noteId).user(user).tag(tag)
                    .content("Contenido").completed(false).build();

            NoteRequest request = NoteRequest.builder()
                    .content("Sin tag")
                    .tagId(null) // null -> se desvincula (setTag(null))
                    .build();

            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(noteWithTag));
            when(noteRepository.save(any(Note.class))).thenReturn(noteWithTag);

            NoteResponse response = noteService.updateNote(userId, noteId, request);

            assertThat(response.getTag()).isNull();
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la nota no existe")
        void shouldThrowExceptionWhenNoteNotFound() {
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());
            NoteRequest request = NoteRequest.builder().content("X").build();

            assertThatThrownBy(() -> noteService.updateNote(userId, noteId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Nota no encontrada");
        }
    }

    // ─── DELETE NOTE ──────────────────────────────────────────
    @Nested
    @DisplayName("deleteNote")
    class DeleteNoteTests {

        @Test
        @DisplayName("Debe eliminar la nota si existe")
        void shouldDeleteNoteWhenFound() {
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

            noteService.deleteNote(userId, noteId);

            verify(noteRepository).delete(note);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la nota no existe")
        void shouldThrowExceptionWhenNoteNotFound() {
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> noteService.deleteNote(userId, noteId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Nota no encontrada");
        }
    }
}