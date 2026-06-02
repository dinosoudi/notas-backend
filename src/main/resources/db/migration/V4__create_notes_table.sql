-- ─────────────────────────────────────────────
-- V4 — Tabla de notas
-- ─────────────────────────────────────────────

CREATE TABLE notes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    tag_id      UUID        NULL,           -- opcional, máximo 1 tag por nota
    content     TEXT        NOT NULL,
    completed   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notes_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notes_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags (id)
        ON DELETE SET NULL,     -- si se borra el tag, la nota queda sin tag (no se borra)

    CONSTRAINT notes_content_not_empty CHECK (LENGTH(TRIM(content)) > 0)
);

-- Índice principal: traer todas las notas de un usuario paginadas
CREATE INDEX idx_notes_user_id ON notes (user_id);

-- Índice para filtrar por tag
CREATE INDEX idx_notes_tag_id ON notes (tag_id) WHERE tag_id IS NOT NULL;

-- Índice para ordenar por fecha de actualización (orden por defecto)
CREATE INDEX idx_notes_updated_at ON notes (user_id, updated_at DESC);

COMMENT ON TABLE notes IS 'Notas del usuario. Sin título, solo contenido. Máximo 1 tag.';
COMMENT ON COLUMN notes.tag_id IS 'NULL si la nota no tiene tag. SET NULL al borrar el tag.';
COMMENT ON COLUMN notes.completed IS 'Estado de completado — equivalente a tachar una nota.';
