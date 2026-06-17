-- I4: precio unitario y total CONGELADOS al momento de la venta.
-- Se persisten una sola vez (precio_unitario es inmutable); los reportes leen
-- estos valores en vez de recalcular desde el grupo actual.
-- Backfill 0 para filas existentes (en prod la tabla arranca vacía).

ALTER TABLE solicitudes_compra
    ADD COLUMN precio_unitario FLOAT(53) NOT NULL DEFAULT 0;

ALTER TABLE solicitudes_compra
    ADD COLUMN total FLOAT(53) NOT NULL DEFAULT 0;
