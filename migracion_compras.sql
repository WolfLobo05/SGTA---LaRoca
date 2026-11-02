-- =====================================================================
-- Migración del módulo de compras de SGTA_LaRoca
--
-- - Compras: nueva columna estado (las compras canceladas quedan 'Inactivo').
-- - Por seguridad, agrega también Clientes.estado, Productos.estado y
--   Productos.existencia si faltan (bases que nunca ejecutaron
--   migracion.sql o migracion_ventas.sql).
--
-- Se puede ejecutar más de una vez: cada columna se agrega solo si
-- todavía no existe.
--
-- En Windows (lower_case_table_names=1) MySQL guarda los nombres de
-- tablas en minúsculas, por eso las comparaciones usan LOWER() en ambos
-- lados.
--
-- Ejecutar desde la carpeta del proyecto (PowerShell):
--   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3306 -u root '-p<password>' --default-character-set=utf8mb4 SGTA_LaRoca -e "source migracion_compras.sql"
-- Se recomienda respaldar antes:  mysqldump -u root -p SGTA_LaRoca > respaldo.sql
-- =====================================================================

USE SGTA_LaRoca;


-- ---------------------------------------------------------------------
-- 1. Compras.estado
-- ---------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Compras')
                 AND LOWER(COLUMN_NAME) = LOWER('estado'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Compras ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER total',
    'SELECT ''Compras.estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 2. Columnas de migraciones anteriores, solo si faltan
-- ---------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Clientes')
                 AND LOWER(COLUMN_NAME) = LOWER('estado'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Clientes ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER correo',
    'SELECT ''Clientes.estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Productos')
                 AND LOWER(COLUMN_NAME) = LOWER('estado'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Productos ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER punto_reorden',
    'SELECT ''Productos.estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;

-- Registrar Compra suma a esta columna.
SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Productos')
                 AND LOWER(COLUMN_NAME) = LOWER('existencia'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Productos ADD COLUMN existencia INT NOT NULL DEFAULT 0 AFTER punto_reorden',
    'SELECT ''Productos.existencia ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 3. Verificación (solo consulta): debe mostrar las 4 columnas.
-- ---------------------------------------------------------------------

SELECT TABLE_NAME AS tabla, COLUMN_NAME AS columna, COLUMN_TYPE AS tipo,
       COLUMN_DEFAULT AS valor_por_omision
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((LOWER(TABLE_NAME) = LOWER('Compras') AND LOWER(COLUMN_NAME) = LOWER('estado'))
    OR (LOWER(TABLE_NAME) = LOWER('Clientes') AND LOWER(COLUMN_NAME) = LOWER('estado'))
    OR (LOWER(TABLE_NAME) = LOWER('Productos') AND LOWER(COLUMN_NAME) IN ('estado', 'existencia')))
ORDER BY 1, 2;
