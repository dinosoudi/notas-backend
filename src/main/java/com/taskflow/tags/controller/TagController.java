package com.taskflow.tags.controller;

import com.taskflow.tags.dto.*;
import com.taskflow.tags.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // ─── GET /tags ────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(tagService.getAllTags(userId));
    }

    // ─── POST /tags ───────────────────────────────────────────
    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TagRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tagService.createTag(userId, request));
    }

    // ─── PUT /tags/{tagId} ────────────────────────────────────
    @PutMapping("/{tagId}")
    public ResponseEntity<TagResponse> updateTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID tagId,
            @Valid @RequestBody TagRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(tagService.updateTag(userId, tagId, request));
    }

    // ─── DELETE /tags/{tagId} ─────────────────────────────────
    // force=false por default — devuelve 409 si tiene notas
    // force=true — borra el tag y desvincula sus notas con el SP
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID tagId,
            @RequestParam(defaultValue = "false") boolean force) {

        UUID userId = extractUserId(userDetails);
        tagService.deleteTag(userId, tagId, force);
        return ResponseEntity.noContent().build(); // 204 sin body
    }

    // ─── HELPER ───────────────────────────────────────────────
    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
