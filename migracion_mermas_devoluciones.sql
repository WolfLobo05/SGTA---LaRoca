-- =====================================================================
-- Migración de mermas y devoluciones de SGTA_LaRoca
--
-- MermaDAO, DevolucionClienteDAO y DevolucionProveedorDAO leen y escriben
-- la columna estado (las canceladas quedan 'Inactivo'), pero ni
-- SGTA_LaRoca.sql ni las migraciones anteriores la creaban. Sin ella las
-- pantallas de Mermas y Devoluciones fallan con
--   Unknown column 'm.estado' / 'd.estado' / 'dp.estado'.
--
-- - Mermas.estado
-- - Devolucion_Clientes.estado
-- - Devolucion_Proveedores.estado
--
-- Los registros existentes quedan como 'Activo'.
-- Se puede ejecutar más de una vez: cada columna se agrega solo si
-- todavía no existe.
--
-- Ejecutar desde la carpeta del proyecto (PowerShell):
--   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3306 -u root '-p<password>' --default-character-set=utf8mb4 SGTA_LaRoca -e "source migracion_mermas_devoluciones.sql"
-- Se recomienda respaldar antes:  mysqldump -u root -p SGTA_LaRoca > respaldo.sql
-- =====================================================================

USE SGTA_LaRoca;


-- ---------------------------------------------------------------------
-- 1. Mermas.estado
-- ---------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Mermas')
                 AND LOWER(COLUMN_NAME) = LOWER('estado'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Mermas ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER fecha',
    'SELECT ''Mermas.estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 2. Devolucion_Clientes.estado
-- ---------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Devolucion_Clientes')
                 AND LOWER(COLUMN_NAME) = LOWER('estado'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Devolucion_Clientes ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER fecha',
    'SELECT ''Devolucion_Clientes.estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 3. Devolucion_Proveedores.estado
-- ---------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = LOWER('Devolucion_Proveedores')
                 AND LOWER(COLUMN_NAME) = LOWER('estado'));
SET @sql = IF(@existe = 0,
    'ALTER TABLE Devolucion_Proveedores ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER fecha',
    'SELECT ''Devolucion_Proveedores.estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 4. Verificación (solo consulta): debe mostrar las 3 columnas.
-- ---------------------------------------------------------------------

SELECT TABLE_NAME AS tabla, COLUMN_NAME AS columna, COLUMN_TYPE AS tipo,
       COLUMN_DEFAULT AS valor_por_omision
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND LOWER(COLUMN_NAME) = 'estado'
  AND LOWER(TABLE_NAME) IN ('mermas', 'devolucion_clientes', 'devolucion_proveedores')
ORDER BY 1;
