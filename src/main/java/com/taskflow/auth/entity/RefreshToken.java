package com.taskflow.auth.entity;

import com.taskflow.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'refresh_tokens'.
 *
 * Cada fila representa una sesión activa del usuario.
 * Un usuario puede tener múltiples refresh tokens (múltiples dispositivos).
 *
 * Decisiones de diseño:
 * - tokenHash: nunca guardar el token real, solo su hash (igual que passwords)
 * - ON DELETE CASCADE en BD: si se borra el User, se borran sus tokens
 * - No tiene @PreUpdate porque un token no se edita, se borra y se crea uno nuevo
 */
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "refresh_tokens_token_hash_unique", columnNames = "token_hash")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // Relación Many-to-One: muchos tokens pueden pertenecer a un usuario
    // Un usuario con 3 dispositivos logueados = 3 refresh tokens
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_tokens_user")
    )
    private User user;

    // Hash del token — el token real solo existe en el cliente (httpOnly cookie)
    // Se hashea igual que las contraseñas: nunca en texto plano en BD
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── Lifecycle hooks ──────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Helpers ──────────────────────────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}