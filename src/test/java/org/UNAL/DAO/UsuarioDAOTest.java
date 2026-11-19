package org.UNAL.DAO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del hashing de contraseñas. No requieren base de datos.
 */
class UsuarioDAOTest {

    private UsuarioDAO dao;

    @BeforeEach
    void setUp() {
        dao = new UsuarioDAO();
    }

    @Test
    @DisplayName("El hash usa PBKDF2 con prefijo identificable")
    void hashUsaPbkdf2() {
        String hash = dao.hashPassword("secreto123");

        assertTrue(hash.startsWith("pbkdf2$"), "El hash debe llevar el prefijo pbkdf2$");
        assertEquals(4, hash.split("\\$").length, "Formato: pbkdf2$iteraciones$salt$hash");
    }

    @Test
    @DisplayName("La misma clave genera hashes distintos por el salt aleatorio")
    void saltRandomHaceHashesDistintos() {
        String hash1 = dao.hashPassword("secreto123");
        String hash2 = dao.hashPassword("secreto123");

        assertNotEquals(hash1, hash2, "El salt aleatorio debe producir hashes distintos");
    }

    @Test
    @DisplayName("Verifica la contraseña correcta y rechaza la incorrecta")
    void verificaPasswordCorrecta() {
        String hash = dao.hashPassword("secreto123");

        assertTrue(dao.verificarPassword("secreto123", hash));
        assertFalse(dao.verificarPassword("secreto124", hash));
        assertFalse(dao.verificarPassword("", hash));
    }

    @Test
    @DisplayName("Rechaza entradas nulas o vacías sin lanzar excepción")
    void rechazaEntradasInvalidas() {
        assertFalse(dao.verificarPassword(null, dao.hashPassword("x")));
        assertFalse(dao.verificarPassword("x", null));
        assertFalse(dao.verificarPassword("x", ""));
        assertFalse(dao.verificarPassword("x", "formato-corrupto"));
    }

    @Test
    @DisplayName("Sigue validando los hashes SHA-256 del esquema anterior")
    void mantieneCompatibilidadConSha256() {
        // Hash generado por la implementación anterior (SHA-256 plano sin salt).
        String hashLegado = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

        assertTrue(dao.verificarPassword("password", hashLegado));
        assertFalse(dao.verificarPassword("otra", hashLegado));
    }
}