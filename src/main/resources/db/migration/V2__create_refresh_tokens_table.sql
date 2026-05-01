-- ─────────────────────────────────────────────
-- V2 — Tabla de refresh tokens
-- ─────────────────────────────────────────────
-- Cada fila = una sesión activa del usuario
-- Permite logout de dispositivo específico o de todos

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,   -- hash del token, nunca en texto plano
    expires_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,              -- si se borra el usuario, se borran sus tokens

    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);

-- Índice para buscar tokens por usuario (logout-all)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Índice para limpiar tokens expirados con un job periódico
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

COMMENT ON TABLE refresh_tokens IS 'Sesiones activas. Una fila por dispositivo logueado. Borra al hacer logout.';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Hash del refreshToken. El token real solo existe en el cliente.';
