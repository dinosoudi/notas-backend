package com.taskflow.auth.repository;

import com.taskflow.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de refresh tokens.
 *
 * Maneja las sesiones activas del usuario.
 * Los métodos más usados son buscar por hash y borrar por usuario.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // ─── Queries por convención de nombres ────────────────────

    // Buscar sesión por hash del token — usado en /auth/refresh y /auth/logout
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Verificar si existe un token (para detectar reutilización — posible robo)
    boolean existsByTokenHash(String tokenHash);

    // Contar sesiones activas de un usuario — útil para logout-all response
    long countByUserId(UUID userId);

    // ─── Queries con JPQL ─────────────────────────────────────

    // Logout de este dispositivo — borrar el token específico por hash
    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt
        WHERE rt.tokenHash = :tokenHash
        """)
    void deleteByTokenHash(@Param("tokenHash") String tokenHash);

    // Logout de todos los dispositivos — borrar todos los tokens del usuario
    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt
        WHERE rt.user.id = :userId
        """)
    void deleteAllByUserId(@Param("userId") UUID userId);

    // Logout de todos los dispositivos EXCEPTO el actual
    // Usado en changePassword con closeOtherSessions = true
    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt
        WHERE rt.user.id = :userId
          AND rt.tokenHash != :currentTokenHash
        """)
    void deleteAllByUserIdExcept(
            @Param("userId") UUID userId,
            @Param("currentTokenHash") String currentTokenHash
    );

    // ─── Limpieza de tokens expirados ─────────────────────────
    // Llamado por un job periódico en AWS para mantener la tabla limpia

    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt
        WHERE rt.expiresAt < :now
        """)
    void deleteAllExpired(@Param("now") LocalDateTime now);

    // Buscar token válido — no expirado — por hash
    // Más seguro que findByTokenHash porque valida expiración en BD
    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.tokenHash = :tokenHash
          AND rt.expiresAt > :now
        """)
    Optional<RefreshToken> findValidToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );
}