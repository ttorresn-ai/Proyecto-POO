package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Cuenta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code cuentas}.
 *
 * <p>El acceso a una cuenta no se limita al propietario: la tabla puente
 * {@code usuario_cuenta} (relación N:M) grants acceso a terceros usuarios con
 * un rol. Por eso {@link #obtenerCuentasPorUsuario(int)} combina las cuentas
 * propias con las compartidas.
 */
public class CuentaDAO {

    private static final List<String> TIPOS_VALIDOS =
            List.of("Ahorros", "Efectivo", "Tarjeta de Crédito");

    /** Inserta la cuenta y su acceso como propietario dentro de la misma transacción. */
    public boolean crearCuenta(Cuenta cuenta, int usuarioId) throws SQLException, ValidacionException {
        if (cuenta.getNombre() == null || cuenta.getNombre().isBlank()) {
            throw new ValidacionException("El nombre de la cuenta es obligatorio.");
        }
        if (!TIPOS_VALIDOS.contains(cuenta.getTipoCuenta())) {
            throw new ValidacionException("Tipo de cuenta no válido: " + cuenta.getTipoCuenta());
        }

        String sqlCuenta = "INSERT INTO cuentas (nombre, tipo_cuenta, balance_actual, propietario_id) "
                + "VALUES (?, ?, ?, ?)";
        String sqlAcceso = "INSERT INTO usuario_cuenta (usuario_id, cuenta_id, rol) VALUES (?, ?, 'PROPIETARIO')";

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCuenta,
                    PreparedStatement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, cuenta.getNombre().trim());
                pstmt.setString(2, cuenta.getTipoCuenta());
                pstmt.setDouble(3, cuenta.getBalanceActual());
                pstmt.setInt(4, usuarioId);
                pstmt.executeUpdate();

                try (ResultSet claves = pstmt.getGeneratedKeys()) {
                    if (!claves.next()) {
                        conn.rollback();
                        return false;
                    }
                    int cuentaId = claves.getInt(1);

                    // El propietario también queda registrado como participante,
                    // para que el acceso sea consultable desde un único lugar.
                    try (PreparedStatement pstmtAcceso = conn.prepareStatement(sqlAcceso)) {
                        pstmtAcceso.setInt(1, usuarioId);
                        pstmtAcceso.setInt(2, cuentaId);
                        pstmtAcceso.executeUpdate();
                    }

                    // El id generado se refleja en el modelo.
                    cuenta.setId(String.valueOf(cuentaId));
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

    /**
     * Cuentas del usuario: las que posee más las que le comparten.
     */
    public List<Cuenta> obtenerCuentasPorUsuario(int usuarioId) throws SQLException {
        List<Cuenta> cuentas = new ArrayList<>();

        String sql = "SELECT DISTINCT c.id, c.nombre, c.tipo_cuenta, c.balance_actual "
                + "FROM cuentas c "
                + "LEFT JOIN usuario_cuenta uc ON uc.cuenta_id = c.id "
                + "WHERE c.propietario_id = ? OR uc.usuario_id = ? "
                + "ORDER BY c.nombre";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    cuentas.add(mapear(rs));
                }
            }
        }
        return cuentas;
    }

    /** Cuentas en las que el usuario es propietario. */
    public List<Cuenta> obtenerCuentasPropias(int usuarioId) throws SQLException {
        List<Cuenta> cuentas = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo_cuenta, balance_actual FROM cuentas "
                + "WHERE propietario_id = ? ORDER BY nombre";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    cuentas.add(mapear(rs));
                }
            }
        }
        return cuentas;
    }

    public Cuenta obtenerPorId(int cuentaId) throws SQLException {
        String sql = "SELECT id, nombre, tipo_cuenta, balance_actual FROM cuentas WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, cuentaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private Cuenta mapear(ResultSet rs) throws SQLException {
        return new Cuenta(
                String.valueOf(rs.getInt("id")),
                rs.getString("nombre"),
                rs.getString("tipo_cuenta"),
                rs.getDouble("balance_actual")
        );
    }

    /** Comparte una cuenta con otro usuario asignándole un rol. */
    public boolean compartirCuenta(int cuentaId, int usuarioDestino, String rol)
            throws SQLException, ValidacionException {

        if (!List.of("PROPIETARIO", "EDITOR", "VISUALIZADOR").contains(rol)) {
            throw new ValidacionException("Rol no válido: " + rol);
        }

        String sql = "INSERT INTO usuario_cuenta (usuario_id, cuenta_id, rol) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE rol = VALUES(rol)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioDestino);
            pstmt.setInt(2, cuentaId);
            pstmt.setString(3, rol);
            return pstmt.executeUpdate() > 0;
        }
    }

    /** Elimina el acceso de un usuario a una cuenta compartida. */
    public boolean revocarAcceso(int cuentaId, int usuarioId) throws SQLException {
        String sql = "DELETE FROM usuario_cuenta WHERE cuenta_id = ? AND usuario_id = ? "
                + "AND rol <> 'PROPIETARIO'";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, cuentaId);
            pstmt.setInt(2, usuarioId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Ajuste absoluto del balance. Para movimientos prefiere
     * {@link TransaccionDAO}, que además registra el movimiento.
     */
    public boolean actualizarBalance(int cuentaId, double nuevoBalance) throws SQLException {
        if (nuevoBalance < 0) {
            return false;
        }
        String sql = "UPDATE cuentas SET balance_actual = ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, nuevoBalance);
            pstmt.setInt(2, cuentaId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean eliminarCuenta(int cuentaId) throws SQLException {
        String sql = "DELETE FROM cuentas WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, cuentaId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Transfiere fondos entre dos cuentas. Ambas actualizaciones viajan en una
     * sola transacción, y la resta incluye la condición de saldo suficiente
     * para que sea atómica frente a concurrencia.
     */
    public boolean transferirFondos(int cuentaOrigenId, int cuentaDestinoId, double monto)
            throws SQLException, ValidacionException {

        Validador.validarMonto(monto);
        if (cuentaOrigenId == cuentaDestinoId) {
            throw new ValidacionException("Las cuentas origen y destino deben ser distintas.");
        }

        String sqlRestar = "UPDATE cuentas SET balance_actual = balance_actual - ? "
                + "WHERE id = ? AND balance_actual >= ?";
        String sqlSumar = "UPDATE cuentas SET balance_actual = balance_actual + ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtRestar = conn.prepareStatement(sqlRestar);
                 PreparedStatement stmtSumar = conn.prepareStatement(sqlSumar)) {

                stmtRestar.setDouble(1, monto);
                stmtRestar.setInt(2, cuentaOrigenId);
                stmtRestar.setDouble(3, monto);

                // La condición "balance_actual >= ?" hace que la resta sea atómica:
                // si dos transferencias compiten por el mismo saldo, solo una gana.
                if (stmtRestar.executeUpdate() == 0) {
                    throw new ValidacionException("Fondos insuficientes en la cuenta origen.");
                }

                stmtSumar.setDouble(1, monto);
                stmtSumar.setInt(2, cuentaDestinoId);
                if (stmtSumar.executeUpdate() == 0) {
                    throw new ValidacionException("La cuenta destino no existe.");
                }

                conn.commit();
                return true;
            } catch (ValidacionException | SQLException e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw (ValidacionException) e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}