-- Siembra el rol de empleado/vendedor. Idempotente: no duplica si ya existe.
-- ROLE_EMPLEADO se asigna SOLO por canal protegido (POST /api/v1/usuarios, ADMIN).
-- No se confunde con ROLE_USER, que es el registro público sin permisos de venta.

INSERT INTO roles (nombre)
SELECT 'ROLE_EMPLEADO'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'ROLE_EMPLEADO');
