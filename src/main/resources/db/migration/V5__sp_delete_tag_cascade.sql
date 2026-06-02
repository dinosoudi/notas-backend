-- ─────────────────────────────────────────────
-- V5 — Stored Procedure: borrar tag en cascada
-- ─────────────────────────────────────────────
-- Usado cuando el usuario confirma borrar un tag
-- que tiene notas asignadas (force=true en el contrato)
--
-- Lo que hace:
--   1. Verifica que el tag pertenezca al usuario
--   2. Desvincula todas las notas del tag (tag_id = NULL)
--   3. Borra el tag
--   Todo en una sola transacción — o pasa todo o nada
--
-- Cómo llamarlo desde Java (JPA):
--   CALL sp_delete_tag_cascade(:tagId, :userId, :affectedNotes)

CREATE OR REPLACE PROCEDURE sp_delete_tag_cascade(
    p_tag_id        UUID,
    p_user_id       UUID,
    OUT p_affected_notes INT   -- cuántas notas quedaron sin tag
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_tag_exists BOOLEAN;
BEGIN
    -- 1. Verificar que el tag existe y pertenece al usuario
    SELECT EXISTS (
        SELECT 1 FROM tags
        WHERE id = p_tag_id
          AND user_id = p_user_id
    ) INTO v_tag_exists;

    IF NOT v_tag_exists THEN
        RAISE EXCEPTION 'TAG_NOT_FOUND: El tag % no existe o no pertenece al usuario %',
            p_tag_id, p_user_id;
    END IF;

    -- 2. Contar cuántas notas se van a desvincular
    SELECT COUNT(*) INTO p_affected_notes
    FROM notes
    WHERE tag_id = p_tag_id
      AND user_id = p_user_id;

    -- 3. Desvincular notas (tag_id = NULL)
    UPDATE notes
    SET tag_id = NULL,
        updated_at = NOW()
    WHERE tag_id = p_tag_id
      AND user_id = p_user_id;

    -- 4. Borrar el tag
    DELETE FROM tags
    WHERE id = p_tag_id
      AND user_id = p_user_id;

EXCEPTION
    WHEN OTHERS THEN
        -- Re-lanza el error para que Spring lo capture
        RAISE;
END;
$$;

COMMENT ON PROCEDURE sp_delete_tag_cascade IS
'Borra un tag y desvincula sus notas en una sola transacción.
Parámetros:
  p_tag_id      — UUID del tag a borrar
  p_user_id     — UUID del usuario (validación de pertenencia)
  p_affected_notes (OUT) — cuántas notas quedaron sin tag';
