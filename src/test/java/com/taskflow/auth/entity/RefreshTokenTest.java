package com.taskflow.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken unit tests")
class RefreshTokenTest {

    @Test
    @DisplayName("isExpired debe devolver true si la fecha de expiración ya pasó")
    void isExpiredShouldBeTrueWhenExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired debe devolver false si aún no ha expirado")
    void isExpiredShouldBeFalseWhenNotExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertThat(token.isExpired()).isFalse();
    }

    // Opcional: probar que el @PrePersist asigna createdAt (se vería en un test de integración con BD)
    // Aquí no cubrimos JPA, pero para entidades simples no suele ser necesario.
}