package com.taskflow.users.controller;

import com.taskflow.shared.dto.MessageResponse;
import com.taskflow.users.dto.*;
import com.taskflow.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller del módulo de usuarios.
 * Todos los endpoints requieren JWT — configurado en SecurityConfig.
 *
 * El userId se extrae del JWT a través de @AuthenticationPrincipal,
 * nunca viene como parámetro del request — así un usuario no puede
 * acceder a datos de otro usuario.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─── GET /users/me ────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    // ─── PATCH /users/me/name ─────────────────────────────────
    @PatchMapping("/me/name")
    public ResponseEntity<UserProfileResponse> updateName(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNameRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.updateName(userId, request));
    }

    // ─── PATCH /users/me/phone ────────────────────────────────
    @PatchMapping("/me/phone")
    public ResponseEntity<UserProfileResponse> updatePhone(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePhoneRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.updatePhone(userId, request));
    }

    // ─── PATCH /users/me/password ─────────────────────────────
    @PatchMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken,
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.changePassword(userId, refreshToken, request));
    }

    // ─── PATCH /users/me/preferences ─────────────────────────
    @PatchMapping("/me/preferences")
    public ResponseEntity<UserProfileResponse.PreferencesDTO> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePreferencesRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.updatePreferences(userId, request));
    }

    // ─── DELETE /users/me ─────────────────────────────────────
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.requestAccountDeletion(userId, request));
    }

    // ─── POST /users/me/cancel-deletion ──────────────────────
    @PostMapping("/me/cancel-deletion")
    public ResponseEntity<MessageResponse> cancelDeletion(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(userService.cancelDeletion(userId));
    }

    // ─── HELPER ───────────────────────────────────────────────
    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}
