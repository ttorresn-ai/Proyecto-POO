# NEXUS FINANCE // CORE_SYSTEM

Gestor de finanzas personales en **Java 21 + JavaFX 21 + MySQL 8**, con
arquitectura por capas (modelo / DAO / vistas).

---

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 21 |
| Maven | 3.9+ |
| MySQL | 8.0+ (usa `CHECK` constraints y `DEFAULT (CURRENT_DATE)`) |

---

## Puesta en marcha

### 1. Crear la base de datos

```bash
mysql -u root -p < db/esquema.sql
```

El script crea la base `nexus_finance` con 10 tablas, 3 vistas y 1 trigger, e
inserta el catálogo inicial de 10 categorías.

> El script hace `DROP DATABASE`. Para recrear todo, ejecuta de nuevo el
> esquema; para vaciar solo los datos de prueba, borra las filas de `usuarios`
> (las demás tablas caen en cascada).

### Documentos del modelo de datos

| Archivo | Contenido |
|---|---|
| `db/esquema.sql` | Implementación: tablas, claves, restricciones, vistas y trigger |
| `db/diagrama_er_chen.sql` | Diagrama ER en notación de Chen, con la matriz de requisitos |
| `db/diagrama_er.drawio` | El mismo diagrama en archivo importable de diagrams.net |
| `db/guion_exposicion.md` | Guion cronometrado para la presentación de 5 minutos |

### 2. Configurar las credenciales

```bash
copy db\db.properties.example src\main\resources\db.properties
```

Luego edita `db.url`, `db.user` y `db.password`. Ese archivo está en
`.gitignore` para que las credenciales nunca se suban al repositorio.

La configuración se resuelve en este orden:

1. Propiedades del sistema: `-Ddb.url=... -Ddb.user=... -Ddb.password=...`
2. Variable de entorno `DB_PROPERTIES` apuntando a un archivo externo
3. `src/main/resources/db.properties` en el classpath

### 3. Ejecutar

```bash
mvn clean javafx:run
```

### 4. Pruebas

```bash
mvn test
```

---

## Estructura

```
src/main/java/org/UNAL/
├── FinanzasApp.java          Application + orquestador de navegación
├── config/
│   ├── ConexionDB.java       Pool de conexión (propiedades + connection factory)
│   ├── Validador.java        Validaciones de entrada y parseo de montos
│   └── ValidacionException.java
├── modelo/
│   ├── Usuario.java
│   ├── Cuenta.java
│   ├── Transaccion.java      Clase abstracta
│   ├── Ingreso.java          hereda de Transaccion
│   ├── Gasto.java            hereda de Transaccion
│   ├── Categoria.java
│   └── Presupuesto.java
├── DAO/
│   ├── UsuarioDAO.java       PBKDF2 + salt; CRUD de usuarios
│   ├── CuentaDAO.java        CRUD + transferencias transaccionales
│   ├── TransaccionDAO.java   Registra movimientos y ajusta saldos atómicamente
│   ├── CategoriaDAO.java     Catálogo; incluye carga masiva (evita N+1)
│   └── PresupuestoDAO.java   Límites por categoría/mes + gasto real
└── views/
    ├── SplashView.java       Animación de arranque
    ├── BienvenidaView.java   Menú principal con video de fondo
    ├── LoginView.java        Acceso (campo de clave ofuscado)
    ├── RegisterView.java     Registro de usuarios
    ├── DashboardView.java    Menú autenticado
    ├── CyberTheme.java       Paleta y estilos compartidos
    ├── ModuloAutenticado.java  Clase base de los módulos
    ├── CuentaView.java       Alta/listado/baja de cuentas
    ├── TransaccionView.java  Registro de ingresos y gastos
    ├── PresupuestoView.java  Límites con barra de progreso
    ├── ReporteView.java      Gráficas de gastos e ingresos
    └── CyberAlert.java       Diálogos de error, éxito y confirmación
```

Los estilos tienen dos capas: `CyberTheme` genera las propiedades `-fx-*`
directas de cada nodo, y `styles/styles.css` aporta los selectores que JavaFX
no interpreta dentro de `setStyle()` (scrollbars, encabezados de tabla,
gráficas). `ModuloAutenticado` carga la hoja en cada pantalla autenticada.

---

## Modelo de datos

