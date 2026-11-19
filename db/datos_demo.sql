-- ============================================================================
--  NEXUS FINANCE - CATALOGO DE DEMOSTRACION
--
--  db/esquema.sql crea la estructura y solo siembra la tabla `categorias`.
--  Este archivo agrega un usuario de prueba con sus cuentas, movimientos y
--  presupuesto, de modo que la aplicacion se vea con informacion real y sea
--  posible ejecutar los INSERT que demuestran el disparador de 90 dias.
--
--  Usuario de prueba
--      correo    : ana.torres@unal.edu.co
--      contrasena: Nexus2026!
--
--  El hash sigue el formato que produce UsuarioDAO:
--      pbkdf2$<iteraciones>$<saltBase64>$<hashBase64>
--      PBKDF2WithHmacSHA256, 120000 iteraciones, salt de 16 bytes.
--
--  Es idempotente: se puede ejecutar mas de una vez sin duplicar filas.
-- ============================================================================

USE nexus_finance;

-- ---------------------------------------------------------------------------
--  Usuario de demostracion
-- ---------------------------------------------------------------------------
INSERT INTO usuarios (nombre, email, password) VALUES
  ('Ana Torres', 'ana.torres@unal.edu.co',
   'pbkdf2$120000$+77BCpTjGrmLdwAzFmbS9Q==$Ey+/mUG8Uo4Y/f1TgZ1tjFPSHwS/GZ0izXdgoPRuVs0=')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

SET @uid = (SELECT id FROM usuarios WHERE email = 'ana.torres@unal.edu.co');

