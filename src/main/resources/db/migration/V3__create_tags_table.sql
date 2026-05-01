-- ─────────────────────────────────────────────
-- V3 — Tabla de tags
-- ─────────────────────────────────────────────
-- Tags va antes que notes porque notes tiene FK a tags

CREATE TABLE tags (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    name        VARCHAR(50) NOT NULL,
    color       VARCHAR(7)  NOT NULL DEFAULT '#4ECDC4',  -- hex #RRGGBB
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tags_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    -- Un usuario no puede tener dos tags con el mismo nombre
    CONSTRAINT tags_user_name_unique UNIQUE (user_id, name),

    CONSTRAINT tags_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Índice principal: traer todos los tags de un usuario
CREATE INDEX idx_tags_user_id ON tags (user_id);

COMMENT ON TABLE tags IS 'Tags personalizados por usuario. Máximo 1 tag por nota.';
COMMENT ON COLUMN tags.color IS 'Color en formato hexadecimal #RRGGBB. Validado por constraint.';
