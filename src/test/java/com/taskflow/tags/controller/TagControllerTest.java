package com.taskflow.tags.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.shared.exception.ConflictException;
import com.taskflow.shared.exception.ResourceNotFoundException;
import com.taskflow.shared.security.JwtService;
import com.taskflow.tags.dto.TagRequest;
import com.taskflow.tags.dto.TagResponse;
import com.taskflow.tags.service.TagService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    private static final String USER_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";
    private static final UUID USER_UUID = UUID.fromString(USER_UUID_STRING);

    private UUID tagId;
    private TagResponse tagResponse;
    private TagRequest tagRequest;

    @BeforeEach
    void setUp() {
        tagId = UUID.randomUUID();
        tagRequest = new TagRequest("Importante", "#FF0000");
        tagResponse = TagResponse.builder()
                .id(tagId)
                .name("Importante")
                .color("#FF0000")
                .createdAt(LocalDateTime.now())
                .noteCount(0)
                .build();
    }

    // ─── GET /tags ────────────────────────────────────────────
    @Nested
    @DisplayName("GET /tags")
    class GetAllTagsTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe retornar lista de tags del usuario")
        void shouldReturnUserTags() throws Exception {
            when(tagService.getAllTags(USER_UUID)).thenReturn(List.of(tagResponse));

            mockMvc.perform(get("/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(tagId.toString()))
                    .andExpect(jsonPath("$[0].name").value("Importante"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe retornar lista vacía si no hay tags")
        void shouldReturnEmptyList() throws Exception {
            when(tagService.getAllTags(USER_UUID)).thenReturn(List.of());

            mockMvc.perform(get("/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ─── POST /tags ───────────────────────────────────────────
    @Nested
    @DisplayName("POST /tags")
    class CreateTagTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe crear tag y devolver 201")
        void shouldCreateTag() throws Exception {
            when(tagService.createTag(eq(USER_UUID), any(TagRequest.class)))
                    .thenReturn(tagResponse);

            mockMvc.perform(post("/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(tagId.toString()))
                    .andExpect(jsonPath("$.name").value("Importante"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el nombre está vacío")
        void shouldReturn400WhenNameBlank() throws Exception {
            TagRequest invalid = new TagRequest("", "#FF0000");

            mockMvc.perform(post("/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el color no es hex válido")
        void shouldReturn400WhenColorInvalid() throws Exception {
            TagRequest invalid = new TagRequest("Importante", "rojo");

            mockMvc.perform(post("/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 409 si el tag ya existe")
        void shouldReturn409WhenTagNameConflict() throws Exception {
            when(tagService.createTag(eq(USER_UUID), any(TagRequest.class)))
                    .thenThrow(new ConflictException("Ya tienes un tag con ese nombre"));

            mockMvc.perform(post("/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isConflict());
        }
    }

    // ─── PUT /tags/{tagId} ────────────────────────────────────
    @Nested
    @DisplayName("PUT /tags/{tagId}")
    class UpdateTagTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe actualizar tag y devolver 200")
        void shouldUpdateTag() throws Exception {
            when(tagService.updateTag(eq(USER_UUID), eq(tagId), any(TagRequest.class)))
                    .thenReturn(tagResponse);

            mockMvc.perform(put("/tags/{tagId}", tagId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Importante"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 404 si el tag no existe")
        void shouldReturn404WhenTagNotFound() throws Exception {
            when(tagService.updateTag(eq(USER_UUID), eq(tagId), any()))
                    .thenThrow(new ResourceNotFoundException("Tag no encontrado"));

            mockMvc.perform(put("/tags/{tagId}", tagId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 409 si el nombre ya está en uso")
        void shouldReturn409WhenNameConflict() throws Exception {
            when(tagService.updateTag(eq(USER_UUID), eq(tagId), any()))
                    .thenThrow(new ConflictException("Ya tienes un tag con ese nombre"));

            mockMvc.perform(put("/tags/{tagId}", tagId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tagRequest)))
                    .andExpect(status().isConflict());
        }
    }

    // ─── DELETE /tags/{tagId} ─────────────────────────────────
    @Nested
    @DisplayName("DELETE /tags/{tagId}")
    class DeleteTagTests {

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe eliminar tag y devolver 204")
        void shouldDeleteTagAndReturnNoContent() throws Exception {
            doNothing().when(tagService).deleteTag(USER_UUID, tagId, false);

            mockMvc.perform(delete("/tags/{tagId}", tagId))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe soportar force=true como query param")
        void shouldPassForceParameter() throws Exception {
            doNothing().when(tagService).deleteTag(USER_UUID, tagId, true);

            mockMvc.perform(delete("/tags/{tagId}?force=true", tagId))
                    .andExpect(status().isNoContent());

            verify(tagService).deleteTag(USER_UUID, tagId, true);
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 404 si el tag no existe")
        void shouldReturn404WhenTagNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Tag no encontrado"))
                    .when(tagService).deleteTag(USER_UUID, tagId, false);

            mockMvc.perform(delete("/tags/{tagId}", tagId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 409 si tiene notas y force=false")
        void shouldReturn409WhenHasNotesAndNotForce() throws Exception {
            doThrow(new ConflictException("Este tag tiene 3 nota(s) asignada(s)."))
                    .when(tagService).deleteTag(USER_UUID, tagId, false);

            mockMvc.perform(delete("/tags/{tagId}", tagId))
                    .andExpect(status().isConflict());
        }
    }
}