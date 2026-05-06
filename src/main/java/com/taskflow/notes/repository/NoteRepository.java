package com.taskflow.notes.repository;

import com.taskflow.notes.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de notas.
 *
 * Usa paginación (Page + Pageable) para no traer todas las notas
 * de golpe — un usuario puede tener cientos de notas.
 *
 * Todos los queries filtran por userId para garantizar aislamiento
 * entre usuarios — nunca exponer notas de otro usuario.
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    // ─── Queries con paginación ───────────────────────────────
    // Page<Note> devuelve las notas + metadata de paginación
    // (totalElements, totalPages, etc.) — igual que el contrato YAML

    // GET /notes — todas las notas del usuario paginadas
    // Pageable recibe page, size y sort desde el Controller
    @Query("""
        SELECT n FROM Note n
        WHERE n.user.id = :userId
        """)
    Page<Note> findByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    // GET /notes/tag/{tagId} — notas de un tag específico paginadas
    @Query("""
        SELECT n FROM Note n
        WHERE n.user.id = :userId
          AND n.tag.id = :tagId
        """)
    Page<Note> findByUserIdAndTagId(
            @Param("userId") UUID userId,
            @Param("tagId") UUID tagId,
            Pageable pageable
    );

    // GET /notes — notas sin tag (tag_id IS NULL)
    @Query("""
        SELECT n FROM Note n
        WHERE n.user.id = :userId
          AND n.tag IS NULL
        """)
    Page<Note> findByUserIdWithoutTag(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    // ─── Queries sin paginación ───────────────────────────────

    // Buscar nota por id verificando que pertenezca al usuario
    // Si la nota es de otro usuario → devuelve empty → 404 (no 403)
    // Nunca revelar que la nota existe si no pertenece al usuario
    @Query("""
        SELECT n FROM Note n
        LEFT JOIN FETCH n.tag
        WHERE n.id = :noteId
          AND n.user.id = :userId
        """)
    Optional<Note> findByIdAndUserId(
            @Param("noteId") UUID noteId,
            @Param("userId") UUID userId
    );

    // Verificar pertenencia sin traer la nota completa — más eficiente
    boolean existsByIdAndUserId(UUID id, UUID userId);

    // Contar notas totales del usuario — para estadísticas
    long countByUserId(UUID userId);

    // Contar notas completadas del usuario
    @Query("""
        SELECT COUNT(n) FROM Note n
        WHERE n.user.id = :userId
          AND n.completed = true
        """)
    long countCompletedByUserId(@Param("userId") UUID userId);

    // ─── Desvinculación de tag ────────────────────────────────
    // Cuando se borra un tag, desvincular todas sus notas
    // Normalmente lo hace el SP, pero este método es útil para tests

    @Modifying
    @Query("""
        UPDATE Note n
        SET n.tag = NULL
        WHERE n.tag.id = :tagId
          AND n.user.id = :userId
        """)
    void detachTagFromNotes(
            @Param("tagId") UUID tagId,
            @Param("userId") UUID userId
    );

    // ─── Queries nativas — SP de resumen ─────────────────────
    // Llama a la función sp_get_notes_summary para el dashboard
    // Se mapea a NotesSummaryDTO en el Service
    @Query(value = """
        SELECT * FROM sp_get_notes_summary(:userId)
        """, nativeQuery = true)
    java.util.List<Object[]> getNotesSummary(@Param("userId") UUID userId);
}