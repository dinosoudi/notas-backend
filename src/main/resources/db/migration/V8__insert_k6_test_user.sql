-- ─────────────────────────────────────────────
-- V8 — Usuario de prueba para k6 (pruebas de carga)
-- Password: K6TestPass123!
-- email_verified = true para login directo sin verificar correo
-- ON CONFLICT DO NOTHING — seguro para correr múltiples veces
-- ─────────────────────────────────────────────
INSERT INTO users (
    id,
    name,
    email,
    password_hash,
    email_verified,
    auth_provider,
    created_at,
    updated_at
)
VALUES (
           'a0000000-0000-0000-0000-000000000001',
           'k6 Test User',
           'k6test@taskflow.com',
           '$2a$12$4auqFwok.nw8kmEHVxD/qeN.nKIPt9TIQivSpgoz5pA2ylf0ki2nK',
           true,
           'EMAIL',
           NOW(),
           NOW()
       )
    ON CONFLICT (email) DO NOTHING;