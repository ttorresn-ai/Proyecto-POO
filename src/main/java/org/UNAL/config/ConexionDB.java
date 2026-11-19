package org.UNAL.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;

/**
 * Punto único de conexión a la base de datos, con un pool de conexiones.
 *
 * <p><b>Por qué el pool.</b> Abrir un canal TCP con MySQL cuesta entre 30 y 80 ms
 * (handshake, autenticación, TLS). La aplicación abre varias consultas por cada
 * acción del usuario y antes creaba una conexión nueva para cada una, lo que se
 *-traducía en una espera visible en cada clic. Este pool mantiene hasta
 * {@link #TAMANO_MAXIMO} conexiones abiertas y las recicla.
 *
 * <p>Las conexiones se devuelven al pool cuando el {@code try-with-resources} las
 * cierra. Antes de reciclar una conexión se restaura su estado (transacciones
 * pendientes y auto-commit) para que una conexión sucia no contamine a la
 * siguiente petición.
 *
 * <p><b>Configuración</b>, por orden de prioridad:
 * <ol>
 *   <li>Propiedades del sistema (-Ddb.url=..., -Ddb.user=..., -Ddb.password=...)</li>
 *   <li>Variable de entorno {@code DB_PROPERTIES} apuntando a un archivo externo</li>
 *   <li>El classpath: {@code /db.properties}</li>
 * </ol>
 *
 * <p>Las credenciales nunca se escriben en el código fuente.
 */
public final class ConexionDB {

    private static final String ARCHIVO_CONFIG = "db.properties";

    /** Conexiones ociosas que se conservan en memoria. */
    private static final int TAMANO_MAXIMO = 8;

    /** Se cierra una conexión ociosa si no se usa en este tiempo (segundos). */
    private static final int MAXIMO_SIN_USO_SEG = 120;

    private static final Properties CONFIG = new Properties();
    private static boolean configCargada = false;

    private static final Deque<Entrada> POOL = new ArrayDeque<>();
    private static int conexionesCreadas = 0;
    private static final Object CERRAR = new Object();

    /** Conexión ociosa con su marca de tiempo de liberación. */
    private record Entrada(Connection conexion, long liberadoEn) {
    }

    private ConexionDB() {
        // Clase utilitaria: no instanciable.
    }

