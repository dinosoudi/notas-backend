-- ─────────────────────────────────────────────
-- V6 — Stored Procedure: resumen de notas por usuario
-- ─────────────────────────────────────────────
-- Devuelve estadísticas del usuario para el dashboard:
--   - Total de notas
--   - Notas completadas
--   - Notas pendientes
--   - Notas por tag (con conteo)
--
-- Es un FUNCTION (no PROCEDURE) porque retorna un resultado
-- que se puede usar en un SELECT
--
-- Diferencia clave para entrevistas:
--   PROCEDURE: ejecuta acciones, no retorna valor directamente
--   FUNCTION:  retorna un valor o tabla, usable en SELECT
--
-- Cómo llamarlo desde Java:
--   SELECT * FROM sp_get_notes_summary(:userId)

CREATE OR REPLACE FUNCTION sp_get_notes_summary(
    p_user_id UUID
)
RETURNS TABLE (
    total_notes         BIGINT,
    completed_notes     BIGINT,
    pending_notes       BIGINT,
    notes_without_tag   BIGINT,
    tag_id              UUID,
    tag_name            VARCHAR(50),
    tag_color           VARCHAR(7),
    notes_in_tag        BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    -- Parte 1: totales generales
    SELECT
        -- Total de notas del usuario
        (SELECT COUNT(*) FROM notes n WHERE n.user_id = p_user_id)     AS total_notes,
        -- Notas completadas
        (SELECT COUNT(*) FROM notes n WHERE n.user_id = p_user_id
            AND n.completed = TRUE)                                     AS completed_notes,
        -- Notas pendientes
        (SELECT COUNT(*) FROM notes n WHERE n.user_id = p_user_id
            AND n.completed = FALSE)                                    AS pending_notes,
        -- Notas sin tag
        (SELECT COUNT(*) FROM notes n WHERE n.user_id = p_user_id
            AND n.tag_id IS NULL)                                       AS notes_without_tag,
        -- Parte 2: desglose por tag
        t.id            AS tag_id,
        t.name          AS tag_name,
        t.color         AS tag_color,
        COUNT(n.id)     AS notes_in_tag
    FROM tags t
    LEFT JOIN notes n ON n.tag_id = t.id AND n.user_id = p_user_id
    WHERE t.user_id = p_user_id
    GROUP BY t.id, t.name, t.color
    ORDER BY notes_in_tag DESC;
END;
$$;

COMMENT ON FUNCTION sp_get_notes_summary IS
'Retorna estadísticas de notas del usuario: totales y desglose por tag.
Útil para dashboard y reportes. Llamar con SELECT * FROM sp_get_notes_summary(userId).';