-- ---------------------------------------------------------------------------
--  Entidad debil 1:1  (clave primaria = clave foranea)
-- ---------------------------------------------------------------------------
INSERT INTO perfiles (usuario_id, biografia, idioma_preferido, fecha_nacimiento)
VALUES (@uid, 'Estudiante de Ingenieria de Sistemas. Uso Nexus Finance para llevar
el control de mis gastos mensuales.', 'es', '2002-04-17')
ON DUPLICATE KEY UPDATE biografia = VALUES(biografia);

-- ---------------------------------------------------------------------------
--  Atributo multivaluado  (1:N -> tabla telefonos)
-- ---------------------------------------------------------------------------
INSERT INTO telefonos (usuario_id, numero, tipo, es_principal) VALUES
  (@uid, '3005551122', 'MOVIL', 1),
  (@uid, '6045553344', 'TRABAJO', 0)
ON DUPLICATE KEY UPDATE es_principal = VALUES(es_principal);

-- ---------------------------------------------------------------------------
--  Cuentas  (1:N desde usuarios)
-- ---------------------------------------------------------------------------
--  Los valores de tipo_cuenta estan restringidos por el CHECK chk_cuentas_tipo
--  a 'Ahorros', 'Efectivo' y 'Tarjeta de Crédito'.
INSERT INTO cuentas (nombre, tipo_cuenta, balance_actual, propietario_id) VALUES
  ('Cuenta corriente',   'Efectivo', 2450000.00, @uid),
  ('Fondo de emergencia','Ahorros',   800000.00, @uid)
ON DUPLICATE KEY UPDATE balance_actual = VALUES(balance_actual);

SET @c1 = (SELECT id FROM cuentas WHERE nombre = 'Cuenta corriente'    AND propietario_id = @uid);
SET @c2 = (SELECT id FROM cuentas WHERE nombre = 'Fondo de emergencia' AND propietario_id = @uid);

-- ---------------------------------------------------------------------------
--  Relacion N:M  (usuario_cuenta con PK compuesta)
-- ---------------------------------------------------------------------------
INSERT INTO usuario_cuenta (usuario_id, cuenta_id, rol) VALUES
  (@uid, @c1, 'PROPIETARIO'),
  (@uid, @c2, 'PROPIETARIO')
ON DUPLICATE KEY UPDATE rol = VALUES(rol);

-- ---------------------------------------------------------------------------
--  Movimientos: jerarquia tabla por subclase.
--  Se insertan primero en `transacciones` (superclase) y luego en `ingresos`
--  o `gastos`, cuya clave primaria es la misma clave foranea.
--
--  Las fechas son relativas a CURDATE() para que la regla de los 90 dias
--  nunca se dispare por accidente al reejecutar este archivo dentro de un mes.
-- ---------------------------------------------------------------------------
INSERT INTO transacciones (fecha, monto, descripcion, cuenta_id, usuario_id) VALUES
  (CURDATE() - INTERVAL  2 DAY, 2500000.00, 'Nomina de practicas',        @c1, @uid),
  (CURDATE() - INTERVAL  5 DAY,  450000.00, 'Venta de apuntes usados',     @c1, @uid),
  (CURDATE() - INTERVAL  7 DAY,  180000.00, 'Almuerzo cafeteria',          @c1, @uid),
  (CURDATE() - INTERVAL 10 DAY,  320000.00, 'Transporte mensual',         @c1, @uid),
  (CURDATE() - INTERVAL 12 DAY,  960000.00, 'Compra de libro de algoritmos',@c1, @uid),
  (CURDATE() - INTERVAL 15 DAY,  150000.00, 'Cafe con companeros',         @c1, @uid),
  (CURDATE() - INTERVAL 20 DAY,  210000.00, 'Material de escritorio',      @c1, @uid);

--  Las subclases cuelgan de la superclase por clave primaria compartida:
--  `transaccion_id` es a la vez PK de la subclase y FK hacia transacciones.id.
--  Por eso el id se localiza por la descripcion del movimiento.
INSERT INTO ingresos (transaccion_id, origen) VALUES
  ((SELECT id FROM transacciones WHERE descripcion = 'Nomina de practicas'
    AND usuario_id = @uid), 'Beca universitaria'),
  ((SELECT id FROM transacciones WHERE descripcion = 'Venta de apuntes usados'
    AND usuario_id = @uid), 'Venta de apuntes usados');

INSERT INTO gastos (transaccion_id, categoria_id) VALUES
  ((SELECT id FROM transacciones WHERE descripcion = 'Almuerzo cafeteria'
    AND usuario_id = @uid), (SELECT id FROM categorias WHERE nombre = 'Alimentación')),
  ((SELECT id FROM transacciones WHERE descripcion = 'Transporte mensual'
    AND usuario_id = @uid), (SELECT id FROM categorias WHERE nombre = 'Transporte')),
  ((SELECT id FROM transacciones WHERE descripcion = 'Compra de libro de algoritmos'
    AND usuario_id = @uid), (SELECT id FROM categorias WHERE nombre = 'Educación')),
  ((SELECT id FROM transacciones WHERE descripcion = 'Cafe con companeros'
    AND usuario_id = @uid), (SELECT id FROM categorias WHERE nombre = 'Alimentación')),
  ((SELECT id FROM transacciones WHERE descripcion = 'Material de escritorio'
    AND usuario_id = @uid), (SELECT id FROM categorias WHERE nombre = 'Otros'));

-- ---------------------------------------------------------------------------
--  Presupuestos
-- ---------------------------------------------------------------------------
INSERT INTO presupuestos (monto_limite, mes, anio, categoria_id, usuario_id)
SELECT 400000.00, MONTH(CURDATE()), YEAR(CURDATE()),
       (SELECT id FROM categorias WHERE nombre = 'Alimentación'), @uid
WHERE NOT EXISTS (
  SELECT 1 FROM presupuestos
  WHERE usuario_id = @uid
    AND anio = YEAR(CURDATE())
    AND mes = MONTH(CURDATE())
);

-- ============================================================================
--  Verificacion
-- ============================================================================
SELECT 'usuarios'      AS tabla, COUNT(*) AS filas FROM usuarios
UNION ALL SELECT 'perfiles',     COUNT(*) FROM perfiles
UNION ALL SELECT 'telefonos',    COUNT(*) FROM telefonos
UNION ALL SELECT 'cuentas',      COUNT(*) FROM cuentas
UNION ALL SELECT 'usuario_cuenta',COUNT(*) FROM usuario_cuenta
UNION ALL SELECT 'transacciones',COUNT(*) FROM transacciones
UNION ALL SELECT 'ingresos',     COUNT(*) FROM ingresos
UNION ALL SELECT 'gastos',       COUNT(*) FROM gastos
UNION ALL SELECT 'presupuestos', COUNT(*) FROM presupuestos;
