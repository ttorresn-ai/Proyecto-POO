package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.modelo.Gasto;
import org.UNAL.modelo.Ingreso;
import org.UNAL.modelo.Transaccion;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TransaccionDAO {

    public boolean registrarTransaccion(Transaccion t, int cuentaId, int usuarioId) {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);

            try {
                java.sql.Date fechaSql = new java.sql.Date(t.getFecha().getTime());

                if (t instanceof Ingreso) {
                    String sql = "INSERT INTO ingresos (cuenta_id, usuario_id, monto, origen, descripcion, fecha) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, cuentaId);
                        pstmt.setInt(2, usuarioId);
                        pstmt.setDouble(3, t.getMonto());
                        pstmt.setString(4, ((Ingreso) t).getOrigen());
                        pstmt.setString(5, t.getDescripcion());
                        pstmt.setDate(6, fechaSql);
                        pstmt.executeUpdate();
                    }

                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(
                            "UPDATE cuentas SET balance_actual = balance_actual + ? WHERE id = ?")) {
                        pstmtUpdate.setDouble(1, t.getMonto());
                        pstmtUpdate.setInt(2, cuentaId);
                        pstmtUpdate.executeUpdate();
                    }

                } else if (t instanceof Gasto) {
                    String sql = "INSERT INTO gastos (cuenta_id, usuario_id, categoria_id, monto, descripcion, fecha) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, cuentaId);
                        pstmt.setInt(2, usuarioId);
                        pstmt.setInt(3, Integer.parseInt(((Gasto) t).getCategoria().getId()));
                        pstmt.setDouble(4, t.getMonto());
                        pstmt.setString(5, t.getDescripcion());
                        pstmt.setDate(6, fechaSql);
                        pstmt.executeUpdate();
                    }

                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(
                            "UPDATE cuentas SET balance_actual = balance_actual - ? WHERE id = ?")) {
                        pstmtUpdate.setDouble(1, t.getMonto());
                        pstmtUpdate.setInt(2, cuentaId);
                        pstmtUpdate.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Error en la transacción: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Double> obtenerGastosPorCategoria(int usuarioId, LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, Double> reporteDatos = new HashMap<>();

        String sql = "SELECT cat.nombre AS categoria, SUM(g.monto) AS total_gastado " +
                "FROM gastos g " +
                "JOIN categorias cat ON g.categoria_id = cat.id " +
                "WHERE g.usuario_id = ? " +
                "AND g.fecha >= ? AND g.fecha <= ? " +
                "GROUP BY cat.nombre";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setDate(2, java.sql.Date.valueOf(fechaInicio));
            pstmt.setDate(3, java.sql.Date.valueOf(fechaFin));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reporteDatos.put(rs.getString("categoria"), rs.getDouble("total_gastado"));
            }

        } catch (SQLException e) {
            System.err.println("Error al generar reporte de categorías: " + e.getMessage());
        }

        return reporteDatos;
    }

    public boolean eliminarTransaccion(int transaccionId, int cuentaId, double monto, String tipoTransaccion) {
        String tabla = tipoTransaccion.equals("GASTO") ? "gastos" : "ingresos";
        String sqlDelete = "DELETE FROM " + tabla + " WHERE id = ?";
        String sqlUpdateCuenta = tipoTransaccion.equals("GASTO") ?
                "UPDATE cuentas SET balance_actual = balance_actual + ? WHERE id = ?" :
                "UPDATE cuentas SET balance_actual = balance_actual - ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDelete);
                 PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateCuenta)) {

                pstmtDelete.setInt(1, transaccionId);
                int rowsDeleted = pstmtDelete.executeUpdate();

                pstmtUpdate.setDouble(1, monto);
                pstmtUpdate.setInt(2, cuentaId);
                int rowsUpdated = pstmtUpdate.executeUpdate();

                if (rowsDeleted > 0 && rowsUpdated > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Map<String, Object>> obtenerTransaccionesPaginadas(int usuarioId, int limit, int offset) {
        List<Map<String, Object>> lista = new ArrayList<>();

        String sql = "(SELECT id, monto, descripcion, 'INGRESO' AS tipo_transaccion, fecha FROM ingresos WHERE usuario_id = ?) " +
                "UNION ALL " +
                "(SELECT id, monto, descripcion, 'GASTO' AS tipo_transaccion, fecha FROM gastos WHERE usuario_id = ?) " +
                "ORDER BY fecha DESC LIMIT ? OFFSET ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, usuarioId);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id", rs.getInt("id"));
                fila.put("monto", rs.getDouble("monto"));
                fila.put("descripcion", rs.getString("descripcion"));
                fila.put("tipo_transaccion", rs.getString("tipo_transaccion"));
                fila.put("fecha", rs.getDate("fecha"));
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener transacciones: " + e.getMessage());
        }
        return lista;
    }
}
