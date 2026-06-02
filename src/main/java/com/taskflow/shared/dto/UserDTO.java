package com.taskflow.shared.dto;

import com.taskflow.users.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de usuario para incluir en responses.
 * Nunca incluir passwordHash, resetCode ni tokens internos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Boolean emailVerified;
    private User.AuthProvider authProvider;
    private LocalDateTime createdAt;
}
