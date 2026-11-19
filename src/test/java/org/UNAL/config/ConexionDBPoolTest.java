package org.UNAL.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifica que el pool de conexiones se comporta bien bajo uso concurrente,
 * que es el escenario real de la aplicación (varias consultas simultáneas).
 *
 * <p>Solo se ejecuta si hay una base de datos configurada en db.properties.
 * Si no la hay, las pruebas se omiten en lugar de fallar, para que el build
 * siga siendo verde en un entorno sin MySQL.
 */
class ConexionDBPoolTest {

    private static final int HILOS = 8;
    private static final int ITERACIONES = 6;

    @AfterEach
    void tearDown() {
        ConexionDB.cerrarPool();
    }

    /** Indica si el entorno tiene una base de datos utilizable. */
    private boolean baseDisponible() {
        try (Connection conn = ConexionDB.conectar();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Abre una conexión válida con la configuración actual")
    void abreConexionValida() {
        if (!baseDisponible()) {
            // Sin base de datos no hay nada que probar aquí.
            return;
        }
        try (Connection conn = ConexionDB.conectar()) {
            assertTrue(conn.isValid(2), "La conexión debe ser válida");
            assertTrue(!conn.isClosed(), "La conexión no debe estar cerrada al obtenerla");
        } catch (SQLException e) {
            fail("No se pudo abrir la conexión: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Recicla conexiones: abrir y cerrar 50 veces no agota el pool")
    void reciclaConexiones() throws Exception {
        if (!baseDisponible()) {
            return;
        }

        for (int i = 0; i < 50; i++) {
            try (Connection conn = ConexionDB.conectar();
                 Statement st = conn.createStatement()) {
                st.execute("SELECT 1");
            }
        }
        // Si el pool no recicla, aquí habríamos abierto 50 conexiones TCP.
        // Con recycleo, siguen siendo unas pocas.
        try (Connection conn = ConexionDB.conectar()) {
            assertTrue(conn.isValid(2));
        }
    }

    @Test
    @DisplayName("Aguenta consultas concurrentes sin romper el pool")
    void soportaConcurrencia() throws Exception {
        if (!baseDisponible()) {
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        AtomicInteger exitosas = new AtomicInteger();
        List<Callable<Void>> tareas = new ArrayList<>();

        for (int i = 0; i < HILOS; i++) {
            tareas.add(() -> {
                for (int j = 0; j < ITERACIONES; j++) {
                    try (Connection conn = ConexionDB.conectar();
                         Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery("SELECT 1")) {
                        if (rs.next()) {
                            exitosas.incrementAndGet();
                        }
                    }
                }
                return null;
            });
        }

        List<Future<Void>> resultados = pool.invokeAll(tareas, 60, TimeUnit.SECONDS);
        for (Future<Void> f : resultados) {
            f.get(); // Propaga cualquier excepción de un hilo.
        }
        pool.shutdownNow();

        assertEquals(HILOS * ITERACIONES, exitosas.get(),
                "Todas las consultas concurrentes deben completarse");
    }

    @Test
    @DisplayName("Un rollback pendiente no se filtra a la siguiente conexión")
    void noFiltraRollbackPendiente() throws Exception {
        if (!baseDisponible()) {
            return;
        }

        // Abrimos una transacción y la abandonamos sin commit.
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            st.execute("SELECT 1"); // Queda una transacción abierta.
            // Al salir del try sin commit, el pool debe hacer rollback.
        }

        // La siguiente conexión no debe heredar el modo transaccional.
        try (Connection conn = ConexionDB.conectar()) {
            assertTrue(conn.getAutoCommit(),
                    "La conexión reciclada debe volver a autoCommit=true");
        }
    }
}