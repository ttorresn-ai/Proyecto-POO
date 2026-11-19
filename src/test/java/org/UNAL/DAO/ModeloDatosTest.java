package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.modelo.Categoria;
import org.UNAL.modelo.Cuenta;
import org.UNAL.modelo.Gasto;
import org.UNAL.modelo.Ingreso;
import org.UNAL.modelo.Transaccion;
import org.UNAL.modelo.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración contra la base de datos real.
 *
 * <p>Verifican los elementos del modelo de datos: la jerarquía
 * TRANSACCION → INGRESO/GASTO, la relación N:M de cuentas compartidas, el
 * atributo multivaluado de teléfonos y la regla de negocio de los 90 días.
 *
 * <p>Si no hay base de datos configurada, las pruebas se omiten.
 */
class ModeloDatosTest {

    private UsuarioDAO usuarioDAO;
    private CuentaDAO cuentaDAO;
    private TransaccionDAO transaccionDAO;
    private CategoriaDAO categoriaDAO;

    private Usuario usuario;

    @BeforeEach
    void setUp() throws Exception {
        if (!baseDisponible()) {
            return;
        }
        usuarioDAO = new UsuarioDAO();
        cuentaDAO = new CuentaDAO();
        transaccionDAO = new TransaccionDAO();
        categoriaDAO = new CategoriaDAO();

        // Usuario único por ejecución para no chocar con datos existentes.
        String email = "it_" + System.nanoTime() + "@test.com";
        usuarioDAO.registrarUsuario(new Usuario("0", "Usuario Prueba", email, "secreto123"));
        usuario = usuarioDAO.obtenerPorEmail(email);
        assertNotNull(usuario, "No se pudo crear el usuario de prueba");
    }

    @AfterEach
    void tearDown() {
        if (usuario != null) {
            try {
                usuarioDAO.eliminarUsuario(Integer.parseInt(usuario.getId()));
            } catch (SQLException ignored) {
                // Limpieza best-effort.
            }
        }
        ConexionDB.cerrarPool();
    }

