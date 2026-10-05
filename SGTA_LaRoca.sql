CREATE DATABASE IF NOT EXISTS SGTA_LaRoca;

USE SGTA_LaRoca;

CREATE TABLE Roles (
    id_rol INTEGER UNSIGNED NOT NULL,
    estado VARCHAR(30) NULL,
    nombre VARCHAR(100) NULL,
    descripcion VARCHAR(100) NULL,
    PRIMARY KEY (id_rol)
);



CREATE TABLE Unidad_Medida (
    id_unidad INTEGER UNSIGNED NOT NULL,
    estado VARCHAR(30) NULL,
    abreviatura VARCHAR(10) NULL,
    nombre VARCHAR(50) NULL,
    PRIMARY KEY (id_unidad)
);



CREATE TABLE Categoria (
    id_categoria INTEGER UNSIGNED NOT NULL,
    estado VARCHAR(30) NULL,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(100) NULL,
    PRIMARY KEY (id_categoria)
);



CREATE TABLE Clientes (
    id_cliente INTEGER UNSIGNED NOT NULL,
    nombre VARCHAR(100) NULL,
    apellido_paterno VARCHAR(100) NULL,
    apellido_materno VARCHAR(100) NULL,
    telefono VARCHAR(15) NULL,
    correo VARCHAR(150) NULL,
    PRIMARY KEY (id_cliente)
);



CREATE TABLE Metodos_Pago (
    id_metodo INTEGER UNSIGNED NOT NULL,
    nombre VARCHAR(50) NULL,
    PRIMARY KEY (id_metodo)
);


CREATE TABLE Proveedores (
    id_proveedor INTEGER UNSIGNED NOT NULL,
    rfc VARCHAR(13) NULL,
    nombre VARCHAR(100) NULL,
    telefono VARCHAR(15) NULL,
    correo VARCHAR(150) NULL,
    direccion VARCHAR(100) NULL,
    contacto VARCHAR(15) NULL,
    estado VARCHAR(30) NULL,
    PRIMARY KEY (id_proveedor)
);





CREATE TABLE Empleados (
    id_empleado INTEGER UNSIGNED NOT NULL,
    id_rol INTEGER UNSIGNED NOT NULL,
    estado VARCHAR(30) NULL,
    usuario VARCHAR(100) NULL,
    password_hash VARCHAR(255) NULL,
    nombre VARCHAR(100) NULL,
    apellido_paterno VARCHAR(100) NULL,
    apellido_materno VARCHAR(100) NULL,
    telefono VARCHAR(15) NULL,
    correo VARCHAR(150) NULL,
    PRIMARY KEY (id_empleado),
    CONSTRAINT fk_empleados_roles FOREIGN KEY (id_rol) REFERENCES Roles(id_rol)
);




CREATE TABLE Productos (
    id_producto INTEGER UNSIGNED NOT NULL,
    id_unidad INTEGER UNSIGNED NOT NULL,
    id_categoria INTEGER UNSIGNED NOT NULL,
    codigo_barras VARCHAR(50) NOT NULL,
    nombre VARCHAR(50) NULL,
    precio DECIMAL(10, 2) NULL,
    punto_reorden INTEGER UNSIGNED NULL,
    PRIMARY KEY (id_producto),
    CONSTRAINT fk_productos_unidad FOREIGN KEY (id_unidad) REFERENCES Unidad_Medida(id_unidad),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (id_categoria) REFERENCES Categoria(id_categoria),
    CONSTRAINT uq_productos_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT chk_productos_precio CHECK (precio IS NULL OR precio > 0)
);



CREATE TABLE Compras (
    id_compras INTEGER UNSIGNED NOT NULL,
    id_proveedor INTEGER UNSIGNED NOT NULL,
    id_empleado INTEGER UNSIGNED NOT NULL,
    fecha DATE NULL,
    total DECIMAL(10, 2) NULL,
    PRIMARY KEY (id_compras),
    CONSTRAINT fk_compras_proveedor FOREIGN KEY (id_proveedor) REFERENCES Proveedores(id_proveedor),
    CONSTRAINT fk_compras_empleado FOREIGN KEY (id_empleado) REFERENCES Empleados(id_empleado),
    CONSTRAINT chk_compras_total CHECK (total IS NULL OR total >= 0)
);



