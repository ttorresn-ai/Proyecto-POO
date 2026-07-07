package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.modelo.Presupuesto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAO {

    public boolean guardarPresupuesto(Presupuesto p, int usuarioId, int categoriaId) {
        String sql = "INSERT INTO presupuestos (monto_limite, mes, anio, categoria_id, usuario_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, p.getMontoLimite());
            pstmt.setInt(2, p.getMes());
            pstmt.setInt(3, p.getAnio());
            pstmt.setInt(4, categoriaId);
            pstmt.setInt(5, usuarioId);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al guardar presupuesto: " + e.getMessage());
            return false;
        }
    }

    public double obtenerGastoActual(int usuarioId, int categoriaId, int mes, int anio) {
        String sql = "SELECT SUM(monto) FROM transacciones WHERE cuenta_id IN (SELECT id FROM cuentas WHERE usuario_id = ?) " +
                "AND categoria_id = ? AND MONTH(fecha) = ? AND YEAR(fecha) = ? AND tipo_transaccion = 'GASTO'";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, categoriaId);
            pstmt.setInt(3, mes);
            pstmt.setInt(4, anio);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.out.println("Error al calcular gasto actual: " + e.getMessage());
        }
        return 0;
    }

    public List<Presupuesto> obtenerPresupuestosPorUsuario(int usuarioId) {
        List<Presupuesto> lista = new ArrayList<>();
        String sql = "SELECT id, monto_limite, mes, anio, categoria_id FROM presupuestos WHERE usuario_id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Presupuesto p = new Presupuesto(
                        String.valueOf(rs.getInt("id")),
                        rs.getDouble("monto_limite"),
                        rs.getInt("mes"),
                        rs.getInt("anio")
                );

                p.setCategoriaId(rs.getInt("categoria_id"));

                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener presupuestos: " + e.getMessage());
        }
        return lista;
    }
}