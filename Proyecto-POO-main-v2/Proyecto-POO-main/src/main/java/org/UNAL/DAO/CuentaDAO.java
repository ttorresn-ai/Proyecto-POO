package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.modelo.Cuenta;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAO {

    public boolean crearCuenta(Cuenta cuenta, int usuarioId) {
        String sql = "INSERT INTO cuentas (nombre, tipo_cuenta, balance_actual, usuario_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cuenta.getNombre());
            pstmt.setString(2, cuenta.getTipoCuenta());
            pstmt.setDouble(3, cuenta.getBalanceActual());
            pstmt.setInt(4, usuarioId);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al crear cuenta: " + e.getMessage());
            return false;
        }
    }

    public List<Cuenta> obtenerCuentasPorUsuario(int usuarioId) {
        List<Cuenta> cuentas = new ArrayList<>();
        String sql = "SELECT * FROM cuentas WHERE usuario_id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Cuenta c = new Cuenta(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nombre"),
                        rs.getString("tipo_cuenta"),
                        rs.getDouble("balance_actual")
                );
                cuentas.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener cuentas: " + e.getMessage());
        }
        return cuentas;
    }

    public boolean actualizarBalance(int cuentaId, double nuevoBalance) {
        String sql = "UPDATE cuentas SET balance_actual = ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, nuevoBalance);
            pstmt.setInt(2, cuentaId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar balance: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarCuenta(int cuentaId) {
        String sql = "DELETE FROM cuentas WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, cuentaId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar cuenta: " + e.getMessage());
            return false;
        }
    }

    public boolean transferirFondos(int cuentaOrigenId, int cuentaDestinoId, double monto) {
        String sqlRestar = "UPDATE cuentas SET balance_actual = balance_actual - ? WHERE id = ?";
        String sqlSumar = "UPDATE cuentas SET balance_actual = balance_actual + ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtRestar = conn.prepareStatement(sqlRestar);
                 PreparedStatement stmtSumar = conn.prepareStatement(sqlSumar)) {

                stmtRestar.setDouble(1, monto);
                stmtRestar.setInt(2, cuentaOrigenId);
                int filasRestadas = stmtRestar.executeUpdate();

                stmtSumar.setDouble(1, monto);
                stmtSumar.setInt(2, cuentaDestinoId);
                int filasSumadas = stmtSumar.executeUpdate();

                if (filasRestadas > 0 && filasSumadas > 0) {
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
}