DROP DATABASE IF EXISTS nexus_finance;
CREATE DATABASE nexus_finance
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE nexus_finance;

DROP TABLE IF EXISTS presupuestos     ;
DROP TABLE IF EXISTS gastos           ;
DROP TABLE IF EXISTS ingresos          ;
DROP TABLE IF EXISTS transacciones    ;
DROP TABLE IF EXISTS usuario_cuenta    ;
DROP TABLE IF EXISTS cuentas           ;
DROP TABLE IF EXISTS telefonos         ;
DROP TABLE IF EXISTS perfiles          ;
DROP TABLE IF EXISTS categorias        ;
DROP TABLE IF EXISTS usuarios          ;

DROP VIEW IF EXISTS v_saldo_usuario;
DROP VIEW IF EXISTS v_gasto_categoria_periodo;
DROP VIEW IF EXISTS v_historial_movimientos;

CREATE TABLE usuarios (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(80)  NOT NULL,
    email     VARCHAR(120) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    creado_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_email  UNIQUE (email),
    CONSTRAINT chk_usuarios_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) >= 3)
) ENGINE = InnoDB;

CREATE TABLE perfiles (
    usuario_id         INT          PRIMARY KEY,
    biografia          VARCHAR(300)     NULL,
    idioma_preferido   ENUM('es','en')  NOT NULL DEFAULT 'es',
    fecha_nacimiento   DATE             NULL,
    avatar_url         VARCHAR(255)     NULL,
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE telefonos (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  INT         NOT NULL,
    numero      VARCHAR(20) NOT NULL,
    tipo        ENUM('MOVIL','FIJO','TRABAJO') NOT NULL DEFAULT 'MOVIL',
    es_principal BOOLEAN    NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_telefono_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uq_telefono_usuario_numero UNIQUE (usuario_id, numero)
) ENGINE = InnoDB;

CREATE TABLE categorias (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE cuentas (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(80) NOT NULL,
    tipo_cuenta    VARCHAR(40) NOT NULL,
    balance_actual DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    propietario_id INT NOT NULL,
    CONSTRAINT fk_cuenta_propietario FOREIGN KEY (propietario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT chk_cuentas_tipo CHECK (tipo_cuenta IN ('Ahorros','Efectivo','Tarjeta de Crédito')),
    CONSTRAINT chk_cuentas_saldo CHECK (balance_actual >= 0),
    INDEX idx_cuentas_propietario (propietario_id)
) ENGINE = InnoDB;

CREATE TABLE usuario_cuenta (
    usuario_id      INT NOT NULL,
    cuenta_id       INT NOT NULL,
    rol             ENUM('PROPIETARIO','EDITOR','VISUALIZADOR') NOT NULL,
    fecha_invitacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_cuenta PRIMARY KEY (usuario_id, cuenta_id),
    CONSTRAINT fk_uc_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_uc_cuenta FOREIGN KEY (cuenta_id)
        REFERENCES cuentas (id) ON DELETE CASCADE,
    INDEX idx_uc_cuenta (cuenta_id)
) ENGINE = InnoDB;

CREATE TABLE transacciones (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    fecha       DATE         NOT NULL DEFAULT (CURRENT_DATE),
    monto       DECIMAL(14,2) NOT NULL,
    descripcion VARCHAR(200) NOT NULL DEFAULT '',
    cuenta_id   INT NOT NULL,
    usuario_id  INT NOT NULL,
    CONSTRAINT fk_trans_cuenta FOREIGN KEY (cuenta_id)
        REFERENCES cuentas (id) ON DELETE CASCADE,
    CONSTRAINT fk_trans_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT chk_trans_monto CHECK (monto > 0),
    INDEX idx_trans_cuenta_fecha (cuenta_id, fecha),
    INDEX idx_trans_usuario_fecha (usuario_id, fecha)
) ENGINE = InnoDB;

CREATE TABLE ingresos (
    transaccion_id INT        PRIMARY KEY,
    origen         VARCHAR(80) NOT NULL,
    CONSTRAINT fk_ingreso_transaccion FOREIGN KEY (transaccion_id)
        REFERENCES transacciones (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE gastos (
    transaccion_id INT PRIMARY KEY,
    categoria_id   INT NOT NULL,
    CONSTRAINT fk_gasto_transaccion FOREIGN KEY (transaccion_id)
        REFERENCES transacciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_gasto_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id) ON DELETE RESTRICT,
    INDEX idx_gasto_categoria (categoria_id)
) ENGINE = InnoDB;

CREATE TABLE presupuestos (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    monto_limite DECIMAL(14,2) NOT NULL,
    mes          TINYINT UNSIGNED  NOT NULL,
    anio         SMALLINT UNSIGNED NOT NULL,
    categoria_id INT NOT NULL,
    usuario_id   INT NOT NULL,
    CONSTRAINT fk_pres_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_pres_categoria FOREIGN KEY (categoria_id)
        REFERENCES categorias (id) ON DELETE CASCADE,
    CONSTRAINT chk_pres_mes    CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT chk_pres_limite CHECK (monto_limite > 0),
    CONSTRAINT uq_pres_unico UNIQUE (usuario_id, categoria_id, mes, anio),
    INDEX idx_pres_periodo (usuario_id, anio, mes)
) ENGINE = InnoDB;

CREATE OR REPLACE VIEW v_saldo_usuario AS
SELECT u.id AS usuario_id,
       u.nombre,
       COALESCE(SUM(c.balance_actual), 0) AS saldo_total
FROM usuarios u
LEFT JOIN cuentas c ON c.propietario_id = u.id
GROUP BY u.id, u.nombre;

CREATE OR REPLACE VIEW v_gasto_categoria_periodo AS
SELECT t.usuario_id,
       cat.id      AS categoria_id,
       cat.nombre  AS categoria,
       YEAR(t.fecha)  AS anio,
       MONTH(t.fecha) AS mes,
       SUM(t.monto)   AS total_gastado
FROM gastos g
JOIN transacciones t ON t.id = g.transaccion_id
JOIN categorias cat   ON cat.id = g.categoria_id
GROUP BY t.usuario_id, cat.id, cat.nombre, YEAR(t.fecha), MONTH(t.fecha);

CREATE OR REPLACE VIEW v_historial_movimientos AS
SELECT t.id, t.fecha, t.descripcion, t.monto, c.nombre AS cuenta,
       'INGRESO' AS tipo, i.origen AS detalle, NULL AS categoria
FROM ingresos i
JOIN transacciones t ON t.id = i.transaccion_id
JOIN cuentas c        ON c.id = t.cuenta_id
UNION ALL
SELECT t.id, t.fecha, t.descripcion, t.monto, c.nombre AS cuenta,
       'GASTO' AS tipo, NULL AS origen_dummy, cat.nombre AS categoria
FROM gastos g
JOIN transacciones t ON t.id = g.transaccion_id
JOIN cuentas c        ON c.id = t.cuenta_id
JOIN categorias cat   ON cat.id = g.categoria_id;

DELIMITER $$

CREATE TRIGGER trg_transacciones_fecha_limita
BEFORE INSERT ON transacciones
FOR EACH ROW
BEGIN
    IF NEW.fecha < (CURDATE() - INTERVAL 90 DAY) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'REGLA DE NEGOCIO: la fecha del movimiento no puede ser anterior a 90 dias.';
    END IF;
END$$

DELIMITER ;

INSERT INTO categorias (nombre) VALUES
    ('Alimentación'), ('Transporte'), ('Vivienda'), ('Servicios'),
    ('Salud'),        ('Educación'),  ('Entretenimiento'), ('Ropa'),
    ('Tecnología'),   ('Otros');
