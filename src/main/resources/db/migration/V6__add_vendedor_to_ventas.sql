-- Fase 2: cada venta registra quién la realizó (vendedor).
-- Nullable a propósito: las ventas legacy sin dueño quedan en NULL (en prod la
-- tabla arranca vacía, así que no hay backfill). Las ventas nuevas siempre lo
-- setean desde el usuario autenticado.

ALTER TABLE solicitudes_compra
    ADD COLUMN vendedor_id BIGINT;

ALTER TABLE solicitudes_compra
    ADD CONSTRAINT fk_solicitud_vendedor
    FOREIGN KEY (vendedor_id) REFERENCES usuarios (id);

-- Índice para filtrar/agrupar ventas por empleado (Fase 3).
CREATE INDEX idx_solicitud_vendedor ON solicitudes_compra (vendedor_id);
