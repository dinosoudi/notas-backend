package com.taskflow.tags.entity;

import com.taskflow.users.entity.User;
import com.taskflow.notes.entity.Note;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'tags'.
 *
 * Los tags son personalizados por usuario — un tag de un usuario
 * no es visible ni accesible por otros usuarios.
 *
 * Decisiones de diseño:
 * - Unique constraint en (user_id, name) — mismo nombre de tag no se repite por usuario
 * - Color en formato hex #RRGGBB validado en BD y en el DTO
 * - orphanRemoval = false en notes — si se borra el tag, las notas NO se borran,
 *   solo quedan sin tag (SET NULL en BD). Diferente al cascade de User→Note.
 */
@Entity
@Table(
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "tags_user_name_unique",
                        columnNames = {"user_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // Relación Many-to-One: muchos tags pueden pertenecer a un usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tags_user")
    )
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    // Color hex #RRGGBB — validado también en TagRequest DTO con @Pattern
    @Column(nullable = false, length = 7)
    @Builder.Default
    private String color = "#4ECDC4";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── Relación con notas ───────────────────────────────────
    // mappedBy = "tag" porque en Note.java el campo se llama 'tag'
    // orphanRemoval = false — si se borra el tag, las notas quedan sin tag (no se borran)
    // El borrado en cascada lo maneja el SP sp_delete_tag_cascade
    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Note> notes = new ArrayList<>();

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

    // Útil para el conteo en TagResponse sin hacer query extra
    public int getNoteCount() {
        return notes != null ? notes.size() : 0;
    }
}