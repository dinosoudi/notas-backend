-- V7 — Agregar columnas reset_token y reset_token_expires a users
ALTER TABLE users
    ADD COLUMN reset_token          VARCHAR(255) NULL,
    ADD COLUMN reset_token_expires  TIMESTAMP    NULL;

COMMENT ON COLUMN users.reset_token IS 'Token temporal para el paso 2→3 del flujo de recuperación. Expira en 15 min.';
COMMENT ON COLUMN users.reset_token_expires IS 'Fecha de expiración del reset_token.';