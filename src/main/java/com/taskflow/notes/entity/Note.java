package com.taskflow.notes.entity;

import com.taskflow.tags.entity.Tag;
import com.taskflow.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'notes'.
 *
 * Decisiones de diseño:
 * - Sin título — solo contenido (decisión de producto desde el contrato)
 * - tag_id nullable — una nota puede existir sin tag
 * - ON DELETE SET NULL en BD para tag_id — si se borra el tag, la nota queda sin tag
 * - ON DELETE CASCADE en BD para user_id — si se borra el usuario, se borran sus notas
 * - completed como boolean simple — no hay estados intermedios
 */
@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // Relación Many-to-One con User
    // LAZY — no cargar el User completo cada vez que traes una nota
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notes_user")
    )
    private User user;

    // Relación Many-to-One con Tag — nullable porque el tag es opcional
    // LAZY — no cargar el Tag completo en cada nota
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tag_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "fk_notes_tag")
    )
    private Tag tag;

    // TEXT en PostgreSQL — sin límite de caracteres (hasta 5000 en el contrato)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── Lifecycle hooks ──────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Helpers ──────────────────────────────────────────────
    public boolean hasTag() {
        return tag != null;
    }

    public boolean isPending() {
        return !Boolean.TRUE.equals(completed);
    }

    // Asigna el tag y mantiene la relación bidireccional consistente
    public void assignTag(Tag newTag) {
        // Si tenía un tag anterior, remover esta nota de su lista
        if (this.tag != null && this.tag.getNotes() != null) {
            this.tag.getNotes().remove(this);
        }
        this.tag = newTag;
        // Agregar esta nota a la lista del nuevo tag
        if (newTag != null && newTag.getNotes() != null) {
            newTag.getNotes().add(this);
        }
    }
}