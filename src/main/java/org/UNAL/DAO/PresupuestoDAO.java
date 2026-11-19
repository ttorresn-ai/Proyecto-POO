package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Presupuesto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla {@code presupuestos}.
 */
public class PresupuestoDAO {

    public boolean guardarPresupuesto(Presupuesto p, int usuarioId, int categoriaId)
            throws SQLException, ValidacionException {

        Validador.validarMonto(p.getMontoLimite());

        if (p.getMes() < 1 || p.getMes() > 12) {
            throw new ValidacionException("Mes inválido: debe estar entre 1 y 12.");
        }
        if (p.getAnio() < 2000 || p.getAnio() > 2100) {
            throw new ValidacionException("Año inválido.");
        }

        // Upsert: evita duplicar el presupuesto del mismo periodo para la misma categoría.
        String sql = "INSERT INTO presupuestos (monto_limite, mes, anio, categoria_id, usuario_id) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE monto_limite = VALUES(monto_limite)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, p.getMontoLimite());
            pstmt.setInt(2, p.getMes());
            pstmt.setInt(3, p.getAnio());
            pstmt.setInt(4, categoriaId);
            pstmt.setInt(5, usuarioId);

            // executeUpdate devuelve 1 en insert y 2 en update; ambos son éxito.
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean actualizarMonto(int presupuestoId, double nuevoMonto)
            throws SQLException, ValidacionException {

        Validador.validarMonto(nuevoMonto);
        String sql = "UPDATE presupuestos SET monto_limite = ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, nuevoMonto);
            pstmt.setInt(2, presupuestoId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean eliminarPresupuesto(int presupuestoId) throws SQLException {
        String sql = "DELETE FROM presupuestos WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, presupuestoId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Gasto acumulado por presupuesto en una sola consulta.
     *
     * <p>Evita el patrón N+1: antes la tabla hacía una consulta por fila y por
     * repintado. Cruza contra la subclase {@code gastos} para identificar los
     * movimientos de gasto.
     *
     * @return mapa id_presupuesto -> total gastado
     */
    public Map<Integer, Double> obtenerGastoPorPresupuesto(int usuarioId, int anio, int mes) throws SQLException {
        Map<Integer, Double> resultado = new HashMap<>();

        String sql = "SELECT p.id AS presupuesto_id, COALESCE(SUM(g.monto), 0) AS gastado "
                + "FROM presupuestos p "
                + "LEFT JOIN ( "
                + "    SELECT gs.categoria_id, t.monto "
                + "    FROM gastos gs "
                + "    JOIN transacciones t ON t.id = gs.transaccion_id "
                + "    WHERE t.usuario_id = ? "
                + "      AND YEAR(t.fecha) = ? AND MONTH(t.fecha) = ? "
                + ") g ON g.categoria_id = p.categoria_id "
                + "WHERE p.usuario_id = ? AND p.anio = ? AND p.mes = ? "
                + "GROUP BY p.id";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, anio);
            pstmt.setInt(3, mes);
            pstmt.setInt(4, usuarioId);
            pstmt.setInt(5, anio);
            pstmt.setInt(6, mes);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getInt("presupuesto_id"), rs.getDouble("gastado"));
                }
            }
        }
        return resultado;
    }

    /**
     * Gasto acumulado del usuario en una categoría durante un mes concreto.
     */
    public double obtenerGastoActual(int usuarioId, int categoriaId, int mes, int anio) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.monto), 0) "
                + "FROM gastos g "
                + "JOIN transacciones t ON t.id = g.transaccion_id "
                + "WHERE t.usuario_id = ? "
                + "AND g.categoria_id = ? "
                + "AND YEAR(t.fecha) = ? AND MONTH(t.fecha) = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, categoriaId);
            pstmt.setInt(3, anio);
            pstmt.setInt(4, mes);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    public List<Presupuesto> obtenerPresupuestosPorUsuario(int usuarioId) throws SQLException {
        List<Presupuesto> lista = new ArrayList<>();
        String sql = "SELECT id, monto_limite, mes, anio, categoria_id "
                + "FROM presupuestos WHERE usuario_id = ? "
                + "ORDER BY anio DESC, mes DESC, categoria_id";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        }
        return lista;
    }
}