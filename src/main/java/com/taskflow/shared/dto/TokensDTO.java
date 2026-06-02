package com.taskflow.shared.dto;

import lombok.*;

/**
 * DTO con los tokens JWT devueltos tras login, register o refresh.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokensDTO {

    private String accessToken;
    private String refreshToken;

    // Segundos hasta que expira el accessToken — siempre 86400 (24h)
    private Long accessTokenExpiresIn;

    // Segundos hasta que expira el refreshToken
    // Web (v1): 604800 (7 días) — Móvil (v2): 2592000 (30 días)
    private Long refreshTokenExpiresIn;
}
