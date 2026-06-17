-- Siembra de roles del sistema. Idempotente: no duplica si ya existen.
-- El primer administrador NO se siembra aquí (la contraseña debe ir hasheada
-- con BCrypt): lo crea AdminBootstrap leyendo credenciales del entorno.

INSERT INTO roles (nombre)
SELECT 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'ROLE_USER');

INSERT INTO roles (nombre)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'ROLE_ADMIN');