    private boolean baseDisponible() {
        try (Connection conn = ConexionDB.conectar();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private Categoria primeraCategoria() throws SQLException {
        return categoriaDAO.obtenerTodas().get(0);
    }

    // -----------------------------------------------------------------------
    //  Jerarquía
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Un ingreso se persiste en la superclase y en la subclase INGRESO")
    void ingresoPersisteEnSuperclaseYSubclase() throws Exception {
        Cuenta cuenta = crearCuenta("Hogar", 0);
        Ingreso ingreso = new Ingreso("0", 1500, LocalDate.now(), "Salario", "Trabajo");

        assertTrue(transaccionDAO.registrarTransaccion(ingreso, Integer.parseInt(cuenta.getId()),
                Integer.parseInt(usuario.getId())));

        assertEquals(1, contar("ingresos"), "Debe existir una fila en la subclase ingresos");
        assertEquals(1, contar("transacciones"), "Debe existir una fila en la superclase");

        assertEquals("Trabajo", valorEscalares(
                "SELECT i.origen FROM ingresos i"
                        + " JOIN transacciones t ON t.id = i.transaccion_id"
                        + " WHERE t.cuenta_id = ?",
                Integer.parseInt(cuenta.getId())),
                "El atributo propio de la subclase se conserva");

        // El saldo se ajusta por el monto del movimiento.
        assertEquals(1500.0, saldo(cuenta), 0.001);
    }

    @Test
    @DisplayName("Un gasto se persiste en la superclase y en la subclase GASTO con su categoría")
    void gastoPersisteEnSuperclaseYSubclase() throws Exception {
        Categoria categoria = primeraCategoria();
        Cuenta cuenta = crearCuenta("Hogar", 500);

        Gasto gasto = new Gasto("0", 120, LocalDate.now(), "Compra", categoria);
        assertTrue(transaccionDAO.registrarTransaccion(gasto, Integer.parseInt(cuenta.getId()),
                Integer.parseInt(usuario.getId())));

        assertEquals(1, contar("gastos"));
        assertEquals(0, contar("ingresos"), "Un gasto no debe crear filas en ingresos");

        assertEquals(String.valueOf(categoria.getId()), valorEscalares(
                "SELECT g.categoria_id FROM gastos g"
                        + " JOIN transacciones t ON t.id = g.transaccion_id"
                        + " WHERE t.cuenta_id = ?",
                Integer.parseInt(cuenta.getId())));

        assertEquals(380.0, saldo(cuenta), 0.001, "El saldo descueenta el gasto");
    }

    @Test
    @DisplayName("Un gasto sin categoría se rechaza antes de tocar la base")
    void gastoSinCategoriaSeRechaza() throws Exception {
        Cuenta cuenta = crearCuenta("Hogar", 500);
        Gasto gasto = new Gasto("0", 50, LocalDate.now(), "Sin categoría", null);

        assertThrows(org.UNAL.config.ValidacionException.class,
                () -> transaccionDAO.registrarTransaccion(gasto, Integer.parseInt(cuenta.getId()),
                        Integer.parseInt(usuario.getId())));
    }

    @Test
    @DisplayName("Un gasto mayor al saldo se revierte y no deja movimientos")
    void gastoSuperiorAlSaldoSeRevierte() throws Exception {
        Categoria categoria = primeraCategoria();
        Cuenta cuenta = crearCuenta("Pobre", 100);
        Gasto gasto = new Gasto("0", 9999, LocalDate.now(), "Imposible", categoria);

        assertThrows(org.UNAL.config.ValidacionException.class,
                () -> transaccionDAO.registrarTransaccion(gasto, Integer.parseInt(cuenta.getId()),
                        Integer.parseInt(usuario.getId())));

        // La transacción SQL se revierte: ni movimiento ni cambio de saldo.
        assertEquals(0, contar("transacciones"));
        assertEquals(100.0, saldo(cuenta), 0.001);
    }

    // -----------------------------------------------------------------------
    //  Reglas de negocio
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Regla de negocio: se rechaza un movimiento con más de 90 días")
    void rechazaMovimientoAntiguo() throws Exception {
        Cuenta cuenta = crearCuenta("Hogar", 0);
        LocalDate hace200Dias = LocalDate.now().minusDays(200);
        Ingreso ingreso = new Ingreso("0", 100, hace200Dias, "Antiguo", "Archivo");

        SQLException ex = assertThrows(SQLException.class,
                () -> transaccionDAO.registrarTransaccion(ingreso, Integer.parseInt(cuenta.getId()),
                        Integer.parseInt(usuario.getId())));

        assertTrue(ex.getMessage().contains("90 dias"),
                "El mensaje debe explicar la regla de los 90 días: " + ex.getMessage());
    }

    @Test
    @DisplayName("Regla de negocio: se acepta un movimiento de hace 30 días")
    void aceptaMovimientoReciente() throws Exception {
        Cuenta cuenta = crearCuenta("Hogar", 0);
        Ingreso ingreso = new Ingreso("0", 100, LocalDate.now().minusDays(30), "Reciente", "Trabajo");

        assertTrue(transaccionDAO.registrarTransaccion(ingreso, Integer.parseInt(cuenta.getId()),
                Integer.parseInt(usuario.getId())));
    }

    // -----------------------------------------------------------------------
    //  N:M e integridad
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Relación N:M: una cuenta puede compartirse con otro usuario")
    void cuentaSeComparte() throws Exception {
        Cuenta cuenta = crearCuenta("Familiar", 0);

        // Segundo usuario, simulando un miembro de la familia.
        String email = "familiar_" + System.nanoTime() + "@test.com";
        usuarioDAO.registrarUsuario(new Usuario("0", "Familiar", email, "secreto123"));
        Usuario familiar = usuarioDAO.obtenerPorEmail(email);

        try {
            assertTrue(cuentaDAO.compartirCuenta(Integer.parseInt(cuenta.getId()),
                    Integer.parseInt(familiar.getId()), "VISUALIZADOR"));

            // El propietario sigue viendo su cuenta.
            assertTrue(cuentaDAO.obtenerCuentasPorUsuario(Integer.parseInt(usuario.getId()))
                    .stream().anyMatch(c -> c.getId().equals(cuenta.getId())));

            // Y ahora el invitado también la ve: eso es la relación N:M.
            assertTrue(cuentaDAO.obtenerCuentasPorUsuario(Integer.parseInt(familiar.getId()))
                    .stream().anyMatch(c -> c.getId().equals(cuenta.getId())),
                    "El usuario compartido debe ver la cuenta");

            // El propietario queda registrado con rol PROPIETARIO en la tabla puente.
            assertEquals("PROPIETARIO", rolDe(Integer.parseInt(usuario.getId()),
                    Integer.parseInt(cuenta.getId())));

            assertTrue(cuentaDAO.revocarAcceso(Integer.parseInt(cuenta.getId()),
                    Integer.parseInt(familiar.getId())));
        } finally {
            usuarioDAO.eliminarUsuario(Integer.parseInt(familiar.getId()));
        }
    }

    @Test
    @DisplayName("Relación 1:1: un usuario no puede tener dos perfiles")
    void perfilEsUnoAUno() throws Exception {
        int id = Integer.parseInt(usuario.getId());

        assertTrue(insertarPerfil(id, "en"));
        SQLException ex = assertThrows(SQLException.class, () -> insertarPerfil(id, "es"));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate")
                        || ex.getMessage().contains("1062"),
                "Debe fallar por clave duplicada: " + ex.getMessage());
    }

    @Test
    @DisplayName("Atributo multivaluado: un usuario puede tener varios teléfonos")
    void usuarioTieneVariosTelefonos() throws Exception {
        int id = Integer.parseInt(usuario.getId());

        assertTrue(insertarTelefono(id, "3001111111"));
        assertTrue(insertarTelefono(id, "3002222222"));
        assertTrue(insertarTelefono(id, "6014444444"));

        assertEquals(3, contar("telefonos"));
    }

    // -----------------------------------------------------------------------
    //  Atributos derivados
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Atributo derivado: v_saldo_usuario suma los saldos de las cuentas")
    void vistaSaldoDerivado() throws Exception {
        crearCuenta("A", 100);
        crearCuenta("B", 250);

        Double saldo = consultaDouble(
                "SELECT saldo_total FROM v_saldo_usuario WHERE usuario_id = ?",
                Integer.parseInt(usuario.getId()));

        assertEquals(350.0, saldo, 0.001);
    }

    @Test
    @DisplayName("Atributo derivado: v_gasto_categoria_periodo agrega por categoría")
    void vistaGastoDerivado() throws Exception {
        Categoria categoria = primeraCategoria();
        Cuenta cuenta = crearCuenta("Hogar", 1000);

        transaccionDAO.registrarTransaccion(new Gasto("0", 100, LocalDate.now(), "A", categoria),
                Integer.parseInt(cuenta.getId()), Integer.parseInt(usuario.getId()));
        transaccionDAO.registrarTransaccion(new Gasto("0", 50, LocalDate.now(), "B", categoria),
                Integer.parseInt(cuenta.getId()), Integer.parseInt(usuario.getId()));

        Double total = consultaDouble(
                "SELECT total_gastado FROM v_gasto_categoria_periodo WHERE usuario_id = ? AND categoria_id = ?",
                Integer.parseInt(usuario.getId()), Integer.parseInt(categoria.getId()));

        assertEquals(150.0, total, 0.001);
    }

    @Test
    @DisplayName("Los reportes distinguen ingresos y gastos por subclase")
    void reportesDistinguenSubclases() throws Exception {
        Categoria categoria = primeraCategoria();
        Cuenta cuenta = crearCuenta("Hogar", 1000);

        transaccionDAO.registrarTransaccion(new Ingreso("0", 800, LocalDate.now(), "Sueldo", "Trabajo"),
                Integer.parseInt(cuenta.getId()), Integer.parseInt(usuario.getId()));
        transaccionDAO.registrarTransaccion(new Gasto("0", 200, LocalDate.now(), "Almo", categoria),
                Integer.parseInt(cuenta.getId()), Integer.parseInt(usuario.getId()));

        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now();

        Map<String, Double> ingresos =
                transaccionDAO.obtenerIngresosPorOrigen(Integer.parseInt(usuario.getId()), inicio, fin);
        Map<String, Double> gastos =
                transaccionDAO.obtenerGastosPorCategoria(Integer.parseInt(usuario.getId()), inicio, fin);

        assertEquals(800.0, ingresos.getOrDefault("Trabajo", 0.0), 0.001);
        assertEquals(200.0, gastos.getOrDefault(categoria.getNombre(), 0.0), 0.001);

        double[] totales = transaccionDAO.obtenerTotalesPeriodo(
                Integer.parseInt(usuario.getId()), inicio, fin);
        assertEquals(800.0, totales[0], 0.001);
        assertEquals(200.0, totales[1], 0.001);
    }

    // -----------------------------------------------------------------------
    //  Utilidades
    // -----------------------------------------------------------------------

    private Cuenta crearCuenta(String nombre, double saldo) throws Exception {
        CuentaDAO dao = new CuentaDAO();
        Cuenta cuenta = new Cuenta("0", nombre, "Ahorros", saldo);
        assertTrue(dao.crearCuenta(cuenta, Integer.parseInt(usuario.getId())));
        assertFalse("0".equals(cuenta.getId()), "La cuenta debe recibir su id generado");
        return cuenta;
    }

    /**
     * Cuenta las filas de una tabla que pertenecen al usuario de esta prueba.
     *
     * <p>El ambito es deliberado. {@link #setUp()} crea un usuario distinto en
     * cada ejecucion precisamente para aislar la prueba, de modo que contar la
     * tabla entera haria que el resultado dependiera de lo que hubiera en la
     * base: el catalogo de demostracion o cualquier otra fila ajena inflaria
     * el conteo y la prueba fallaria sin que el codigo bajo prueba estuviera
     * roto. Las subclases se alcanzan a traves de la superclase, que es la que
     * guarda el {@code usuario_id}.
     */
    private int contar(String tabla) throws SQLException {
        String sql;
        switch (tabla) {
            case "transacciones":
            case "telefonos":
                sql = "SELECT COUNT(*) FROM " + tabla + " WHERE usuario_id = ?";
                break;
            case "ingresos":
                sql = "SELECT COUNT(*) FROM ingresos i"
                        + " JOIN transacciones t ON t.id = i.transaccion_id"
                        + " WHERE t.usuario_id = ?";
                break;
            case "gastos":
                sql = "SELECT COUNT(*) FROM gastos g"
                        + " JOIN transacciones t ON t.id = g.transaccion_id"
                        + " WHERE t.usuario_id = ?";
                break;
            default:
                throw new IllegalArgumentException("Tabla no contemplada por contar(): " + tabla);
        }
        return consultaInt(sql, Integer.parseInt(usuario.getId()));
    }

    private int consultaInt(String sql, int... params) throws SQLException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Devuelve el primer valor de la primera fila, o {@code null} si no hay
     * ninguna. La consulta debe acotarse a la cuenta o al usuario de la prueba:
     * este metodo no anade ningun filtro por su cuenta.
     */
    private String valorEscalares(String sql, int... params) throws SQLException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? String.valueOf(rs.getObject(1)) : null;
            }
        }
    }

    private Double consultaDouble(String sql, int... params) throws SQLException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    private double saldo(Cuenta cuenta) throws SQLException {
        return consultaDouble("SELECT balance_actual FROM cuentas WHERE id = ?",
                Integer.parseInt(cuenta.getId()));
    }

    private String rolDe(int usuarioId, int cuentaId) throws SQLException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT rol FROM usuario_cuenta WHERE usuario_id = ? AND cuenta_id = ?")) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, cuentaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private boolean insertarPerfil(int usuarioId, String idioma) throws SQLException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO perfiles (usuario_id, idioma_preferido) VALUES (?, ?)")) {

            pstmt.setInt(1, usuarioId);
            pstmt.setString(2, idioma);
            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean insertarTelefono(int usuarioId, String numero) throws SQLException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO telefonos (usuario_id, numero) VALUES (?, ?)")) {

            pstmt.setInt(1, usuarioId);
            pstmt.setString(2, numero);
            return pstmt.executeUpdate() > 0;
        }
    }
}