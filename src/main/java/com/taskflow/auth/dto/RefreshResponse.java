package com.taskflow.auth.dto;

import lombok.*;

/**
 * Response para POST /auth/refresh
 * Devuelve los nuevos tokens tras un refresh exitoso.
 * El token anterior queda invalidado (refresh token rotation).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshResponse {

    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;
    private Long refreshTokenExpiresIn;
}
