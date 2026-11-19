package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Gasto;
import org.UNAL.modelo.Ingreso;
import org.UNAL.modelo.Transaccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la jerarquía TRANSACCION → INGRESO / GASTO.
 *
 * <p>La superclase guarda los atributos comunes (fecha, monto, descripción,
 * cuenta). Cada subclase guarda sus atributos propios: {@code ingresos.origen}
 * y {@code gastos.categoria_id}. Un movimiento se persiste en la superclase y
 * en su subclase dentro de la misma transacción, junto con el ajuste del saldo
 * de la cuenta.
 */
public class TransaccionDAO {

    private static final String SQL_INSERT_SUPER =
            "INSERT INTO transacciones (fecha, monto, descripcion, cuenta_id, usuario_id) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_INGRESO =
            "INSERT INTO ingresos (transaccion_id, origen) VALUES (?, ?)";

    private static final String SQL_INSERT_GASTO =
            "INSERT INTO gastos (transaccion_id, categoria_id) VALUES (?, ?)";

    private static final String SQL_SUMAR =
            "UPDATE cuentas SET balance_actual = balance_actual + ? WHERE id = ?";
    private static final String SQL_RESTAR =
            "UPDATE cuentas SET balance_actual = balance_actual - ? WHERE id = ? AND balance_actual >= ?";

