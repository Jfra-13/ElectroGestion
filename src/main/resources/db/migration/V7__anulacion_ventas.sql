-- Anulación de ventas: soft-delete reversible con auditoría.
-- Una venta no se borra ni se edita libremente: se anula (repone stock, deja
-- rastro de quién/cuándo/por qué) y sale de los reportes financieros.
-- DEFAULT 'ACTIVA' cubre todas las ventas legacy sin tocar dato.

ALTER TABLE solicitudes_compra ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA';
ALTER TABLE solicitudes_compra ADD COLUMN motivo_anulacion VARCHAR(500);
ALTER TABLE solicitudes_compra ADD COLUMN anulada_at TIMESTAMP;
ALTER TABLE solicitudes_compra ADD COLUMN anulada_por BIGINT;

ALTER TABLE solicitudes_compra
    ADD CONSTRAINT fk_solicitud_anulada_por
    FOREIGN KEY (anulada_por) REFERENCES usuarios (id);
