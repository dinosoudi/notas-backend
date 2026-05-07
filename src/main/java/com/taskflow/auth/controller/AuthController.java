package com.taskflow.auth.controller;

import com.taskflow.auth.dto.*;
import com.taskflow.auth.service.AuthService;
import com.taskflow.shared.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller del módulo de autenticación.
 *
 * Responsabilidades:
 * - Recibir el request HTTP
 * - Validar con @Valid (Bean Validation)
 * - Delegar al AuthService
 * - Devolver el ResponseEntity con el status correcto
 *
 * El Controller NO tiene lógica de negocio — solo orquesta.
 * Toda la lógica está en AuthService.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── POST /auth/register ──────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── POST /auth/verify-email ──────────────────────────────
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @RequestBody VerifyEmailRequest request) {

        AuthResponse response = authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/resend-verification ──────────────────────
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(
                "Si esa cuenta existe y no está verificada, recibirás un nuevo correo."));
    }

    // ─── POST /auth/login ─────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/forgot-password ───────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/verify-reset-code ────────────────────────
    @PostMapping("/verify-reset-code")
    public ResponseEntity<VerifyResetCodeResponse> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request) {

        VerifyResetCodeResponse response = authService.verifyResetCode(request);
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/reset-password ───────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/refresh ───────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    // ─── POST /auth/logout ────────────────────────────────────
    // Requiere JWT — el usuario debe estar autenticado para hacer logout
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Sesión cerrada correctamente"));
    }

    // ─── POST /auth/logout-all ────────────────────────────────
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        int closedSessions = authService.logoutAll(userId);

        MessageResponse response = new MessageResponse("Se cerraron todas las sesiones activas");
        response.setSessionsClosedCount(closedSessions);
        return ResponseEntity.ok(response);
    }
}
