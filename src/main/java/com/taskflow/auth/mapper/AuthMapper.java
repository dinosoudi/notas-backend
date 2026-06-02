package com.taskflow.auth.mapper;

import com.taskflow.auth.dto.*;
import com.taskflow.shared.dto.TokensDTO;
import com.taskflow.shared.dto.UserDTO;
import com.taskflow.users.entity.User;
import org.mapstruct.*;

/**
 * Mapper de MapStruct para el módulo de autenticación.
 *
 * Construye los responses del AuthController combinando
 * datos del User entity con los tokens generados por JwtService.
 *
 * Los métodos que necesitan múltiples fuentes usan @Mapping(source = "param.field")
 * para indicar de qué parámetro viene cada campo.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuthMapper {

    // ─── User → UserDTO ──────────────────────────────────────
    // MapStruct mapea automáticamente todos los campos con el mismo nombre
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "authProvider", source = "authProvider")
    @Mapping(target = "createdAt", source = "createdAt")
    UserDTO toUserDTO(User user);

    // ─── AuthResponse — login / verify-email / reset-password ─
    // Combina el UserDTO ya construido con los tokens
    @Mapping(target = "message", source = "message")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "tokens", source = "tokens")
    AuthResponse toAuthResponse(String message, UserDTO user, TokensDTO tokens);

    // ─── AuthResponse para register ──────────────────────────
    // tokens es null — el usuario aún no ha verificado su email
    @Mapping(target = "message", source = "message")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "tokens", ignore = true)
    AuthResponse toRegisterResponse(String message, UserDTO user);

    // ─── TokensDTO ────────────────────────────────────────────
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "accessTokenExpiresIn", source = "accessTokenExpiresIn")
    @Mapping(target = "refreshTokenExpiresIn", source = "refreshTokenExpiresIn")
    TokensDTO toTokensDTO(
            String accessToken,
            String refreshToken,
            Long accessTokenExpiresIn,
            Long refreshTokenExpiresIn
    );

    // ─── VerifyResetCodeResponse ──────────────────────────────
    @Mapping(target = "message", source = "message")
    @Mapping(target = "resetToken", source = "resetToken")
    @Mapping(target = "expiresInMinutes", source = "expiresInMinutes")
    VerifyResetCodeResponse toVerifyResetCodeResponse(
            String message,
            String resetToken,
            Integer expiresInMinutes
    );

    // ─── RefreshResponse ──────────────────────────────────────
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "accessTokenExpiresIn", source = "accessTokenExpiresIn")
    @Mapping(target = "refreshTokenExpiresIn", source = "refreshTokenExpiresIn")
    RefreshResponse toRefreshResponse(
            String accessToken,
            String refreshToken,
            Long accessTokenExpiresIn,
            Long refreshTokenExpiresIn
    );
}