```
                     ┌──────────────┐
                     │  USUARIOS   │──1:1──┌──────────┐
                     └──────┬───────┘        │ PERFILES │
                            │ 1:N            └──────────┘
              ┌─────────────┼──────────────┐
              │             │              │ (MV) 1:N
        ┌─────┴─────┐  ┌────┴─────┐   ┌────┴──────┐
        │  CUENTAS  │  │PRESUPUESTOS│  │ TELEFONOS │
        └─────┬─────┘  └────┬─────┘   └───────────┘
              │             │
              │ N:M         │ N:1
        ┌─────┴──────┐      │
        │USUARIO_CUENTA│   ┌──┴──────────┐
        └────────────┘    │ CATEGORIAS  │
                          └──────▲──────┘
                                 │ N:1
        ┌──────────────┐         │
        │ TRANSACCIONES│─────────┘
        └──────┬───────┘
               │ (superclase)
        ┌──────┴──────┐
        │             │
   ┌────┴─────┐  ┌────┴────┐
   │ INGRESO  │  │  GASTO  │
   └──────────┘  └─────────┘
   (subclase)    (subclase)
```

### Elementos de modelado

| Requisito | Implementación |
|---|---|
| Entidades | 10 tablas |
| Relación 1:N | `USUARIOS` → `CUENTAS`, `USUARIOS` → `PRESUPUESTOS` |
| Relación N:M | `USUARIO_CUENTA` (cuentas compartidas, con rol) |
| Relación 1:1 | `USUARIOS` ↔ `PERFILES` (PK = FK) |
| Atributo multivaluado | `TELEFONOS` (doble óvalo en el ER) |
| Atributo derivado | Vistas `v_saldo_usuario`, `v_gasto_categoria_periodo` |
| Jerarquía | `TRANSACCIONES` → `INGRESOS`, `GASTOS` |
| Regla de negocio | Trigger `trg_transacciones_fecha_limita` (90 días) |

### Integridad

- `usuarios.email` es único
- Las tablas caen en cascada al borrar el usuario o la cuenta
- Cada subclase usa la PK de la superclase como PK propia: cardinalidad 0..1
  garantizada (un movimiento es un ingreso o un gasto, nunca ambos)
- Los montos son `DECIMAL(14,2)` para evitar errores de punto flotante
- `CHECK` impide montos no positivos, meses inválidos y sobregiro

---

## Seguridad

Las contraseñas se almacenan con **PBKDF2-HMAC-SHA256**, 120 000 iteraciones y un
salt aleatorio de 16 bytes por usuario:

```
pbkdf2$120000$<saltBase64>$<hashBase64>
```

La comparación se hace en tiempo constante (`MessageDigest.isEqual`) para
resistir ataques de temporización. Los usuarios creados con el esquema anterior
(hash SHA-256 plano) todavía pueden iniciar sesión y se migran al siguiente login.

---

## Notas de implementación

- **Pool de conexiones**: `ConexionDB` mantiene hasta 8 conexiones abiertas y las
  recicla. Abrir un canal TCP con MySQL cuesta entre 30 y 80 ms, así que sin pool
  cada clic pagaba ese coste. Al devolverse al pool se restaura el estado
  (rollback de transacciones pendientes y `autoCommit=true`) para que una
  conexión sucia no contamine la siguiente petición.
- **Transacciones**: registrar un movimiento y ajustar el saldo de la cuenta
  viajan en la misma transacción SQL. Si el saldo es insuficiente, se revierte
  con `rollback`.
- **Sin N+1**: `PresupuestoView` carga en dos consultas todo lo que la tabla
  necesita (nombres de categoría y gasto de cada presupuesto) en lugar de una
  consulta por fila y repintado.
- **Parseo de montos**: `Validador.parsearMonto` acepta `1500.50`, `1500,50` y
  `1.500.000,00` sin depender del locale del sistema.
- **Multimedia**: los `MediaPlayer` se liberan con `dispose()` antes de cambiar
  de pantalla para no dejar decodificadores nativos consumiendo CPU. Las
  animaciones (`Timeline`) también se detienen: si siguen corriendo retienen los
  nodos de la escena anterior.
- **Animación de login**: el motor de glitch del campo de clave solo corre
  mientras el campo tiene foco, en vez de un `Timeline` infinito desde el arranque.
- **Navegación**: las vistas no conocen el `Stage`. `FinanzasApp` sustituye la
  `Scene` y cada módulo recibe un callback para volver.

### Ajustes pendientes

- El `jar-with-dependencies` incluye las librerías nativas de JavaFX, que no
  funcionan bien empaquetadas. Para distribuir, conviene un `jpackage` nativo.