CREATE TABLE Ventas (
    id_venta INTEGER UNSIGNED NOT NULL,
    id_empleado INTEGER UNSIGNED NOT NULL,
    id_cliente INTEGER UNSIGNED NOT NULL,
    id_metodo INTEGER UNSIGNED NOT NULL,
    fecha DATE NULL,
    total DECIMAL(10, 2) NULL,
    PRIMARY KEY (id_venta),
    CONSTRAINT fk_ventas_empleado FOREIGN KEY (id_empleado) REFERENCES Empleados(id_empleado),
    CONSTRAINT fk_ventas_cliente FOREIGN KEY (id_cliente) REFERENCES Clientes(id_cliente),
    CONSTRAINT fk_ventas_metodo FOREIGN KEY (id_metodo) REFERENCES Metodos_Pago(id_metodo),
    CONSTRAINT chk_ventas_total CHECK (total IS NULL OR total >= 0)
);



CREATE TABLE Mermas (
    id_merma INTEGER UNSIGNED NOT NULL,
    id_empleado INTEGER UNSIGNED NOT NULL,
    fecha DATE NULL,
    PRIMARY KEY (id_merma),
    CONSTRAINT fk_mermas_empleado FOREIGN KEY (id_empleado) REFERENCES Empleados(id_empleado)
);





CREATE TABLE Devolucion_Clientes (
    id_devolucion_cliente INTEGER UNSIGNED NOT NULL,
    id_venta INTEGER UNSIGNED NOT NULL,
    id_empleado INTEGER UNSIGNED NOT NULL,
    fecha DATE NULL,
    PRIMARY KEY (id_devolucion_cliente),
    CONSTRAINT fk_devolucion_clientes_venta FOREIGN KEY (id_venta) REFERENCES Ventas(id_venta),
    CONSTRAINT fk_devolucion_clientes_empleado FOREIGN KEY (id_empleado) REFERENCES Empleados(id_empleado)
);


CREATE TABLE Devolucion_Proveedores (
    id_devolucion_proveedor INTEGER UNSIGNED NOT NULL,
    id_compras INTEGER UNSIGNED NOT NULL,
    id_empleado INTEGER UNSIGNED NOT NULL,
    fecha DATE NULL,
    PRIMARY KEY (id_devolucion_proveedor),
    CONSTRAINT fk_devolucion_proveedores_compra FOREIGN KEY (id_compras) REFERENCES Compras(id_compras),
    CONSTRAINT fk_devolucion_proveedores_empleado FOREIGN KEY (id_empleado) REFERENCES Empleados(id_empleado)
);



CREATE TABLE detalle_compras (
    id_detalle_compra INTEGER UNSIGNED NOT NULL,
    id_producto INTEGER UNSIGNED NOT NULL,
    id_compras INTEGER UNSIGNED NOT NULL,
    cantidad INTEGER UNSIGNED NULL,
    precio_compra DECIMAL(10, 2) NULL,
    PRIMARY KEY (id_detalle_compra),
    CONSTRAINT fk_detalle_compras_producto FOREIGN KEY (id_producto) REFERENCES Productos(id_producto),
    CONSTRAINT fk_detalle_compras_compra FOREIGN KEY (id_compras) REFERENCES Compras(id_compras),
    CONSTRAINT chk_detalle_compras_cantidad CHECK (cantidad IS NULL OR cantidad > 0),
    CONSTRAINT chk_detalle_compras_precio CHECK (precio_compra IS NULL OR precio_compra > 0)
);



