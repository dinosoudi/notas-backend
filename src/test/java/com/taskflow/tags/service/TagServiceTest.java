package com.taskflow.tags.service;

import com.taskflow.shared.exception.ConflictException;
import com.taskflow.shared.exception.ResourceNotFoundException;
import com.taskflow.tags.dto.TagRequest;
import com.taskflow.tags.dto.TagResponse;
import com.taskflow.tags.entity.Tag;
import com.taskflow.tags.mapper.TagMapper;
import com.taskflow.tags.repository.TagRepository;
import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock private TagRepository tagRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    private UUID userId;
    private UUID tagId;
    private User user;
    private Tag tag;
    private TagRequest tagRequest;
    private TagResponse tagResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tagId = UUID.randomUUID();

        user = User.builder().id(userId).build();

        tag = Tag.builder()
                .id(tagId)
                .user(user)
                .name("Importante")
                .color("#FF0000")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tagRequest = new TagRequest("Trabajo", "#00FF00");

        tagResponse = TagResponse.builder()
                .id(tagId)
                .name("Trabajo")
                .color("#00FF00")
                .createdAt(tag.getCreatedAt())
                .noteCount(0)
                .build();
    }

    // ─── getAllTags ───────────────────────────────────────────
    @Nested
    @DisplayName("getAllTags")
    class GetAllTags {

        @Test
        @DisplayName("Debe retornar lista de tags con conteo de notas")
        void shouldReturnTagsWithNoteCount() {
            Object[] row = new Object[]{tag, 5L};
            List<Object[]> mockResult = new ArrayList<>();
            mockResult.add(row);

            // Forzamos el tipo de retorno
            doReturn(mockResult).when(tagRepository).findAllWithNoteCount(userId);
            when(tagMapper.toResponse(tag, 5)).thenReturn(tagResponse);

            List<TagResponse> result = tagService.getAllTags(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Trabajo");
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay tags")
        void shouldReturnEmptyList() {
            when(tagRepository.findAllWithNoteCount(userId)).thenReturn(List.of());

            List<TagResponse> result = tagService.getAllTags(userId);

            assertThat(result).isEmpty();
        }
    }

    // ─── createTag ────────────────────────────────────────────
    @Nested
    @DisplayName("createTag")
    class CreateTag {

        @Test
        @DisplayName("Debe crear tag correctamente")
        void shouldCreateTag() {
            when(tagRepository.existsByUserIdAndName(userId, "Trabajo")).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tagMapper.toEntity(tagRequest)).thenReturn(tag);
            when(tagMapper.toResponse(tag, 0)).thenReturn(tagResponse);
            when(tagRepository.save(tag)).thenReturn(tag);

            TagResponse response = tagService.createTag(userId, tagRequest);

            assertThat(response.getName()).isEqualTo("Trabajo");
            verify(tagRepository).save(tag);
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el nombre ya existe")
        void shouldThrowConflictIfNameExists() {
            when(tagRepository.existsByUserIdAndName(userId, "Trabajo")).thenReturn(true);

            assertThatThrownBy(() -> tagService.createTag(userId, tagRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Ya tienes un tag con ese nombre");
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe")
        void shouldThrowIfUserNotFound() {
            when(tagRepository.existsByUserIdAndName(userId, "Trabajo")).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.createTag(userId, tagRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuario no encontrado");
        }
    }

    // ─── updateTag ────────────────────────────────────────────
    @Nested
    @DisplayName("updateTag")
    class UpdateTag {

        @Test
        @DisplayName("Debe actualizar tag existente")
        void shouldUpdateTag() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            when(tagRepository.existsByUserIdAndNameAndIdNot(userId, "Trabajo", tagId)).thenReturn(false);
            when(tagRepository.countNotesByTagId(tagId, userId)).thenReturn(2L);
            when(tagMapper.toResponse(tag, 2)).thenReturn(tagResponse);

            TagResponse response = tagService.updateTag(userId, tagId, tagRequest);

            assertThat(response).isNotNull();
            verify(tagMapper).updateEntityFromRequest(tagRequest, tag);
            verify(tagRepository).save(tag);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el tag no existe")
        void shouldThrowIfTagNotFound() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.updateTag(userId, tagId, tagRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Tag no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el nombre ya existe en otro tag")
        void shouldThrowConflictIfNameTaken() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            when(tagRepository.existsByUserIdAndNameAndIdNot(userId, "Trabajo", tagId)).thenReturn(true);

            assertThatThrownBy(() -> tagService.updateTag(userId, tagId, tagRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Ya tienes un tag con ese nombre");
        }
    }

    // ─── deleteTag ────────────────────────────────────────────
    @Nested
    @DisplayName("deleteTag")
    class DeleteTag {

        @Test
        @DisplayName("Debe eliminar tag sin notas")
        void shouldDeleteTagWithoutNotes() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            when(tagRepository.countNotesByTagId(tagId, userId)).thenReturn(0L);

            tagService.deleteTag(userId, tagId, false);

            verify(tagRepository).deleteByIdAndUserId(tagId, userId);
            verify(tagRepository, never()).detachNotesByTagId(any());
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si tiene notas y force=false")
        void shouldThrowConflictIfHasNotesAndNotForce() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            when(tagRepository.countNotesByTagId(tagId, userId)).thenReturn(3L);

            assertThatThrownBy(() -> tagService.deleteTag(userId, tagId, false))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("3 nota(s)");
        }

        @Test
        @DisplayName("Debe eliminar tag con notas si force=true")
        void shouldDeleteTagWithNotesWhenForced() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
            when(tagRepository.countNotesByTagId(tagId, userId)).thenReturn(3L);

            tagService.deleteTag(userId, tagId, true);

            verify(tagRepository).detachNotesByTagId(tagId);
            verify(tagRepository).deleteByIdAndUserId(tagId, userId);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el tag no existe")
        void shouldThrowIfTagNotFound() {
            when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.deleteTag(userId, tagId, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Tag no encontrado");
        }
    }
}