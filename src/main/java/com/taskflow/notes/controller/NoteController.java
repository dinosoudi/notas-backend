package com.taskflow.notes.controller;

import com.taskflow.notes.dto.*;
import com.taskflow.notes.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    // ─── GET /notes ───────────────────────────────────────────
    @GetMapping
    public ResponseEntity<NotePageResponse> getAllNotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(noteService.getAllNotes(userId, page, size, sort, direction));
    }

    // ─── GET /notes/{noteId} ──────────────────────────────────
    @GetMapping("/{noteId}")
    public ResponseEntity<NoteResponse> getNoteById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(noteService.getNoteById(userId, noteId));
    }

    // ─── GET /notes/tag/{tagId} ───────────────────────────────
    @GetMapping("/tag/{tagId}")
    public ResponseEntity<NotePageResponse> getNotesByTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(noteService.getNotesByTag(userId, tagId, page, size));
    }

    // ─── POST /notes ──────────────────────────────────────────
    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoteRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.createNote(userId, request));
    }

    // ─── PUT /notes/{noteId} ──────────────────────────────────
    @PutMapping("/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(noteService.updateNote(userId, noteId, request));
    }

    // ─── DELETE /notes/{noteId} ───────────────────────────────
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID noteId) {

        UUID userId = extractUserId(userDetails);
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.noContent().build(); // 204 sin body
    }

    // ─── HELPER ───────────────────────────────────────────────
    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
