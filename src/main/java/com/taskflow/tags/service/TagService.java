package com.taskflow.tags.service;

import com.taskflow.shared.exception.*;
import com.taskflow.tags.dto.*;
import com.taskflow.tags.entity.Tag;
import com.taskflow.tags.mapper.TagMapper;
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
    private final TagMapper tagMapper;

    // ─── GET /tags ────────────────────────────────────────────

    public List<TagResponse> getAllTags(UUID userId) {
        List<Object[]> results = tagRepository.findAllWithNoteCount(userId);

        return results.stream().map(row -> {
            Tag tag = (Tag) row[0];
            Long noteCount = (Long) row[1];
            return tagMapper.toResponse(tag, noteCount.intValue());
        }).collect(Collectors.toList());
    }

    // ─── POST /tags ───────────────────────────────────────────

    @Transactional
    public TagResponse createTag(UUID userId, TagRequest request) {

        if (tagRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new ConflictException("Ya tienes un tag con ese nombre");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Tag tag = tagMapper.toEntity(request);
        tag.setUser(user);
        tagRepository.save(tag);

        return tagMapper.toResponse(tag, 0);
    }

    // ─── PUT /tags/{tagId} ────────────────────────────────────

    @Transactional
    public TagResponse updateTag(UUID userId, UUID tagId, TagRequest request) {

        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado"));

        if (tagRepository.existsByUserIdAndNameAndIdNot(userId, request.getName(), tagId)) {
            throw new ConflictException("Ya tienes un tag con ese nombre");
        }

        tagMapper.updateEntityFromRequest(request, tag);
        tagRepository.save(tag);

        int noteCount = (int) tagRepository.countNotesByTagId(tagId, userId);
        return tagMapper.toResponse(tag, noteCount);
    }

    // ─── DELETE /tags/{tagId} ─────────────────────────────────

    @Transactional
    public void deleteTag(UUID userId, UUID tagId, boolean force) {

        tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado"));

        long noteCount = tagRepository.countNotesByTagId(tagId, userId);

        if (noteCount > 0 && !force) {
            throw new ConflictException(
                    String.format("Este tag tiene %d nota(s) asignada(s). ¿Deseas eliminarlo de todas formas?", noteCount),
                    (int) noteCount
            );
        }

        if (noteCount > 0) {
            tagRepository.detachNotesByTagId(tagId);
        }
        tagRepository.deleteByIdAndUserId(tagId, userId);
    }
}
