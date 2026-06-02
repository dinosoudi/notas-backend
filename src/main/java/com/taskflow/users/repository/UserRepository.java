package com.taskflow.users.repository;

import com.taskflow.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de usuarios.
 *
 * Spring Data JPA genera la implementación automáticamente.
 * Los métodos que siguen la convención de nombres (findBy, existsBy, deleteBy)
 * no necesitan @Query — Spring los traduce a SQL solo.
 *
 * Los métodos más complejos usan JPQL (@Query) o SQL nativo (@Query nativeQuery=true).
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ─── Queries por convención de nombres ────────────────────
    // Spring traduce estos a SQL automáticamente — no necesitan @Query

    // Login — buscar usuario activo por email
    Optional<User> findByEmail(String email);

    // Verificar si existe un email antes de registrar (para el 409 del contrato)
    boolean existsByEmail(String email);

    // Buscar por token de verificación de email
    Optional<User> findByVerificationToken(String verificationToken);

    // Buscar por token de reset de contraseña
    Optional<User> findByResetCode(String resetCode);

    Optional<User> findByResetToken(String resetToken);

    // ─── Queries con JPQL ─────────────────────────────────────
    // JPQL usa nombres de entidades y campos Java, no tablas y columnas SQL

    // Buscar usuario activo (no eliminado) por email
    // Útil en login para rechazar cuentas en proceso de eliminación
    @Query("""
        SELECT u FROM User u
        WHERE u.email = :email
          AND u.deletedAt IS NULL
        """)
    Optional<User> findActiveByEmail(@Param("email") String email);

    // Buscar usuario por email sin importar si está eliminado
    // Útil para el flujo de cancelación de eliminación (login especial)
    @Query("""
        SELECT u FROM User u
        WHERE u.email = :email
        """)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    // ─── Queries nativas (SQL puro) ───────────────────────────
    // Se usan cuando JPQL no tiene la expresividad necesaria
    // o cuando necesitas funciones específicas de PostgreSQL

    // Actualizar email_verified y limpiar el token de verificación
    // @Modifying indica que es un UPDATE/DELETE, no un SELECT
    @Modifying
    @Query(value = """
        UPDATE users
        SET email_verified = true,
            verification_token = NULL,
            verification_expires = NULL,
            updated_at = NOW()
        WHERE id = :userId
        """, nativeQuery = true)
    void verifyEmail(@Param("userId") UUID userId);

    // Resetear intentos de código y limpiar datos de reset
    @Modifying
    @Query(value = """
        UPDATE users
        SET reset_code = NULL,
            reset_code_expires = NULL,
            reset_attempts = 0,
            updated_at = NOW()
        WHERE id = :userId
        """, nativeQuery = true)
    void clearResetCode(@Param("userId") UUID userId);

    // Soft delete — marcar cuenta para eliminación con fecha límite
    @Modifying
    @Query(value = """
        UPDATE users
        SET deleted_at = :deletedAt,
            deletion_scheduled_at = :scheduledAt,
            updated_at = NOW()
        WHERE id = :userId
        """, nativeQuery = true)
    void softDelete(
            @Param("userId") UUID userId,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("scheduledAt") LocalDateTime scheduledAt
    );

    // Cancelar eliminación — limpiar las fechas de soft delete
    @Modifying
    @Query(value = """
        UPDATE users
        SET deleted_at = NULL,
            deletion_scheduled_at = NULL,
            updated_at = NOW()
        WHERE id = :userId
        """, nativeQuery = true)
    void cancelDeletion(@Param("userId") UUID userId);

    // Para el job de limpieza en AWS Lambda — encontrar cuentas listas para borrar
    @Query("""
        SELECT u FROM User u
        WHERE u.deletionScheduledAt IS NOT NULL
          AND u.deletionScheduledAt <= :now
        """)
    java.util.List<User> findUsersReadyForDeletion(@Param("now") LocalDateTime now);
}