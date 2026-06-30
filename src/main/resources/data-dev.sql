-- Seed de desarrollo (perfil dev, H2 en memoria). Se ejecuta en cada arranque
-- DESPUES de que Hibernate crea el esquema (defer-datasource-initialization=true)
-- y SOLO en dev (spring.sql.init.platform=dev -> Spring carga data-${platform}.sql).
--
-- Como H2 es efimera, cada arranque nace vacia: INSERT planos, sin idempotencia.
-- Tras sembrar con IDs explicitos, reiniciamos cada IDENTITY por encima del maximo
-- para que las inserciones siguientes (DataInitializer crea 'admin', y la app en
-- runtime) no colisionen con los IDs sembrados.
-- ponytail: feature nativo de Spring SQL init; sin seeder Java. Si en el futuro se
-- necesita logica (passwords dinamicos, datos calculados), mover a un CommandLineRunner.

-- ── Roles ───────────────────────────────────────────────────────────────────
-- DataInitializer (dev) los busca por nombre; al existir, no los duplica.
INSERT INTO roles (id, nombre) VALUES (1, 'ROLE_USER');
INSERT INTO roles (id, nombre) VALUES (2, 'ROLE_ADMIN');
INSERT INTO roles (id, nombre) VALUES (3, 'ROLE_EMPLEADO');
ALTER TABLE roles ALTER COLUMN id RESTART WITH 10;

-- ── Usuarios (vendedores) ─────────────────────────────────────────────────────
-- El admin (admin/admin123) lo crea DataInitializer tras este script.
-- password BCrypt de 'password' para los dos vendedores.
INSERT INTO usuarios (id, username, password, email) VALUES
  (1, 'jperez', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'jperez@electrogenos.com'),
  (2, 'mgomez', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'mgomez@electrogenos.com');

INSERT INTO usuarios_roles (usuario_id, role_id) VALUES
  (1, 3),  -- jperez  -> ROLE_EMPLEADO
  (2, 3);  -- mgomez  -> ROLE_EMPLEADO
ALTER TABLE usuarios ALTER COLUMN id RESTART WITH 100;

-- ── Clientes (entidades) ──────────────────────────────────────────────────────
INSERT INTO entidades (id, nombre, created_at, updated_at) VALUES
  (1, 'Constructora del Sur S.A.', TIMESTAMP '2026-05-01 09:00:00', TIMESTAMP '2026-05-01 09:00:00'),
  (2, 'Minera Andes SRL',          TIMESTAMP '2026-05-03 09:00:00', TIMESTAMP '2026-05-03 09:00:00');
ALTER TABLE entidades ALTER COLUMN id RESTART WITH 100;

-- ── Grupos electrogenos (9: 5 fijos + 4 moviles) ─────────────────────────────
-- JOINED inheritance: los moviles llevan fila en grupos_electrogenos Y en
-- grupos_electrogenos_moviles con el mismo id. version=0 (columna @Version NOT NULL).
INSERT INTO grupos_electrogenos
  (id, codigo, vida_util, tipo_combustible, tipo_arranque, p_min, p_max, insonorizado, capo, stock, version, created_at, updated_at) VALUES
  (1, 'GE-FIJ-001', 10, 'GASOIL',      'AUTOMATICO', 5.0,  20.0,  TRUE,  TRUE,  8, 0, TIMESTAMP '2026-05-05 10:00:00', TIMESTAMP '2026-05-05 10:00:00'),
  (2, 'GE-FIJ-002', 12, 'GASOIL',      'MANUAL',     10.0, 40.0,  FALSE, TRUE,  5, 0, TIMESTAMP '2026-05-05 10:00:00', TIMESTAMP '2026-05-05 10:00:00'),
  (3, 'GE-FIJ-003', 8,  'NAFTA',       'AUTOMATICO', 3.0,  12.0,  TRUE,  FALSE, 12,0, TIMESTAMP '2026-05-05 10:00:00', TIMESTAMP '2026-05-05 10:00:00'),
  (4, 'GE-FIJ-004', 15, 'GAS_NATURAL', 'AUTOMATICO', 20.0, 80.0,  TRUE,  TRUE,  3, 0, TIMESTAMP '2026-05-05 10:00:00', TIMESTAMP '2026-05-05 10:00:00'),
  (5, 'GE-FIJ-005', 10, 'NAFTA',       'MANUAL',     2.0,  8.0,   FALSE, FALSE, 20,0, TIMESTAMP '2026-05-05 10:00:00', TIMESTAMP '2026-05-05 10:00:00'),
  (6, 'GE-MOV-001', 9,  'GASOIL',      'AUTOMATICO', 6.0,  25.0,  TRUE,  TRUE,  7, 0, TIMESTAMP '2026-05-06 10:00:00', TIMESTAMP '2026-05-06 10:00:00'),
  (7, 'GE-MOV-002', 11, 'NAFTA',       'MANUAL',     4.0,  15.0,  FALSE, TRUE,  9, 0, TIMESTAMP '2026-05-06 10:00:00', TIMESTAMP '2026-05-06 10:00:00'),
  (8, 'GE-MOV-003', 14, 'GAS_NATURAL', 'AUTOMATICO', 15.0, 60.0,  TRUE,  TRUE,  4, 0, TIMESTAMP '2026-05-06 10:00:00', TIMESTAMP '2026-05-06 10:00:00'),
  (9, 'GE-MOV-004', 7,  'GASOIL',      'MANUAL',     8.0,  30.0,  FALSE, FALSE, 6, 0, TIMESTAMP '2026-05-06 10:00:00', TIMESTAMP '2026-05-06 10:00:00');

