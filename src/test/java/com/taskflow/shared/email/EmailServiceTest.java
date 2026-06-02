package com.taskflow.shared.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("EmailService (stub)")
class EmailServiceTest {

    private final EmailService emailService = new EmailService();

    @Test
    @DisplayName("sendVerificationEmail no debe lanzar excepción")
    void sendVerificationEmailShouldNotThrow() {
        assertThatCode(() -> emailService.sendVerificationEmail("test@example.com", "Juan", "abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendResetCodeEmail no debe lanzar excepción")
    void sendResetCodeEmailShouldNotThrow() {
        assertThatCode(() -> emailService.sendResetCodeEmail("test@example.com", "Juan", "123456"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendGoogleAccountEmail no debe lanzar excepción")
    void sendGoogleAccountEmailShouldNotThrow() {
        assertThatCode(() -> emailService.sendGoogleAccountEmail("test@example.com", "Juan"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendDeletionScheduledEmail no debe lanzar excepción")
    void sendDeletionScheduledEmailShouldNotThrow() {
        assertThatCode(() -> emailService.sendDeletionScheduledEmail("test@example.com", "Juan", "01/01/2025"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("resendVerificationEmail no debe lanzar excepción")
    void resendVerificationEmailShouldNotThrow() {
        assertThatCode(() -> emailService.resendVerificationEmail("test@example.com", "Juan", "abc123"))
                .doesNotThrowAnyException();
    }
}