    /**
     * Registra un ingreso o gasto: inserta en la superclase, en la subclase
     * correspondiente y ajusta el balance de la cuenta.
     *
     * @throws ValidacionException si los datos no son coherentes o el saldo no alcanza.
     */
    public boolean registrarTransaccion(Transaccion t, int cuentaId, int usuarioId)
            throws SQLException, ValidacionException {

        if (t == null) {
            throw new ValidacionException("Transacción nula.");
        }
        Validador.validarMonto(t.getMonto());

        boolean esGasto;
        String origen = null;
        Integer categoriaId = null;

        if (t instanceof Ingreso ingreso) {
            esGasto = false;
            origen = ingreso.getOrigen() == null || ingreso.getOrigen().isBlank()
                    ? "No especificado"
                    : ingreso.getOrigen().trim();
        } else if (t instanceof Gasto gasto) {
            esGasto = true;
            if (gasto.getCategoria() == null) {
                throw new ValidacionException("Debes seleccionar una categoría para el gasto.");
            }
            categoriaId = Integer.parseInt(gasto.getCategoria().getId());
        } else {
            throw new ValidacionException("Tipo de transacción no soportado.");
        }

        String descripcion = t.getDescripcion() == null ? "" : t.getDescripcion().trim();
        // El trigger de negocio valida que la fecha no supere los 90 días.
        LocalDate fecha = t.getFecha() == null ? LocalDate.now() : t.getFecha();

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                int transaccionId = insertarSuperclase(conn, fecha, t.getMonto(), descripcion, cuentaId, usuarioId);

                if (esGasto) {
                    try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_GASTO)) {
                        pstmt.setInt(1, transaccionId);
                        pstmt.setInt(2, categoriaId);
                        pstmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_INGRESO)) {
                        pstmt.setInt(1, transaccionId);
                        pstmt.setString(2, origen);
                        pstmt.executeUpdate();
                    }
                }

                if (!ajustarSaldo(conn, cuentaId, t.getMonto(), esGasto)) {
                    conn.rollback();
                    throw new ValidacionException("Fondos insuficientes en la cuenta seleccionada.");
                }

                conn.commit();
                return true;
            } catch (SQLException | ValidacionException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int insertarSuperclase(Connection conn, LocalDate fecha, double monto,
                                   String descripcion, int cuentaId, int usuarioId)
            throws SQLException {

        try (PreparedStatement pstmt = conn.prepareStatement(
                SQL_INSERT_SUPER, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setDate(1, java.sql.Date.valueOf(fecha));
            pstmt.setDouble(2, monto);
            pstmt.setString(3, descripcion);
            pstmt.setInt(4, cuentaId);
            pstmt.setInt(5, usuarioId);
            pstmt.executeUpdate();

            try (ResultSet claves = pstmt.getGeneratedKeys()) {
                return claves.next() ? claves.getInt(1) : 0;
            }
        }
    }

    /**
     * Aplica el signo correspondiente al balance.
     *
     * @return false si un gasto excede el saldo disponible.
     */
    private boolean ajustarSaldo(Connection conn, int cuentaId, double monto, boolean esGasto)
            throws SQLException {

        if (esGasto) {
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_RESTAR)) {
                pstmt.setDouble(1, monto);
                pstmt.setInt(2, cuentaId);
                pstmt.setDouble(3, monto);
                return pstmt.executeUpdate() > 0;
            }
        }
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_SUMAR)) {
            pstmt.setDouble(1, monto);
            pstmt.setInt(2, cuentaId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /** Elimina un movimiento y revierte su efecto sobre el balance. */
    public boolean eliminarTransaccion(int transaccionId, int cuentaId, double monto, String tipo)
            throws SQLException {

        String sqlAjuste = "GASTO".equalsIgnoreCase(tipo) ? SQL_SUMAR : SQL_RESTAR;

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtDelete =
                         conn.prepareStatement("DELETE FROM transacciones WHERE id = ?")) {

                if (pstmtDelete.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                boolean ok;
                if (sqlAjuste == SQL_SUMAR) {
                    ok = ajustarSaldo(conn, cuentaId, monto, false);
                } else {
                    ok = ajustarSaldo(conn, cuentaId, monto, true);
                }

                if (!ok) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Suma de gastos agrupada por categoría dentro de un rango de fechas. */
    public Map<String, Double> obtenerGastosPorCategoria(int usuarioId, LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {

        Map<String, Double> reporte = new LinkedHashMap<>();
        if (rangoInvalido(fechaInicio, fechaFin)) {
            return reporte;
        }

        String sql = "SELECT cat.nombre AS categoria, SUM(t.monto) AS total_gastado "
                + "FROM gastos g "
                + "JOIN transacciones t ON t.id = g.transaccion_id "
                + "JOIN categorias cat   ON cat.id = g.categoria_id "
                + "WHERE t.usuario_id = ? "
                + "AND t.fecha BETWEEN ? AND ? "
                + "GROUP BY cat.nombre "
                + "ORDER BY total_gastado DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setDate(2, java.sql.Date.valueOf(fechaInicio));
            pstmt.setDate(3, java.sql.Date.valueOf(fechaFin.plusDays(1)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reporte.put(rs.getString("categoria"), rs.getDouble("total_gastado"));
                }
            }
        }
        return reporte;
    }

    /** Ingresos agrupados por origen dentro de un rango de fechas. */
    public Map<String, Double> obtenerIngresosPorOrigen(int usuarioId, LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {

        Map<String, Double> reporte = new LinkedHashMap<>();
        if (rangoInvalido(fechaInicio, fechaFin)) {
            return reporte;
        }

        String sql = "SELECT i.origen AS origen, SUM(t.monto) AS total "
                + "FROM ingresos i "
                + "JOIN transacciones t ON t.id = i.transaccion_id "
                + "WHERE t.usuario_id = ? "
                + "AND t.fecha BETWEEN ? AND ? "
                + "GROUP BY i.origen "
                + "ORDER BY total DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setDate(2, java.sql.Date.valueOf(fechaInicio));
            pstmt.setDate(3, java.sql.Date.valueOf(fechaFin.plusDays(1)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reporte.put(rs.getString("origen"), rs.getDouble("total"));
                }
            }
        }
        return reporte;
    }

    /** Totales de ingresos y gastos del usuario en un rango de fechas. */
    public double[] obtenerTotalesPeriodo(int usuarioId, LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {

        if (rangoInvalido(fechaInicio, fechaFin)) {
            return new double[]{0, 0};
        }

        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN i.transaccion_id IS NOT NULL THEN t.monto END), 0), "
                + "COALESCE(SUM(CASE WHEN g.transaccion_id IS NOT NULL THEN t.monto END), 0) "
                + "FROM transacciones t "
                + "LEFT JOIN ingresos i ON i.transaccion_id = t.id "
                + "LEFT JOIN gastos g   ON g.transaccion_id = t.id "
                + "WHERE t.usuario_id = ? AND t.fecha BETWEEN ? AND ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setDate(2, java.sql.Date.valueOf(fechaInicio));
            pstmt.setDate(3, java.sql.Date.valueOf(fechaFin.plusDays(1)));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? new double[]{rs.getDouble(1), rs.getDouble(2)} : new double[]{0, 0};
            }
        }
    }

    /**
     * Historial paginado. LEFT JOIN con las dos subclases permite distinguir
     * el tipo de movimiento y traer su atributo propio.
     */
    public List<Map<String, Object>> obtenerTransaccionesPaginadas(int usuarioId, int limit, int offset)
            throws SQLException {

        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT t.id, t.monto, t.descripcion, t.fecha, c.nombre AS cuenta, "
                + "CASE WHEN i.transaccion_id IS NOT NULL THEN 'INGRESO' ELSE 'GASTO' END AS tipo, "
                + "i.origen AS origen, cat.nombre AS categoria "
                + "FROM transacciones t "
                + "JOIN cuentas c        ON c.id = t.cuenta_id "
                + "LEFT JOIN ingresos i   ON i.transaccion_id = t.id "
                + "LEFT JOIN gastos g     ON g.transaccion_id = t.id "
                + "LEFT JOIN categorias cat ON cat.id = g.categoria_id "
                + "WHERE t.usuario_id = ? "
                + "ORDER BY t.fecha DESC, t.id DESC "
                + "LIMIT ? OFFSET ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", rs.getInt("id"));
                    fila.put("monto", rs.getDouble("monto"));
                    fila.put("descripcion", rs.getString("descripcion"));
                    fila.put("fecha", rs.getDate("fecha"));
                    fila.put("cuenta", rs.getString("cuenta"));
                    fila.put("tipo", rs.getString("tipo"));
                    fila.put("origen", rs.getString("origen"));
                    fila.put("categoria", rs.getString("categoria"));
                    lista.add(fila);
                }
            }
        }
        return lista;
    }

    private boolean rangoInvalido(LocalDate inicio, LocalDate fin) {
        return inicio == null || fin == null || inicio.isAfter(fin);
    }
}