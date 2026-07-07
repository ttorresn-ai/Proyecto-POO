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

    public boolean registrarTransaccion(Transaccion t, int cuentaId) {
        String sql = "INSERT INTO transacciones (monto, descripcion, tipo_transaccion, origen, categoria_id, cuenta_id) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlUpdateCuenta = "";

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, t.getMonto());
                pstmt.setString(2, t.getDescripcion());

                if (t instanceof Ingreso) {
                    pstmt.setString(3, "INGRESO");
                    pstmt.setString(4, ((Ingreso) t).getOrigen());
                    pstmt.setNull(5, Types.INTEGER);
                    sqlUpdateCuenta = "UPDATE cuentas SET balance_actual = balance_actual + ? WHERE id = ?";
                } else if (t instanceof Gasto) {
                    pstmt.setString(3, "GASTO");
                    pstmt.setNull(4, Types.VARCHAR);
                    pstmt.setInt(5, Integer.parseInt(((Gasto) t).getCategoria().getId()));
                    sqlUpdateCuenta = "UPDATE cuentas SET balance_actual = balance_actual - ? WHERE id = ?";
                }

                pstmt.setInt(6, cuentaId);
                pstmt.executeUpdate();

                try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateCuenta)) {
                    pstmtUpdate.setDouble(1, t.getMonto());
                    pstmtUpdate.setInt(2, cuentaId);
                    pstmtUpdate.executeUpdate();
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

        String sql = "SELECT cat.nombre AS categoria, SUM(t.monto) AS total_gastado " +
                "FROM transacciones t " +
                "JOIN cuentas c ON t.cuenta_id = c.id " +
                "JOIN categorias cat ON t.categoria_id = cat.id " +
                "WHERE c.usuario_id = ? " +
                "AND t.tipo_transaccion = 'GASTO' " +
                "AND t.fecha >= ? AND t.fecha <= ? " +
                "GROUP BY cat.nombre";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setDate(2, java.sql.Date.valueOf(fechaInicio));
            pstmt.setDate(3, java.sql.Date.valueOf(fechaFin));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String nombreCategoria = rs.getString("categoria");
                double totalGastado = rs.getDouble("total_gastado");

                reporteDatos.put(nombreCategoria, totalGastado);
            }

        } catch (SQLException e) {
            System.err.println("Error al generar reporte de categorías: " + e.getMessage());
        }

        return reporteDatos;
    }

    public boolean eliminarTransaccion(int transaccionId, int cuentaId, double monto, String tipoTransaccion) {
        String sqlDelete = "DELETE FROM transacciones WHERE id = ?";
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
        String sql = "SELECT t.id, t.monto, t.descripcion, t.tipo_transaccion, t.fecha " +
                "FROM transacciones t " +
                "JOIN cuentas c ON t.cuenta_id = c.id " +
                "WHERE c.usuario_id = ? " +
                "ORDER BY t.fecha DESC LIMIT ? OFFSET ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
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