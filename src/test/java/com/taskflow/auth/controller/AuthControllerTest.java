package com.taskflow.auth.controller;

import com.taskflow.auth.dto.*;
import com.taskflow.auth.service.AuthService;
import com.taskflow.shared.dto.MessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController unit tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    // ─── Registro ─────────────────────────────────────────────
    @Test
    @DisplayName("POST /register → 201 Created + AuthResponse")
    void registerShouldReturn201AndAuthResponse() {
        RegisterRequest request = new RegisterRequest(); // Rellenar datos según tu DTO
        AuthResponse expectedResponse = new AuthResponse();
        given(authService.register(request)).willReturn(expectedResponse);

        ResponseEntity<AuthResponse> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(expectedResponse);
        then(authService).should().register(request);
    }

    // ─── Verificar email ─────────────────────────────────────
    @Test
    @DisplayName("POST /verify-email → 200 OK + AuthResponse")
    void verifyEmailShouldReturn200AndAuthResponse() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("token123");
        AuthResponse expectedResponse = new AuthResponse();
        given(authService.verifyEmail("token123")).willReturn(expectedResponse);

        ResponseEntity<AuthResponse> result = controller.verifyEmail(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(expectedResponse);
        then(authService).should().verifyEmail("token123");
    }

    // ─── Reenviar verificación ────────────────────────────────
    @Test
    @DisplayName("POST /resend-verification → 200 + MessageResponse con mensaje esperado")
    void resendVerificationShouldReturn200AndMessage() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@example.com");

        ResponseEntity<MessageResponse> result = controller.resendVerification(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMessage())
                .isEqualTo("Si esa cuenta existe y no está verificada, recibirás un nuevo correo.");
        then(authService).should().resendVerification("test@example.com");
    }

    // ─── Login ────────────────────────────────────────────────
    @Test
    @DisplayName("POST /login → 200 + AuthResponse")
    void loginShouldReturn200AndAuthResponse() {
        LoginRequest request = new LoginRequest();
        AuthResponse expectedResponse = new AuthResponse();
        given(authService.login(request)).willReturn(expectedResponse);

        ResponseEntity<AuthResponse> result = controller.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(expectedResponse);
        then(authService).should().login(request);
    }

    // ─── Olvidé contraseña ───────────────────────────────────
    @Test
    @DisplayName("POST /forgot-password → 200 + MessageResponse del servicio")
    void forgotPasswordShouldReturn200AndServiceMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        MessageResponse serviceResponse = new MessageResponse("Instrucciones enviadas");
        given(authService.forgotPassword(request)).willReturn(serviceResponse);

        ResponseEntity<MessageResponse> result = controller.forgotPassword(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        then(authService).should().forgotPassword(request);
    }

    // ─── Verificar código de reseteo ─────────────────────────
    @Test
    @DisplayName("POST /verify-reset-code → 200 + VerifyResetCodeResponse")
    void verifyResetCodeShouldReturn200AndResponse() {
        VerifyResetCodeRequest request = new VerifyResetCodeRequest();
        VerifyResetCodeResponse expectedResponse = new VerifyResetCodeResponse();
        given(authService.verifyResetCode(request)).willReturn(expectedResponse);

        ResponseEntity<VerifyResetCodeResponse> result = controller.verifyResetCode(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(expectedResponse);
        then(authService).should().verifyResetCode(request);
    }

    // ─── Resetear contraseña ─────────────────────────────────
    @Test
    @DisplayName("POST /reset-password → 200 + AuthResponse")
    void resetPasswordShouldReturn200AndAuthResponse() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        AuthResponse expectedResponse = new AuthResponse();
        given(authService.resetPassword(request)).willReturn(expectedResponse);

        ResponseEntity<AuthResponse> result = controller.resetPassword(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(expectedResponse);
        then(authService).should().resetPassword(request);
    }

    // ─── Refrescar token ─────────────────────────────────────
    @Test
    @DisplayName("POST /refresh → 200 + RefreshResponse")
    void refreshShouldReturn200AndRefreshResponse() {
        RefreshRequest request = new RefreshRequest();
        RefreshResponse expectedResponse = new RefreshResponse();
        given(authService.refresh(request)).willReturn(expectedResponse);

        ResponseEntity<RefreshResponse> result = controller.refresh(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(expectedResponse);
        then(authService).should().refresh(request);
    }

    // ─── Logout ──────────────────────────────────────────────
    @Test
    @DisplayName("POST /logout → 200 + Mensaje de cierre de sesión")
    void logoutShouldReturn200AndMessage() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token-abc");

        ResponseEntity<MessageResponse> result = controller.logout(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMessage()).isEqualTo("Sesión cerrada correctamente");
        then(authService).should().logout("refresh-token-abc");
    }

    // ─── Logout all ──────────────────────────────────────────
    @Test
    @DisplayName("POST /logout-all → 200 + Mensaje y sesiones cerradas")
    void logoutAllShouldReturn200AndClosedSessionsCount() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("123e4567-e89b-12d3-a456-426614174000")
                .password("ignored")
                .authorities("ROLE_USER")
                .build();
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        int closedSessions = 3;
        given(authService.logoutAll(userId)).willReturn(closedSessions);

        ResponseEntity<MessageResponse> result = controller.logoutAll(userDetails);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMessage()).isEqualTo("Se cerraron todas las sesiones activas");
        assertThat(result.getBody().getSessionsClosedCount()).isEqualTo(3);
        then(authService).should().logoutAll(userId);
    }

    // Opcional: si quieres probar que el controlador no hace nada raro con excepciones,
    // puedes verificar que el método delegue sin catch (la excepción se propagaría).
    // Pero eso se cubre mejor con tests de integración (MockMvc) o probando el @ControllerAdvice.
}