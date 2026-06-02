package com.taskflow.auth.dto;

import com.taskflow.shared.dto.TokensDTO;
import com.taskflow.shared.dto.UserDTO;
import lombok.*;

/**
 * Response para /auth/login, /auth/verify-email y /auth/reset-password.
 * tokens es null en el response de /auth/register (usuario aún no verificado).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String message;
    private UserDTO user;

    // Null en /auth/register — el usuario debe verificar su email primero
    private TokensDTO tokens;
}