CREATE TABLE detalle_ventas (
    id_detalle_venta INTEGER UNSIGNED NOT NULL,
    id_venta INTEGER UNSIGNED NOT NULL,
    id_producto INTEGER UNSIGNED NOT NULL,
    cantidad INTEGER UNSIGNED NULL,
    precio_unitario DECIMAL(10, 2) NULL,
    PRIMARY KEY (id_detalle_venta),
    CONSTRAINT fk_detalle_ventas_venta FOREIGN KEY (id_venta) REFERENCES Ventas(id_venta),
    CONSTRAINT fk_detalle_ventas_producto FOREIGN KEY (id_producto) REFERENCES Productos(id_producto),
    CONSTRAINT chk_detalle_ventas_cantidad CHECK (cantidad IS NULL OR cantidad > 0),
    CONSTRAINT chk_detalle_ventas_precio CHECK (precio_unitario IS NULL OR precio_unitario > 0)
);



CREATE TABLE detalle_mermas (
    id_detalle_merma INTEGER UNSIGNED NOT NULL,
    id_producto INTEGER UNSIGNED NOT NULL,
    id_merma INTEGER UNSIGNED NOT NULL,
    cantidad INTEGER UNSIGNED NULL,
    precio DECIMAL(10, 2) NULL,
    motivo VARCHAR(100) NULL,
    PRIMARY KEY (id_detalle_merma),
    CONSTRAINT fk_detalle_mermas_producto FOREIGN KEY (id_producto) REFERENCES Productos(id_producto),
    CONSTRAINT fk_detalle_mermas_merma FOREIGN KEY (id_merma) REFERENCES Mermas(id_merma),
    CONSTRAINT chk_detalle_mermas_cantidad CHECK (cantidad IS NULL OR cantidad > 0),
    CONSTRAINT chk_detalle_mermas_precio CHECK (precio IS NULL OR precio >= 0)
);



CREATE TABLE detalle_devolucion_clientes (
    id_detalle_devolucion_cliente INTEGER UNSIGNED NOT NULL,
    id_producto INTEGER UNSIGNED NOT NULL,
    id_devolucion_cliente INTEGER UNSIGNED NOT NULL,
    cantidad INTEGER UNSIGNED NULL,
    precio DECIMAL(10, 2) NULL,
    motivo VARCHAR(100) NULL,
    PRIMARY KEY (id_detalle_devolucion_cliente),
    CONSTRAINT fk_detalle_devolucion_clientes_producto FOREIGN KEY (id_producto) REFERENCES Productos(id_producto),
    CONSTRAINT fk_detalle_devolucion_clientes_devolucion FOREIGN KEY (id_devolucion_cliente) REFERENCES Devolucion_Clientes(id_devolucion_cliente),
    CONSTRAINT chk_detalle_devolucion_clientes_cantidad CHECK (cantidad IS NULL OR cantidad > 0),
    CONSTRAINT chk_detalle_devolucion_clientes_precio CHECK (precio IS NULL OR precio >= 0)
);



CREATE TABLE detalle_devolucion_proveedores (
    id_detalle_devolucion_proveedor INTEGER UNSIGNED NOT NULL,
    id_producto INTEGER UNSIGNED NOT NULL,
    id_devolucion_proveedor INTEGER UNSIGNED NOT NULL,
    cantidad INTEGER UNSIGNED NULL,
    precio DECIMAL(10, 2) NULL,
    motivo VARCHAR(100) NULL,
    PRIMARY KEY (id_detalle_devolucion_proveedor),
    CONSTRAINT fk_detalle_devolucion_proveedores_producto FOREIGN KEY (id_producto) REFERENCES Productos(id_producto),
    CONSTRAINT fk_detalle_devolucion_proveedores_devolucion FOREIGN KEY (id_devolucion_proveedor) REFERENCES Devolucion_Proveedores(id_devolucion_proveedor),
    CONSTRAINT chk_detalle_devolucion_proveedores_cantidad CHECK (cantidad IS NULL OR cantidad > 0),
    CONSTRAINT chk_detalle_devolucion_proveedores_precio CHECK (precio IS NULL OR precio >= 0)
);
