package com.taskflow.users.entity;

import com.taskflow.auth.entity.RefreshToken;
import com.taskflow.notes.entity.Note;
import com.taskflow.tags.entity.Tag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'users'.
 *
 * Decisiones de diseño:
 * - UUID como PK — más seguro que autoincrement en APIs públicas
 * - JSONB para preferences — evita columnas extra por cada preferencia
 * - Soft delete con deletedAt y deletionScheduledAt
 * - passwordHash NULL para cuentas OAuth (Google en v2)
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "users_email_unique", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    // NULL para cuentas OAuth — nunca exponer este campo en responses
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // v2: autenticación por SMS
    @Column(length = 20)
    private String phone;

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.EMAIL;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    // ─── Recuperación de contraseña ───────────────────────────
    // Hash del código de 6 dígitos — nunca guardar en texto plano
    @Column(name = "reset_code", length = 255)
    private String resetCode;

    @Column(name = "reset_code_expires")
    private LocalDateTime resetCodeExpires;

    @Column(name = "reset_attempts", nullable = false)
    @Builder.Default
    private Integer resetAttempts = 0;

    // ─── Verificación de email ────────────────────────────────
    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Column(name = "verification_expires")
    private LocalDateTime verificationExpires;

    // ─── Reset token (paso 2→3 del flujo de recuperación) ────
    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expires")
    private LocalDateTime resetTokenExpires;

    // ─── Preferencias de UI ───────────────────────────────────
    // JSONB en PostgreSQL — flexible para agregar campos sin migraciones
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private UserPreferences preferences = new UserPreferences();

    // ─── Soft delete ──────────────────────────────────────────
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deletion_scheduled_at")
    private LocalDateTime deletionScheduledAt;

    // ─── Auditoría ────────────────────────────────────────────
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── Relaciones ───────────────────────────────────────────
    // mappedBy indica que la FK está en la otra tabla
    // CascadeType.ALL + orphanRemoval = si se borra el User, se borran sus tags y notas
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Note> notes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    // ─── Lifecycle hooks ──────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Helpers ──────────────────────────────────────────────
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isActive() {
        return deletedAt == null && Boolean.TRUE.equals(emailVerified);
    }

    // ─── Enum para auth provider ──────────────────────────────
    public enum AuthProvider {
        EMAIL,
        GOOGLE   // v2
    }

    // ─── Clase embebida para preferences JSONB ────────────────
    // Lombok genera getters/setters automáticamente
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPreferences {
        @Builder.Default
        private Boolean darkMode = false;
        @Builder.Default
        private String language = "es";
    }
}