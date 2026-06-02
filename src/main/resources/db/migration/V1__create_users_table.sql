-- ─────────────────────────────────────────────
-- V1 — Tabla de usuarios
-- ─────────────────────────────────────────────
-- Reglas:
--   - UUID como PK (más seguro que autoincrement en APIs públicas)
--   - password_hash NULL para cuentas OAuth (Google en v2)
--   - auth_provider preparado para v2 (GOOGLE)
--   - phone preparado para v2
--   - preferences como JSONB — flexible para agregar campos sin migraciones
--   - soft delete con deleted_at y deletion_scheduled_at

CREATE TABLE users (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NULL,        -- NULL si es cuenta OAuth
    phone                   VARCHAR(20)     NULL,        -- v2: autenticación por SMS
    auth_provider           VARCHAR(20)     NOT NULL DEFAULT 'EMAIL',  -- EMAIL | GOOGLE
    email_verified          BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Recuperación de contraseña
    reset_code              VARCHAR(255)    NULL,        -- hash del código de 6 dígitos
    reset_code_expires      TIMESTAMP       NULL,
    reset_attempts          INT             NOT NULL DEFAULT 0,
    -- Verificación de email
    verification_token      VARCHAR(255)    NULL,
    verification_expires    TIMESTAMP       NULL,
    -- Preferencias de UI (JSONB para evitar columnas extras por cada preferencia)
    preferences             JSONB           NOT NULL DEFAULT '{"darkMode": false, "language": "es"}',
    -- Soft delete — 30 días de gracia antes de borrado definitivo
    deleted_at              TIMESTAMP       NULL,
    deletion_scheduled_at   TIMESTAMP       NULL,
    -- Auditoría
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_auth_provider_check CHECK (auth_provider IN ('EMAIL', 'GOOGLE'))
);

-- Índices
-- El más importante: búsqueda por email en login
CREATE INDEX idx_users_email ON users (email);

-- Para soft delete: filtrar usuarios activos rápidamente
CREATE INDEX idx_users_deleted_at ON users (deleted_at) WHERE deleted_at IS NULL;

-- Para limpiar cuentas programadas para eliminación (job en AWS)
CREATE INDEX idx_users_deletion_scheduled ON users (deletion_scheduled_at) WHERE deletion_scheduled_at IS NOT NULL;

COMMENT ON TABLE users IS 'Usuarios del sistema. Soft delete con período de gracia de 30 días.';
COMMENT ON COLUMN users.password_hash IS 'NULL para cuentas OAuth. Hasheado con bcrypt cost 12.';
COMMENT ON COLUMN users.auth_provider IS 'EMAIL = registro normal. GOOGLE = OAuth2 (v2).';
COMMENT ON COLUMN users.preferences IS 'Preferencias de UI. JSONB permite agregar campos sin migraciones.';
