-- I5: columna de versión para bloqueo optimista (@Version) en grupos electrógenos.
-- Evita lost updates cuando ventas concurrentes descuentan stock del mismo grupo.

ALTER TABLE grupos_electrogenos
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