INSERT INTO grupos_electrogenos_moviles (id, cantidad_ruedas, material_eje) VALUES
  (6, 2, 'ACERO'),
  (7, 2, 'ALEACION'),
  (8, 4, 'ACERO'),
  (9, 2, 'ALEACION');
ALTER TABLE grupos_electrogenos ALTER COLUMN id RESTART WITH 100;

-- ── Ventas (solicitudes_compra) ──────────────────────────────────────────────
-- precio_unitario y total CONGELADOS (no se recalculan). Atribuidas a vendedores
-- y repartidas entre los 2 clientes con distinto volumen (para el ranking).
--
-- IMPORTANTE: precio_unitario = SALIDA REAL de GrupoElectrogenoServiceImpl.calcularPrecioVenta()
-- para el grupo_id asignado (precio = vidaUtil*(pMin+pMax)/2  +10 si insonorizado∧capo
-- +15 si AUTOMATICO;  fijo +200;  movil +ruedas*5 +(ACERO:20|ALEACION:13)).
-- total = precio_unitario * cantidad. No usar montos inventados: deben coincidir con la fórmula.
--   g1=350.0  g2=500.0  g3=275.0  g4=975.0  g5=250.0  g6=194.5  g7=127.5  g8=590.0  g9=156.0
INSERT INTO solicitudes_compra
  (id, identificador, nombre_solicitante, cantidad, potencia_requerida, vida_util_solicitada,
   tipo_combustible, tipo_pago, precio_unitario, total, entidad_id, grupo_id, vendedor_id, created_at, updated_at) VALUES
  (1, 'SOL-0001', 'Constructora del Sur S.A.', 2, 18.0, 8,  'GASOIL',      'EFECTIVO', 350.0,  700.0,  1, 1, 1, TIMESTAMP '2026-05-10 11:00:00', TIMESTAMP '2026-05-10 11:00:00'),
  (2, 'SOL-0002', 'Constructora del Sur S.A.', 1, 35.0, 10, 'GASOIL',      'CHEQUE',   500.0,  500.0,  1, 2, 1, TIMESTAMP '2026-05-12 11:00:00', TIMESTAMP '2026-05-12 11:00:00'),
  (3, 'SOL-0003', 'Constructora del Sur S.A.', 3, 10.0, 8,  'NAFTA',       'EFECTIVO', 275.0,  825.0,  1, 3, 2, TIMESTAMP '2026-05-15 11:00:00', TIMESTAMP '2026-05-15 11:00:00'),
  (4, 'SOL-0004', 'Constructora del Sur S.A.', 1, 22.0, 9,  'GASOIL',      'CHEQUE',   194.5,  194.5,  1, 6, 2, TIMESTAMP '2026-05-18 11:00:00', TIMESTAMP '2026-05-18 11:00:00'),
  (5, 'SOL-0005', 'Constructora del Sur S.A.', 2, 14.0, 11, 'NAFTA',       'EFECTIVO', 127.5,  255.0,  1, 7, 1, TIMESTAMP '2026-05-20 11:00:00', TIMESTAMP '2026-05-20 11:00:00'),
  (6, 'SOL-0006', 'Minera Andes SRL',          1, 70.0, 15, 'GAS_NATURAL', 'CHEQUE',   975.0,  975.0,  2, 4, 2, TIMESTAMP '2026-05-22 11:00:00', TIMESTAMP '2026-05-22 11:00:00'),
  (7, 'SOL-0007', 'Minera Andes SRL',          2, 55.0, 14, 'GAS_NATURAL', 'CHEQUE',   590.0,  1180.0, 2, 8, 1, TIMESTAMP '2026-05-25 11:00:00', TIMESTAMP '2026-05-25 11:00:00'),
  (8, 'SOL-0008', 'Minera Andes SRL',          1, 28.0, 7,  'GASOIL',      'EFECTIVO', 156.0,  156.0,  2, 9, 2, TIMESTAMP '2026-05-28 11:00:00', TIMESTAMP '2026-05-28 11:00:00');
ALTER TABLE solicitudes_compra ALTER COLUMN id RESTART WITH 100;
