package com.taskflow.shared.security;

import com.taskflow.shared.config.JwtConfig;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtService")
class JwtServiceTest {

    // Clave de al menos 256 bits para HS256 (64 caracteres)
    private static final String SECRET = "my-secret-key-very-long-at-least-64-characters-1234567890abcdef";
    private static final Long ACCESS_EXPIRATION_MS = 86_400_000L; // 24h
    private static final Long REFRESH_WEB_EXPIRATION_MS = 604_800_000L; // 7d

    @Mock
    private JwtConfig jwtConfig;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(SECRET);
        when(jwtConfig.getAccessExpirationMs()).thenReturn(ACCESS_EXPIRATION_MS);
        when(jwtConfig.getAccessExpiresInSeconds()).thenReturn(ACCESS_EXPIRATION_MS / 1000);
        when(jwtConfig.getRefreshWebExpiresInSeconds()).thenReturn(REFRESH_WEB_EXPIRATION_MS / 1000);

        jwtService = new JwtService(jwtConfig);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("Debe generar un token JWT con el userId como subject")
        void shouldGenerateTokenWithUserId() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateAccessToken(userId);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // header.payload.signature

            UUID extracted = jwtService.extractUserId(token);
            assertThat(extracted).isEqualTo(userId);
        }

        @Test
        @DisplayName("El token debe tener una fecha de expiración futura")
        void tokenShouldHaveFutureExpiration() {
            String token = jwtService.generateAccessToken(UUID.randomUUID());
            Date expiration = jwtService.extractExpiration(token);
            assertThat(expiration).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("Debe retornar true para un token válido")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID());
            assertThat(jwtService.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false para token nulo")
        void shouldReturnFalseForNullToken() {
            assertThat(jwtService.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para token vacío")
        void shouldReturnFalseForEmptyToken() {
            assertThat(jwtService.isValid("")).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para token modificado")
        void shouldReturnFalseForTamperedToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID());
            String tampered = token.substring(0, token.length() - 1) + "X";
            assertThat(jwtService.isValid(tampered)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("Debe extraer el userId correcto del token")
        void shouldExtractUserId() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateAccessToken(userId);
            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        }

        @Test
        @DisplayName("Debe lanzar excepción con token inválido")
        void shouldThrowExceptionForInvalidToken() {
            assertThatThrownBy(() -> jwtService.extractUserId("invalid.token.here"))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("extractExpiration")
    class ExtractExpiration {

        @Test
        @DisplayName("Debe retornar la fecha de expiración del token")
        void shouldReturnExpirationDate() {
            String token = jwtService.generateAccessToken(UUID.randomUUID());
            Date exp = jwtService.extractExpiration(token);
            // Debe estar cerca del momento actual + 24h
            long now = System.currentTimeMillis();
            assertThat(exp.getTime()).isBetween(now, now + ACCESS_EXPIRATION_MS + 1000);
        }
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("Debe retornar false para token recién generado")
        void shouldReturnFalseForFreshToken() {
            String token = jwtService.generateAccessToken(UUID.randomUUID());
            assertThat(jwtService.isExpired(token)).isFalse();
        }

        @Test
        @DisplayName("Debe retornar true para token expirado")
        void shouldReturnTrueForExpiredToken() {
            // Forzamos una expiración negativa para generar un token ya vencido
            when(jwtConfig.getAccessExpirationMs()).thenReturn(-1000L);
            JwtService expiredService = new JwtService(jwtConfig);
            String token = expiredService.generateAccessToken(UUID.randomUUID());
            assertThat(expiredService.isExpired(token)).isTrue();
        }

        @Test
        @DisplayName("Debe retornar true para token inválido (considerado expirado)")
        void shouldReturnTrueForInvalidToken() {
            assertThat(jwtService.isExpired("invalid")).isTrue();
        }
    }

    @Nested
    @DisplayName("Helpers de tiempo")
    class TimeHelpers {

        @Test
        @DisplayName("getAccessTokenExpiresIn debe delegar en JwtConfig")
        void getAccessTokenExpiresInShouldDelegate() {
            assertThat(jwtService.getAccessTokenExpiresIn()).isEqualTo(ACCESS_EXPIRATION_MS / 1000);
        }

        @Test
        @DisplayName("getRefreshWebExpiresIn debe delegar en JwtConfig")
        void getRefreshWebExpiresInShouldDelegate() {
            assertThat(jwtService.getRefreshWebExpiresIn()).isEqualTo(REFRESH_WEB_EXPIRATION_MS / 1000);
        }
    }
}