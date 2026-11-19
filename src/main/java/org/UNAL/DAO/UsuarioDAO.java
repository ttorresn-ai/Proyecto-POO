package org.UNAL.DAO;

import org.UNAL.config.ConexionDB;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Usuario;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

/**
 * Acceso a datos de la tabla {@code usuarios}.
 *
 * <p>Las contraseñas se almacenan con PBKDF2-HMAC-SHA256 y un salt aleatorio
 * por usuario. El formato persistido es:
 * <pre>pbkdf2$&lt;iteraciones&gt;$&lt;saltBase64&gt;$&lt;hashBase64&gt;</pre>
 */
public class UsuarioDAO {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_BITS = 256;
    private static final int LONGITUD_SALT = 16;
    private static final String PREFIJO = "pbkdf2";

    private final SecureRandom random = new SecureRandom();

    // -----------------------------------------------------------------------
    //  Hashing
    // -----------------------------------------------------------------------

    public String hashPassword(String password) {
        try {
            byte[] salt = new byte[LONGITUD_SALT];
            random.nextBytes(salt);

            byte[] hash = derivar(password.toCharArray(), salt, ITERACIONES);

            return PREFIJO + "$" + ITERACIONES + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo derivar la contraseña", e);
        }
    }

    /**
     * Verifica una contraseña en claro contra el hash almacenado.
     * La comparación es de tiempo constante para evitar ataques de temporización.
     */
    public boolean verificarPassword(String passwordEnClaro, String hashAlmacenado) {
        if (passwordEnClaro == null || hashAlmacenado == null || hashAlmacenado.isBlank()) {
            return false;
        }

        // Retrocompatibilidad: hashes antiguos generados con SHA-256 plano.
        if (!hashAlmacenado.startsWith(PREFIJO + "$")) {
            return MessageDigest.isEqual(
                    hashSha256Legado(passwordEnClaro).getBytes(),
                    hashAlmacenado.getBytes()
            );
        }

        String[] partes = hashAlmacenado.split("\\$");
        if (partes.length != 4) {
            return false;
        }

        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);

            byte[] calculado = derivar(passwordEnClaro.toCharArray(), salt, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] clave, byte[] salt, int iteraciones)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(clave, salt, iteraciones, LONGITUD_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** SHA-256 plano sin salt. Solo para validar hashes creados antes de la migración a PBKDF2. */
    private String hashSha256Legado(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // -----------------------------------------------------------------------
    //  Operaciones CRUD
    // -----------------------------------------------------------------------

    public boolean registrarUsuario(Usuario usuario) throws SQLException, ValidacionException {
        Validador.validarNombre(usuario.getNombre());
        Validador.validarEmail(usuario.getEmail());
        Validador.validarPassword(usuario.getPassword());

        String sql = "INSERT INTO usuarios (nombre, email, password) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNombre().trim());
            pstmt.setString(2, usuario.getEmail().trim().toLowerCase());
            pstmt.setString(3, hashPassword(usuario.getPassword()));

            return pstmt.executeUpdate() > 0;
        }
    }

    public Usuario iniciarSesion(String email, String password) throws SQLException {
        String sql = "SELECT id, nombre, email, password FROM usuarios WHERE email = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("password");
                    if (verificarPassword(password, hashAlmacenado)) {
                        // No propagamos el hash hacia la capa de vista.
                        return new Usuario(
                                String.valueOf(rs.getInt("id")),
                                rs.getString("nombre"),
                                rs.getString("email"),
                                ""
                        );
                    }
                }
            }
        }
        return null;
    }

    public Usuario obtenerPorEmail(String email) throws SQLException {
        String sql = "SELECT id, nombre, email FROM usuarios WHERE email = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            String.valueOf(rs.getInt("id")),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            ""
                    );
                }
            }
        }
        return null;
    }

    public boolean actualizarUsuario(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuarios SET nombre = ?, email = ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNombre().trim());
            pstmt.setString(2, usuario.getEmail().trim().toLowerCase());
            pstmt.setInt(3, Integer.parseInt(usuario.getId()));

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean cambiarPassword(int usuarioId, String passwordNueva) throws SQLException, ValidacionException {
        Validador.validarPassword(passwordNueva);
        String sql = "UPDATE usuarios SET password = ? WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashPassword(passwordNueva));
            pstmt.setInt(2, usuarioId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean eliminarUsuario(int id) throws SQLException {
        // Las cuentas, transacciones y presupuestos caen en cascada (ON DELETE CASCADE).
        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public double obtenerBalanceTotal(int usuarioId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(c.balance_actual), 0) FROM cuentas c "
                + "LEFT JOIN usuario_cuenta uc ON uc.cuenta_id = c.id "
                + "WHERE c.propietario_id = ? OR uc.usuario_id = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }
}