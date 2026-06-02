package com.taskflow.tags.repository;

import com.taskflow.tags.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de tags.
 *
 * Todos los queries filtran por userId — un usuario nunca
 * debe ver ni modificar tags de otro usuario.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    // ─── Queries por convención de nombres ────────────────────

    // Traer todos los tags de un usuario — para GET /tags
    List<Tag> findByUserIdOrderByCreatedAtAsc(UUID userId);

    // Verificar si existe un tag con ese nombre para ese usuario — para el 409
    boolean existsByUserIdAndName(UUID userId, String name);

    // Verificar si existe un tag con ese nombre para ese usuario, excluyendo el propio
    // Usado en PUT /tags/{id} para no conflictuar con el tag que se está editando
    boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID id);

    // ─── Queries con JPQL ─────────────────────────────────────

    // Buscar tag por id verificando que pertenezca al usuario
    // Si el tag existe pero es de otro usuario, devuelve empty (→ 404, no 403)
    @Query("""
        SELECT t FROM Tag t
        WHERE t.id = :tagId
          AND t.user.id = :userId
        """)
    Optional<Tag> findByIdAndUserId(
            @Param("tagId") UUID tagId,
            @Param("userId") UUID userId
    );

    // Traer tags con conteo de notas en una sola query — evita N+1
    // Sin esto, por cada tag harías una query para contar sus notas
    @Query("""
        SELECT t, COUNT(n.id) AS noteCount
        FROM Tag t
        LEFT JOIN t.notes n
        WHERE t.user.id = :userId
        GROUP BY t.id
        ORDER BY t.createdAt ASC
        """)
    List<Object[]> findAllWithNoteCount(@Param("userId") UUID userId);

    // Verificar cuántas notas tiene un tag antes de borrarlo (para el 409 del contrato)
    @Query("""
        SELECT COUNT(n.id)
        FROM Tag t
        JOIN t.notes n
        WHERE t.id = :tagId
          AND t.user.id = :userId
        """)
    long countNotesByTagId(
            @Param("tagId") UUID tagId,
            @Param("userId") UUID userId
    );

    //Eliminar tags y desvincular notas:
    @Modifying
    @Query("""
    UPDATE Note n SET n.tag = NULL
    WHERE n.tag.id = :tagId
    """)
    void detachNotesByTagId(@Param("tagId") UUID tagId);

    @Modifying
    @Query("""
    DELETE FROM Tag t
    WHERE t.id = :tagId AND t.user.id = :userId
    """)
    void deleteByIdAndUserId(@Param("tagId") UUID tagId, @Param("userId") UUID userId);


}