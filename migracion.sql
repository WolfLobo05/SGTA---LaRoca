-- =====================================================================
-- Migración de SGTA_LaRoca
-- Actualiza una base creada con la versión anterior de SGTA_LaRoca.sql.
--
-- - Agrega la columna estado a Clientes y Productos.
-- - Deja estado como NOT NULL DEFAULT 'Activo' en todas las tablas
--   y unifica los valores a 'Activo' / 'Inactivo'.
-- - Hace obligatorios teléfono y correo en Clientes, Proveedores y Empleados.
--
-- Ejecutar UNA sola vez (el ADD COLUMN falla si se repite).
-- Se recomienda respaldar antes:  mysqldump -u root -p SGTA_LaRoca > respaldo.sql
-- =====================================================================

USE SGTA_LaRoca;

-- MySQL Workbench bloquea los UPDATE sin llave en el WHERE (error 1175).
SET @safe_updates_anterior = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;


-- ---------------------------------------------------------------------
-- 1. Columna estado nueva en Clientes y Productos.
--    Los registros existentes quedan como 'Activo'.
-- ---------------------------------------------------------------------

ALTER TABLE Clientes
    ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'Activo' AFTER correo;

ALTER TABLE Productos
    ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'Activo' AFTER punto_reorden;


-- ---------------------------------------------------------------------
-- 2. Unificar los valores de estado existentes.
--    NULL, vacío o 'ACTIVO' (cualquier mayúscula) -> 'Activo'
--    'INACTIVO' (cualquier mayúscula)             -> 'Inactivo'
--    ProveedorDAO guardaba 'ACTIVO' en mayúsculas.
-- ---------------------------------------------------------------------

UPDATE Roles
SET estado = 'Activo'
WHERE estado IS NULL OR UPPER(TRIM(estado)) IN ('', 'ACTIVO');

UPDATE Roles
SET estado = 'Inactivo'
WHERE UPPER(TRIM(estado)) = 'INACTIVO';

UPDATE Unidad_Medida
SET estado = 'Activo'
WHERE estado IS NULL OR UPPER(TRIM(estado)) IN ('', 'ACTIVO');

UPDATE Unidad_Medida
SET estado = 'Inactivo'
WHERE UPPER(TRIM(estado)) = 'INACTIVO';

UPDATE Categoria
SET estado = 'Activo'
WHERE estado IS NULL OR UPPER(TRIM(estado)) IN ('', 'ACTIVO');

UPDATE Categoria
SET estado = 'Inactivo'
WHERE UPPER(TRIM(estado)) = 'INACTIVO';

UPDATE Proveedores
SET estado = 'Activo'
WHERE estado IS NULL OR UPPER(TRIM(estado)) IN ('', 'ACTIVO');

UPDATE Proveedores
SET estado = 'Inactivo'
WHERE UPPER(TRIM(estado)) = 'INACTIVO';

UPDATE Empleados
SET estado = 'Activo'
WHERE estado IS NULL OR UPPER(TRIM(estado)) IN ('', 'ACTIVO');

UPDATE Empleados
SET estado = 'Inactivo'
WHERE UPPER(TRIM(estado)) = 'INACTIVO';


-- ---------------------------------------------------------------------
-- 3. estado NOT NULL con 'Activo' por defecto.
-- ---------------------------------------------------------------------

ALTER TABLE Roles
    MODIFY estado VARCHAR(30) NOT NULL DEFAULT 'Activo';

ALTER TABLE Unidad_Medida
    MODIFY estado VARCHAR(30) NOT NULL DEFAULT 'Activo';

ALTER TABLE Categoria
    MODIFY estado VARCHAR(30) NOT NULL DEFAULT 'Activo';

ALTER TABLE Proveedores
    MODIFY estado VARCHAR(30) NOT NULL DEFAULT 'Activo';

ALTER TABLE Empleados
    MODIFY estado VARCHAR(30) NOT NULL DEFAULT 'Activo';


-- ---------------------------------------------------------------------
-- 4. Teléfono y correo obligatorios.
--    Los NULL existentes se cambian por cadena vacía para poder aplicar
--    NOT NULL. Esos registros se deben completar desde la aplicación
--    (no se pueden guardar hasta tener un teléfono y un correo válidos).
--    Ver la consulta del punto 5.
-- ---------------------------------------------------------------------

UPDATE Clientes    SET telefono = '' WHERE telefono IS NULL;
UPDATE Clientes    SET correo   = '' WHERE correo   IS NULL;
UPDATE Proveedores SET telefono = '' WHERE telefono IS NULL;
UPDATE Proveedores SET correo   = '' WHERE correo   IS NULL;
UPDATE Empleados   SET telefono = '' WHERE telefono IS NULL;
UPDATE Empleados   SET correo   = '' WHERE correo   IS NULL;

ALTER TABLE Clientes
    MODIFY telefono VARCHAR(15) NOT NULL,
    MODIFY correo VARCHAR(150) NOT NULL;

ALTER TABLE Proveedores
    MODIFY telefono VARCHAR(15) NOT NULL,
    MODIFY correo VARCHAR(150) NOT NULL;

ALTER TABLE Empleados
    MODIFY telefono VARCHAR(15) NOT NULL,
    MODIFY correo VARCHAR(150) NOT NULL;


SET SQL_SAFE_UPDATES = @safe_updates_anterior;


-- ---------------------------------------------------------------------
-- 5. Revisión (solo consulta, no modifica nada).
-- ---------------------------------------------------------------------

-- Registros que quedaron sin teléfono o correo y hay que completar.
SELECT 'Clientes' AS tabla, id_cliente AS id, nombre, telefono, correo
FROM Clientes
WHERE telefono = '' OR correo = ''
UNION ALL
SELECT 'Proveedores', id_proveedor, nombre, telefono, correo
FROM Proveedores
WHERE telefono = '' OR correo = ''
UNION ALL
SELECT 'Empleados', id_empleado, usuario, telefono, correo
FROM Empleados
WHERE telefono = '' OR correo = '';

-- Estados distintos de 'Activo' / 'Inactivo'. La aplicación los trata
-- como inactivos; corrígelos a mano si alguno debería estar activo.
SELECT 'Roles' AS tabla, id_rol AS id, estado FROM Roles WHERE estado NOT IN ('Activo', 'Inactivo')
UNION ALL
SELECT 'Unidad_Medida', id_unidad, estado FROM Unidad_Medida WHERE estado NOT IN ('Activo', 'Inactivo')
UNION ALL
SELECT 'Categoria', id_categoria, estado FROM Categoria WHERE estado NOT IN ('Activo', 'Inactivo')
UNION ALL
SELECT 'Proveedores', id_proveedor, estado FROM Proveedores WHERE estado NOT IN ('Activo', 'Inactivo')
UNION ALL
SELECT 'Empleados', id_empleado, estado FROM Empleados WHERE estado NOT IN ('Activo', 'Inactivo');
