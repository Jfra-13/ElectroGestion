INSERT INTO ENTIDADES (ID, CREATED_AT, NOMBRE, UPDATED_AT)
SELECT 1, CURRENT_TIMESTAMP, 'Empresa Demo', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
	SELECT 1
	FROM ENTIDADES
	WHERE ID = 1
);

-- El insert usa ID=1 explícito, pero la columna es IDENTITY: H2 no avanza la
-- secuencia con inserts explícitos. Se reinicia en 100 para que los tests que
-- guardan nuevas entidades no colisionen con el ID sembrado. (Solo H2 dev/test;
-- prod no ejecuta este script: usa Flyway.)
ALTER TABLE ENTIDADES ALTER COLUMN ID RESTART WITH 100;

