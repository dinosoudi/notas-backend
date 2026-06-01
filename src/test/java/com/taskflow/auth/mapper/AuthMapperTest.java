package com.taskflow.auth.mapper;

import com.taskflow.auth.dto.*;
import com.taskflow.shared.dto.TokensDTO;
import com.taskflow.shared.dto.UserDTO;
import com.taskflow.users.entity.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthMapper unit tests")
class AuthMapperTest {

    private static AuthMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = Mappers.getMapper(AuthMapper.class);
    }

    // ─── User a UserDTO ──────────────────────────────────────
    @Test
    @DisplayName("toUserDTO debe mapear todos los campos")
    void toUserDTOShouldMapAllFields() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);
        user.setName("María");
        user.setEmail("maria@example.com");
        user.setPhone("+56912345678");
        user.setEmailVerified(true);
        user.setAuthProvider(User.AuthProvider.EMAIL);  // o el valor que corresponda
        user.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));

        UserDTO dto = mapper.toUserDTO(user);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo("María");
        assertThat(dto.getEmail()).isEqualTo("maria@example.com");
        assertThat(dto.getPhone()).isEqualTo("+56912345678");
        assertThat(dto.getEmailVerified()).isTrue();
        assertThat(dto.getAuthProvider()).isEqualTo(User.AuthProvider.EMAIL);
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
    }

    // ─── AuthResponse con tokens ─────────────────────────────
    @Test
    @DisplayName("toAuthResponse debe combinar mensaje, usuario y tokens")
    void toAuthResponseShouldCombineAll() {
        UserDTO user = new UserDTO();
        user.setName("Juan");
        TokensDTO tokens = new TokensDTO("access", "refresh", 900L, 604800L);

        AuthResponse response = mapper.toAuthResponse("Login exitoso", user, tokens);

        assertThat(response.getMessage()).isEqualTo("Login exitoso");
        assertThat(response.getUser()).isSameAs(user);
        assertThat(response.getTokens()).isSameAs(tokens);
    }

    // ─── AuthResponse para registro (sin tokens) ─────────────
    @Test
    @DisplayName("toRegisterResponse debe ignorar tokens")
    void toRegisterResponseShouldIgnoreTokens() {
        UserDTO user = new UserDTO();
        user.setEmail("new@example.com");

        AuthResponse response = mapper.toRegisterResponse("Registro exitoso", user);

        assertThat(response.getMessage()).isEqualTo("Registro exitoso");
        assertThat(response.getUser()).isSameAs(user);
        assertThat(response.getTokens()).isNull();
    }

    // ─── TokensDTO ────────────────────────────────────────────
    @Test
    @DisplayName("toTokensDTO debe mapear los cuatro parámetros")
    void toTokensDTOShouldMapAll() {
        TokensDTO dto = mapper.toTokensDTO("access123", "refresh456", 1800L, 86400L);

        assertThat(dto.getAccessToken()).isEqualTo("access123");
        assertThat(dto.getRefreshToken()).isEqualTo("refresh456");
        assertThat(dto.getAccessTokenExpiresIn()).isEqualTo(1800L);
        assertThat(dto.getRefreshTokenExpiresIn()).isEqualTo(86400L);
    }

    // ─── VerifyResetCodeResponse ──────────────────────────────
    @Test
    @DisplayName("toVerifyResetCodeResponse debe mapear mensaje, token y expiración")
    void toVerifyResetCodeResponseShouldMapAll() {
        VerifyResetCodeResponse response = mapper.toVerifyResetCodeResponse(
                "Código válido", "reset-token-789", 10
        );

        assertThat(response.getMessage()).isEqualTo("Código válido");
        assertThat(response.getResetToken()).isEqualTo("reset-token-789");
        assertThat(response.getExpiresInMinutes()).isEqualTo(10);
    }

    // ─── RefreshResponse ──────────────────────────────────────
    @Test
    @DisplayName("toRefreshResponse debe mapear tokens de refresco")
    void toRefreshResponseShouldMapAll() {
        RefreshResponse response = mapper.toRefreshResponse("acc", "ref", 1800L, 86400L);

        assertThat(response.getAccessToken()).isEqualTo("acc");
        assertThat(response.getRefreshToken()).isEqualTo("ref");
        assertThat(response.getAccessTokenExpiresIn()).isEqualTo(1800L);
        assertThat(response.getRefreshTokenExpiresIn()).isEqualTo(86400L);
    }
}