package org.UNAL.views;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.UNAL.modelo.Usuario;

/**
 * Base común de los módulos del área autenticada (cuentas, transacciones,
 * presupuestos y reportes).
 *
 * <p>Centraliza el montaje de la escena sobre el {@link Stage} principal y el
 * comportamiento del botón "Volver", de modo que cada subclase solo implementa
 * {@link #construirEscena()}.
 */
public abstract class ModuloAutenticado {

    protected final Usuario usuario;
    private Runnable onVolver;

    protected ModuloAutenticado(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El módulo requiere un usuario autenticado.");
        }
        this.usuario = usuario;
    }

    /**
     * Construye la escena del módulo. Invocado una vez por cada montaje.
     */
    protected abstract Scene construirEscena();

    /**
     * Monta el módulo en el stage principal.
     *
     * @param stage    stage donde se muestra la escena
     * @param onVolver callback ejecutado al pulsar "Volver"
     */
    public final void mostrarEn(Stage stage, Runnable onVolver) {
        this.onVolver = onVolver;

        // Cada montaje reconstruye la escena: los nodos JavaFX no pueden
        // volver a añadirse a dos escenas distintas a la vez.
        Scene escena = construirEscena();
        aplicarEstilos(escena);

        Button volver = (Button) escena.lookup("#btnVolver");
        if (volver != null) {
            volver.setOnAction(e -> volver());
        }

        stage.setResizable(true);
        stage.setScene(escena);
        stage.centerOnScreen();
    }

    /** Regresa al dashboard. */
    protected void volver() {
        if (onVolver != null) {
            onVolver.run();
        }
    }

    /**
     * Carga la hoja de estilos compartida. Los selectores (scrollbars, tablas)
     * solo funcionan desde una stylesheet: JavaFX no los interpreta dentro de
     * {@code Node.setStyle()}.
     */
    private void aplicarEstilos(Scene escena) {
        java.net.URL hoja = Thread.currentThread()
                .getContextClassLoader()
                .getResource("styles/styles.css");
        if (hoja != null) {
            escena.getStylesheets().add(hoja.toExternalForm());
        }
    }

    /** Identificador del usuario autenticado. */
    protected int usuarioId() {
        return Integer.parseInt(usuario.getId());
    }
}