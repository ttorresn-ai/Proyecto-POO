package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import java.util.Date;

public class ReporteDAO {

    public ObservableMap<String, Double> obtenerGastosPorCategoria(int usuarioId, Date inicio, Date fin) {
        ObservableMap<String, Double> resultados = FXCollections.observableHashMap();

        String sql = "SELECT c.nombre, SUM(t.monto) as total " +
                "FROM transacciones t " +
                "JOIN categorias c ON t.categoria_id = c.id " +
                "JOIN cuentas cu ON t.cuenta_id = cu.id " +
                "WHERE cu.usuario_id = ? AND t.tipo_transaccion = 'GASTO' " +
                "AND t.fecha BETWEEN ? AND ? " +
                "GROUP BY c.nombre";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setTimestamp(2, new java.sql.Timestamp(inicio.getTime()));
            pstmt.setTimestamp(3, new java.sql.Timestamp(fin.getTime()));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                resultados.put(rs.getString("nombre"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            System.out.println("Error al generar reporte de gastos: " + e.getMessage());
        }
        return resultados;
    }

    public double obtenerBalanceTotal(int usuarioId) {
        String sql = "SELECT SUM(balance_actual) FROM cuentas WHERE usuario_id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.out.println("Error al obtener balance total: " + e.getMessage());
        }
        return 0.0;
    }
}