package com.taskflow.notes.service;

import com.taskflow.notes.dto.*;
import com.taskflow.notes.entity.Note;
import com.taskflow.notes.repository.NoteRepository;
import com.taskflow.shared.exception.*;
import com.taskflow.tags.entity.Tag;
import com.taskflow.tags.repository.TagRepository;
import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    // ─── GET /notes ───────────────────────────────────────────

    public NotePageResponse getAllNotes(UUID userId, int page, int size,
                                        String sort, String direction) {
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Note> notePage = noteRepository.findByUserId(userId, pageable);

        return toPageResponse(notePage);
    }

    // ─── GET /notes/{noteId} ──────────────────────────────────

    public NoteResponse getNoteById(UUID userId, UUID noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
        return toResponse(note);
    }

    // ─── GET /notes/tag/{tagId} ───────────────────────────────

    public NotePageResponse getNotesByTag(UUID userId, UUID tagId, int page, int size) {

        // Verificar que el tag existe y pertenece al usuario
        tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag no encontrado"));

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Note> notePage = noteRepository.findByUserIdAndTagId(userId, tagId, pageable);
        return toPageResponse(notePage);
    }

    // ─── POST /notes ──────────────────────────────────────────

    @Transactional
    public NoteResponse createNote(UUID userId, NoteRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Resolver el tag si se envió
        Tag tag = resolveTag(userId, request.getTagId());

        Note note = Note.builder()
                .user(user)
                .tag(tag)
                .content(request.getContent().trim())
                .completed(request.getCompleted() != null ? request.getCompleted() : false)
                .build();

        noteRepository.save(note);
        return toResponse(note);
    }

    // ─── PUT /notes/{noteId} ──────────────────────────────────

    @Transactional
    public NoteResponse updateNote(UUID userId, UUID noteId, NoteRequest request) {

        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));

        // Resolver el tag — null desvincula el tag actual
        Tag tag = resolveTag(userId, request.getTagId());

        note.setContent(request.getContent().trim());
        note.setCompleted(request.getCompleted() != null ? request.getCompleted() : false);
        note.setTag(tag);

        noteRepository.save(note);
        return toResponse(note);
    }

    // ─── DELETE /notes/{noteId} ───────────────────────────────

    @Transactional
    public void deleteNote(UUID userId, UUID noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
        noteRepository.delete(note);
    }

    // ─── HELPERS PRIVADOS ─────────────────────────────────────

    /**
     * Resuelve el tag por id verificando que pertenezca al usuario.
     * Devuelve null si tagId es null — la nota queda sin tag.
     */
    private Tag resolveTag(UUID userId, UUID tagId) {
        if (tagId == null) return null;
        return tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El tag seleccionado no existe", "tagId", tagId));
    }

    private NoteResponse toResponse(Note note) {
        NoteResponse.TagSummary tagSummary = null;
        if (note.getTag() != null) {
            tagSummary = NoteResponse.TagSummary.builder()
                    .id(note.getTag().getId())
                    .name(note.getTag().getName())
                    .color(note.getTag().getColor())
                    .build();
        }

        return NoteResponse.builder()
                .id(note.getId())
                .content(note.getContent())
                .completed(note.getCompleted())
                .tag(tagSummary)
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    private NotePageResponse toPageResponse(Page<Note> page) {
        return NotePageResponse.builder()
                .content(page.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
