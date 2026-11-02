-- =====================================================================
-- Migración del módulo de ventas de SGTA_LaRoca
-- Requiere haber ejecutado antes migracion.sql (columna estado en Clientes).
--
-- - Ventas: fecha pasa a DATETIME; nuevas columnas monto_pagado,
--   cambio y estado.
-- - Productos: nueva columna existencia.
-- - Métodos de pago: Efectivo, Tarjeta y Transferencia.
-- - Cliente 'Público en general' para ventas de mostrador.
--
-- Se puede ejecutar más de una vez: cada columna se agrega solo si
-- todavía no existe, y los datos solo se insertan si faltan.
-- Se recomienda respaldar antes:  mysqldump -u root -p SGTA_LaRoca > respaldo.sql
-- =====================================================================

USE SGTA_LaRoca;

-- MySQL Workbench bloquea los UPDATE sin llave en el WHERE (error 1175).
SET @safe_updates_anterior = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;


-- ---------------------------------------------------------------------
-- 1. Ventas
-- ---------------------------------------------------------------------

-- Guardar también la hora de la venta (las fechas existentes quedan a las 00:00).
ALTER TABLE Ventas
    MODIFY fecha DATETIME NULL;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = 'ventas'
                 AND LOWER(COLUMN_NAME) = 'monto_pagado');
SET @sql = IF(@existe = 0,
    'ALTER TABLE Ventas ADD COLUMN monto_pagado DECIMAL(10, 2) NULL AFTER total',
    'SELECT ''monto_pagado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = 'ventas'
                 AND LOWER(COLUMN_NAME) = 'cambio');
SET @sql = IF(@existe = 0,
    'ALTER TABLE Ventas ADD COLUMN cambio DECIMAL(10, 2) NULL AFTER monto_pagado',
    'SELECT ''cambio ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = 'ventas'
                 AND LOWER(COLUMN_NAME) = 'estado');
SET @sql = IF(@existe = 0,
    'ALTER TABLE Ventas ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT ''Activo'' AFTER cambio',
    'SELECT ''estado ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 2. Productos: existencia
--    Los productos existentes quedan con existencia 0; hay que
--    capturar su inventario antes de poder venderlos.
-- ---------------------------------------------------------------------

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND LOWER(TABLE_NAME) = 'productos'
                 AND LOWER(COLUMN_NAME) = 'existencia');
SET @sql = IF(@existe = 0,
    'ALTER TABLE Productos ADD COLUMN existencia INT NOT NULL DEFAULT 0 AFTER punto_reorden',
    'SELECT ''existencia ya existe''');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;


-- ---------------------------------------------------------------------
-- 3. Métodos de pago
-- ---------------------------------------------------------------------

INSERT IGNORE INTO Metodos_Pago (id_metodo, nombre) VALUES
    (1, 'Efectivo'),
    (2, 'Tarjeta'),
    (3, 'Transferencia');


-- ---------------------------------------------------------------------
-- 4. Cliente 'Público en general' (solo si no existe)
-- ---------------------------------------------------------------------

INSERT INTO Clientes
    (id_cliente, nombre, apellido_paterno, apellido_materno, telefono, correo, estado)
SELECT siguiente.id,
       'Público en general', NULL, NULL,
       '0000000000', 'publico@laroca.mx', 'Activo'
FROM (SELECT COALESCE(MAX(id_cliente), 0) + 1 AS id FROM Clientes) AS siguiente
WHERE NOT EXISTS (SELECT 1 FROM (SELECT nombre FROM Clientes) AS c
                  WHERE c.nombre = 'Público en general');


SET SQL_SAFE_UPDATES = @safe_updates_anterior;


-- ---------------------------------------------------------------------
-- 5. Inventario inicial (EJEMPLO, no se ejecuta)
--    Ajusta los valores y quita el comentario para cargar existencias:
--
-- UPDATE Productos SET existencia = 50 WHERE codigo_barras = '7501055300075';
-- ---------------------------------------------------------------------
