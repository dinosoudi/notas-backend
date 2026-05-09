package com.taskflow.tags.service;

import com.taskflow.shared.exception.*;
import com.taskflow.tags.dto.*;
import com.taskflow.tags.entity.Tag;
import com.taskflow.tags.repository.TagRepository;
import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    // ─── GET /tags ────────────────────────────────────────────

    public List<TagResponse> getAllTags(UUID userId) {
        // Traer tags con noteCount en una sola query — evita N+1
        List<Object[]> results = tagRepository.findAllWithNoteCount(userId);

        return results.stream().map(row -> {
            Tag tag = (Tag) row[0];
            Long noteCount = (Long) row[1];
            return toResponse(tag, noteCount.intValue());
        }).collect(Collectors.toList());
    }

    // ─── POST /tags ───────────────────────────────────────────

    @Transactional
    public TagResponse createTag(UUID userId, TagRequest request) {

        // 1. Verificar nombre único por usuario
        if (tagRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new ConflictException("Ya tienes un tag con ese nombre");
        }

        // 2. Obtener referencia al usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 3. Crear el tag
        Tag tag = Tag.builder()
                .user(user)
                .name(request.getName().trim())
                .color(request.getColor())
                .build();

        tagRepository.save(tag);
        return toResponse(tag, 0);
    }

    // ─── PUT /tags/{tagId} ────────────────────────────────────

    @Transactional
    public TagResponse updateTag(UUID userId, UUID tagId, TagRequest request) {

        // 1. Verificar que el tag existe y pertenece al usuario
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado"));

        // 2. Verificar nombre único — excluyendo el tag actual
        if (tagRepository.existsByUserIdAndNameAndIdNot(userId, request.getName(), tagId)) {
            throw new ConflictException("Ya tienes un tag con ese nombre");
        }

        // 3. Actualizar
        tag.setName(request.getName().trim());
        tag.setColor(request.getColor());
        tagRepository.save(tag);

        int noteCount = (int) tagRepository.countNotesByTagId(tagId, userId);
        return toResponse(tag, noteCount);
    }

    // ─── DELETE /tags/{tagId} ─────────────────────────────────

    @Transactional
    public void deleteTag(UUID userId, UUID tagId, boolean force) {

        // 1. Verificar que el tag existe y pertenece al usuario
        tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado"));

        // 2. Contar notas afectadas
        long noteCount = tagRepository.countNotesByTagId(tagId, userId);

        // 3. Si tiene notas y no es force — devolver 409 para que el frontend confirme
        if (noteCount > 0 && !force) {
            throw new ConflictException(
                    String.format("Este tag tiene %d nota(s) asignada(s). ¿Deseas eliminarlo de todas formas?", noteCount),
                    (int) noteCount
            );
        }

        // 4. Borrar usando el SP — desvincula notas y borra tag en una transacción
        tagRepository.deleteTagCascade(tagId, userId);
    }

    // ─── HELPER ───────────────────────────────────────────────

    private TagResponse toResponse(Tag tag, int noteCount) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .noteCount(noteCount)
                .createdAt(tag.getCreatedAt())
                .build();
    }
}