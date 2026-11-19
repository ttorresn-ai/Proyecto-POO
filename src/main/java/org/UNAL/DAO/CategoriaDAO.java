package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos del catálogo {@code categorias}.
 */
public class CategoriaDAO {

    public List<Categoria> obtenerTodas() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM categorias ORDER BY nombre";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Categoria obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre FROM categorias WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Resuelve varios ids en una sola consulta, evitando el patrón N+1 al pintar
     * tablas que guardan únicamente la clave foránea de la categoría.
     *
     * @return mapa id -> nombre
     */
    public Map<Integer, String> mapearNombres(List<Integer> ids) throws SQLException {
        Map<Integer, String> resultado = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return resultado;
        }

        // Los placeholders se generan a partir del tamaño de la lista, nunca del contenido.
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, nombre FROM categorias WHERE id IN (" + placeholders + ")";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                pstmt.setInt(i + 1, ids.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultado.put(rs.getInt("id"), rs.getString("nombre"));
                }
            }
        }
        return resultado;
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        return new Categoria(
                String.valueOf(rs.getInt("id")),
                rs.getString("nombre")
        );
    }
}