package com.taskflow.notes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.notes.dto.NotePageResponse;
import com.taskflow.notes.dto.NoteRequest;
import com.taskflow.notes.dto.NoteResponse;
import com.taskflow.notes.service.NoteService;
import com.taskflow.shared.exception.ResourceNotFoundException;
import com.taskflow.shared.security.JwtService;
import com.taskflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean   // o @MockBean si usas Spring Boot <3.4.0
    private JwtService jwtService;

    @MockitoBean private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoteService noteService;

    // UUID válido que se usará como userId en el UserDetails simulado
    private static final String USER_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";
    private static final UUID USER_UUID = UUID.fromString(USER_UUID_STRING);

    // Datos de ejemplo
    private NoteResponse noteResponse;
    private NotePageResponse notePageResponse;
    private UUID noteId;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();

        NoteResponse.TagSummary tagSummary = NoteResponse.TagSummary.builder()
                .id(UUID.randomUUID())
                .name("Importante")
                .color("#FF0000")
                .build();

        noteResponse = NoteResponse.builder()
                .id(noteId)
                .content("Comprar pan")
                .completed(false)
                .tag(tagSummary)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        notePageResponse = NotePageResponse.builder()
                .content(List.of(noteResponse))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    // ─── GET /notes ───────────────────────────────────────────
    @Nested
    @DisplayName("GET /notes")
    class GetAllNotesTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver página de notas con parámetros por defecto")
        void shouldReturnNotePageWithDefaultParams() throws Exception {
            when(noteService.getAllNotes(USER_UUID, 0, 20, "updatedAt", "DESC"))
                    .thenReturn(notePageResponse);

            mockMvc.perform(get("/notes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(noteService).getAllNotes(USER_UUID, 0, 20, "updatedAt", "DESC");
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe pasar parámetros personalizados al servicio")
        void shouldPassCustomPaginationAndSort() throws Exception {
            when(noteService.getAllNotes(any(), anyInt(), anyInt(), anyString(), anyString()))
                    .thenReturn(notePageResponse);

            mockMvc.perform(get("/notes")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "createdAt")
                            .param("direction", "ASC"))
                    .andExpect(status().isOk());

            verify(noteService).getAllNotes(USER_UUID, 1, 5, "createdAt", "ASC");
        }
    }

    // ─── GET /notes/{noteId} ──────────────────────────────────
    @Nested
    @DisplayName("GET /notes/{noteId}")
    class GetNoteByIdTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver la nota cuando existe")
        void shouldReturnNoteWhenFound() throws Exception {
            when(noteService.getNoteById(USER_UUID, noteId)).thenReturn(noteResponse);

            mockMvc.perform(get("/notes/{noteId}", noteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(noteId.toString()))
                    .andExpect(jsonPath("$.content").value("Comprar pan"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 404 cuando la nota no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(noteService.getNoteById(USER_UUID, noteId))
                    .thenThrow(new ResourceNotFoundException("Nota no encontrada"));

            mockMvc.perform(get("/notes/{noteId}", noteId))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── GET /notes/tag/{tagId} ───────────────────────────────
    @Nested
    @DisplayName("GET /notes/tag/{tagId}")
    class GetNotesByTagTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver página de notas filtradas por tag")
        void shouldReturnNotesByTag() throws Exception {
            UUID tagId = UUID.randomUUID();
            when(noteService.getNotesByTag(USER_UUID, tagId, 0, 20))
                    .thenReturn(notePageResponse);

            mockMvc.perform(get("/notes/tag/{tagId}", tagId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(noteService).getNotesByTag(USER_UUID, tagId, 0, 20);
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 404 cuando el tag no pertenece al usuario")
        void shouldReturn404WhenTagNotFound() throws Exception {
            UUID tagId = UUID.randomUUID();
            when(noteService.getNotesByTag(USER_UUID, tagId, 0, 20))
                    .thenThrow(new ResourceNotFoundException("Tag no encontrado"));

            mockMvc.perform(get("/notes/tag/{tagId}", tagId))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── POST /notes ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /notes")
    class CreateNoteTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe crear nota y devolver 201 con el recurso creado")
        void shouldCreateNoteAndReturnCreated() throws Exception {
            NoteRequest request = NoteRequest.builder()
                    .content("Nueva nota")
                    .completed(false)
                    .tagId(null)
                    .build();

            when(noteService.createNote(eq(USER_UUID), any(NoteRequest.class)))
                    .thenReturn(noteResponse);

            mockMvc.perform(post("/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(noteId.toString()));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el contenido está vacío")
        void shouldReturn400WhenContentIsBlank() throws Exception {
            NoteRequest request = NoteRequest.builder()
                    .content("")  // @NotBlank
                    .build();

            mockMvc.perform(post("/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el contenido supera 5000 caracteres")
        void shouldReturn400WhenContentExceedsMaxSize() throws Exception {
            String tooLong = "a".repeat(5001);
            NoteRequest request = NoteRequest.builder()
                    .content(tooLong)
                    .build();

            mockMvc.perform(post("/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── PUT /notes/{noteId} ──────────────────────────────────
    @Nested
    @DisplayName("PUT /notes/{noteId}")
    class UpdateNoteTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe actualizar la nota y devolver 200")
        void shouldUpdateNote() throws Exception {
            NoteRequest request = NoteRequest.builder()
                    .content("Contenido actualizado")
                    .completed(true)
                    .tagId(UUID.randomUUID())
                    .build();

            when(noteService.updateNote(eq(USER_UUID), eq(noteId), any(NoteRequest.class)))
                    .thenReturn(noteResponse);

            mockMvc.perform(put("/notes/{noteId}", noteId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(noteId.toString()));

            verify(noteService).updateNote(eq(USER_UUID), eq(noteId), any(NoteRequest.class));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 404 si la nota a actualizar no existe")
        void shouldReturn404WhenNoteNotFound() throws Exception {
            NoteRequest request = NoteRequest.builder()
                    .content("Contenido")
                    .build();

            when(noteService.updateNote(eq(USER_UUID), eq(noteId), any()))
                    .thenThrow(new ResourceNotFoundException("Nota no encontrada"));

            mockMvc.perform(put("/notes/{noteId}", noteId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── DELETE /notes/{noteId} ───────────────────────────────
    @Nested
    @DisplayName("DELETE /notes/{noteId}")
    class DeleteNoteTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe eliminar la nota y devolver 204")
        void shouldDeleteNoteAndReturnNoContent() throws Exception {
            doNothing().when(noteService).deleteNote(USER_UUID, noteId);

            mockMvc.perform(delete("/notes/{noteId}", noteId))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(noteService).deleteNote(USER_UUID, noteId);
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 404 si la nota a eliminar no existe")
        void shouldReturn404WhenDeletingNonExistentNote() throws Exception {
            doThrow(new ResourceNotFoundException("Nota no encontrada"))
                    .when(noteService).deleteNote(USER_UUID, noteId);

            mockMvc.perform(delete("/notes/{noteId}", noteId))
                    .andExpect(status().isNotFound());
        }
    }
}