    /**
     * Obtiene una conexión del pool, creando una nueva si el pool está vacío.
     *
     * <p>Al cerrar la conexión (try-with-resources) esta vuelve al pool en lugar
     * de cerrarse físicamente, salvo que haya pasado demasiado tiempo o.IOException
     * marcada como inválida.
     *
     * @throws SQLException si la configuración falta o no se puede conectar.
     */
    public static Connection conectar() throws SQLException {
        if (!configCargada) {
            cargarConfiguracion();
            configCargada = true;
        }

        synchronized (POOL) {
            Entrada entrada = null;
            while (!POOL.isEmpty()) {
                Entrada candidata = POOL.pop();
                if (candidata.conexion().isClosed()) {
                    conexionesCreadas--;
                    continue; // Another thread closed it; discard.
                }
                if (System.currentTimeMillis() - candidata.liberadoEn() > MAXIMO_SIN_USO_SEG * 1000L) {
                    cerrarSilenciosamente(candidata.conexion());
                    conexionesCreadas--;
                    continue;
                }
                entrada = candidata;
                break;
            }
            if (entrada != null) {
                return envolver(entrada.conexion());
            }
            if (conexionesCreadas >= TAMANO_MAXIMO) {
                // Pool saturado: se espera breve a que alguien devuelva una conexión.
                try {
                    POOL.wait(2000);
                    Entrada liberada = POOL.poll();
                    if (liberada != null && !liberada.conexion().isClosed()) {
                        return envolver(liberada.conexion());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            conexionesCreadas++;
        }

        try {
            Connection nueva = abrirConexionFisica();
            return envolver(nueva);
        } catch (SQLException e) {
            synchronized (POOL) {
                conexionesCreadas--;
            }
            throw e;
        }
    }

    /** Crea una conexión nueva contra la base de datos configurada. */
    private static Connection abrirConexionFisica() throws SQLException {
        String url = CONFIG.getProperty("db.url");
        String user = CONFIG.getProperty("db.user");
        String password = CONFIG.getProperty("db.password");

        if (url == null || url.isBlank()) {
            throw new SQLException(
                    "Falta la propiedad 'db.url'. Copia db/db.properties.example a "
                    + "src/main/resources/db.properties y configura tus credenciales."
            );
        }

        try {
            if (DriverManager.getDriver(url) == null) {
                String driver = CONFIG.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                try {
                    Class.forName(driver);
                } catch (ClassNotFoundException ignored) {
                    // Si el driver no está en el classpath, DriverManager lo informará abajo.
                }
            }
            return DriverManager.getConnection(url, user, password == null ? "" : password);
        } catch (SQLException e) {
            throw new SQLException("No se pudo conectar con la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Envuelve la conexión física en un proxy que, al hacer {@code close()},
     * restaura su estado y la devuelve al pool.
     */
    private static Connection envolver(Connection fisica) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                ConexionDB.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, args) -> {
                    String nombre = metodo.getName();
                    try {
                        switch (nombre) {
                            case "close" -> {
                                devolverAlPool(fisica);
                                return null;
                            }
                            case "isClosed" -> {
                                return fisica.isClosed();
                            }
                            case "unwrap" -> {
                                // unwrap(Connection.class) debe devolver la física,
                                // de lo contrario try-with-resources la cerraría dos veces.
                                if (args != null && args.length == 1 && args[0] == Connection.class) {
                                    return fisica;
                                }
                                return metodo.invoke(fisica, args);
                            }
                            case "isValid" -> {
                                // Detecta conexiones muertas antes de reciclar.
                                int timeout = (args != null && args.length == 1 && args[0] instanceof Integer i)
                                        ? i : 0;
                                return validar(fisica, timeout);
                            }
                            default -> {
                                return metodo.invoke(fisica, args);
                            }
                        }
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }

    private static boolean validar(Connection conexion, int timeoutSeg) {
        try {
            if (!conexion.isValid(timeoutSeg)) {
                return false;
            }
            if (timeoutSeg > 0) {
                return true;
            }
            // Con timeout 0, isValid no siempre consulta el servidor: forzamos un ping.
            try (Statement st = conexion.createStatement()) {
                st.execute("SELECT 1");
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Devuelve la conexión al pool tras limpiar su estado transaccional.
     */
    private static void devolverAlPool(Connection conexion) {
        if (conexion == null) return;

        boolean cerrada;
        try {
            cerrada = conexion.isClosed();
        } catch (SQLException e) {
            cerrada = true;
        }

        if (cerrada) {
            synchronized (POOL) {
                conexionesCreadas = Math.max(0, conexionesCreadas - 1);
            }
            return;
        }

        try {
            // Si quedó una transacción abierta, la deshacemos para no filtrar
            // cambios hacia la siguiente petición que reutilice esta conexión.
            if (!conexion.getAutoCommit()) {
                conexion.rollback();
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            // Conexión defectuosa: se descarta en lugar de devolverla.
            cerrarSilenciosamente(conexion);
            synchronized (POOL) {
                conexionesCreadas = Math.max(0, conexionesCreadas - 1);
            }
            return;
        }

        synchronized (POOL) {
            if (conexionesCreadas > TAMANO_MAXIMO || POOL.size() >= TAMANO_MAXIMO) {
                cerrarSilenciosamente(conexion);
                conexionesCreadas = Math.max(0, conexionesCreadas - 1);
                POOL.notifyAll();
            } else {
                POOL.push(new Entrada(conexion, System.currentTimeMillis()));
            }
        }
    }

    private static void cerrarSilenciosamente(Connection conexion) {
        try {
            conexion.close();
        } catch (SQLException ignored) {
            // Nada que hacer al descartar.
        }
    }

    /**
     * Cierra todas las conexiones del pool. Se invoca al terminar la aplicación
     * para liberar los sockets de MySQL.
     */
    public static void cerrarPool() {
        synchronized (POOL) {
            while (!POOL.isEmpty()) {
                cerrarSilenciosamente(POOL.pop().conexion());
            }
            conexionesCreadas = 0;
        }
    }

    // -----------------------------------------------------------------------
    //  Configuración
    // -----------------------------------------------------------------------

    private static void cargarConfiguracion() {
        CONFIG.clear();

        String archivoExterno = System.getenv("DB_PROPERTIES");
        if (archivoExterno != null && !archivoExterno.isBlank()
                && Files.exists(Path.of(archivoExterno))) {
            try (InputStream in = Files.newInputStream(Path.of(archivoExterno))) {
                CONFIG.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "No se pudo leer el archivo de configuración: " + archivoExterno, e);
            }
        } else {
            try (InputStream in = ConexionDB.class.getClassLoader().getResourceAsStream(ARCHIVO_CONFIG)) {
                if (in == null) {
                    // Sin archivo: conectar() reportará el error con un mensaje accionable.
                    return;
                }
                CONFIG.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + ARCHIVO_CONFIG, e);
            }
        }

        sobreescribirDesdePropiedadesDelSistema("db.url");
        sobreescribirDesdePropiedadesDelSistema("db.user");
        sobreescribirDesdePropiedadesDelSistema("db.password");
        sobreescribirDesdePropiedadesDelSistema("db.driver");
    }

    private static void sobreescribirDesdePropiedadesDelSistema(String clave) {
        String valor = System.getProperty(clave);
        if (valor != null && !valor.isBlank()) {
            CONFIG.setProperty(clave, valor);
        }
    }

    /** Permite a las pruebas inyectar una configuración en memoria. */
    public static void configurarParaPruebas(String url, String user, String password) {
        CONFIG.setProperty("db.url", url);
        CONFIG.setProperty("db.user", user);
        CONFIG.setProperty("db.password", password);
        configCargada = true;
    }
}