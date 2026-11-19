package org.UNAL.views;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Paleta y estilos compartidos por las pantallas de NEXUS FINANCE.
 *
 * <p>Centraliza el lenguaje visual (fondos oscuros, neón, tipografía monoespaciada)
 * para que el menú de bienvenida, el dashboard y los módulos se vean como una sola
 * aplicación y no como cuatro pantallas escritas por separado.
 */
public final class CyberTheme {

    private CyberTheme() {
        // Clase de constantes: no instanciable.
    }

    // --- Paleta -----------------------------------------------------------
    public static final String CIAN = "#00f2fe";
    public static final String VERDE = "#13C784";
    public static final String ROJO = "#ff3366";
    public static final String AMBAR = "#ffa500";
    public static final String GRIS = "#a2b0b8";
    public static final String GRIS_OSCURO = "#666688";

    public static final String FONDO = "#05050a";
    public static final String FONDO_ALT = "#0a0f1e";
    public static final String FONDO_PANEL = "rgba(10, 15, 30, 0.85)";

    public static final String FUENTE = "'Courier New'";

    // --- Estilos reutilizables -------------------------------------------

    /** Panel de fondo translúcido con borde neón. */
    public static String panel(String colorBorde) {
        return "-fx-background-color: " + FONDO_PANEL + ";"
                + "-fx-border-color: rgba(" + rgb(colorBorde) + ", 0.35);"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;";
    }

    /** Botón principal en modo reposo. */
    public static String boton(String color, double opacidadFondo, double opacidadBorde) {
        return "-fx-background-color: rgba(" + rgb(color) + ", " + opacidadFondo + ");"
                + "-fx-text-fill: " + GRIS + ";"
                + "-fx-font-family: " + FUENTE + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-border-color: rgba(" + rgb(color) + ", " + opacidadBorde + ");"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 4;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.35), 6, 0, 0, 0);";
    }

    /** Botón en estado hover: fondoilled, texto blanco y halo del color. */
    public static String botonHover(String color) {
        return "-fx-background-color: rgba(" + rgb(color) + ", 0.18);"
                + "-fx-text-fill: #ffffff;"
                + "-fx-font-family: " + FUENTE + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-border-color: " + color + ";"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 4;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(" + rgb(color) + ", 0.4), 12, 0, 0, 0);";
    }

    /** Campo de texto / combo. */
    public static String campo(String color) {
        return "-fx-background-color: rgba(255,255,255,0.03);"
                + "-fx-text-fill: " + color + ";"
                + "-fx-font-family: " + FUENTE + ";"
                + "-fx-font-size: 12px;"
                + "-fx-prompt-text-fill: rgba(" + rgb(color) + ", 0.45);"
                + "-fx-border-color: rgba(" + rgb(color) + ", 0.28);"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 8;";
    }

    /**
     * Estilo base de tablas.
     *
     * <p>Solo propiedades directas: los selectores de encabezado y filas viven en
     * {@code styles/styles.css} porque JavaFX los ignora dentro de setStyle().
     */
    public static String tabla(String color) {
        return "-fx-background-color: transparent;"
                + "-fx-font-family: " + FUENTE + ";"
                + "-fx-font-size: 12px;"
                + "-fx-grid-cell-color: transparent;"
                + "-fx-border-color: rgba(" + rgb(color) + ", 0.3);"
                + "-fx-border-radius: 4;";
    }

    /** Contenedor de barras de desplazamiento. El detalle va en styles.css. */
    public static String scroll(String color) {
        return "-fx-background-color: transparent;"
                + "-fx-border-color: transparent;";
    }

    // --- Constructores de nodos con estilo aplicado -----------------------

    /**
     * Crea un botón con hover neón ya conectado.
     */
    public static Button boton(String texto, String color, Runnable accion, double ancho, double alto) {
        Button b = new Button(texto);
        b.setPrefSize(ancho, alto);
        String reposo = boton(color, 0.05, 0.35);
        b.setStyle(reposo);
        b.setOnMouseEntered(e -> b.setStyle(botonHover(color)));
        b.setOnMouseExited(e -> b.setStyle(reposo));
        if (accion != null) {
            b.setOnAction(e -> accion.run());
        }
        return b;
    }

    public static Label etiqueta(String texto, String color, double tamano, FontWeight peso) {
        Label l = new Label(texto);
        l.setFont(Font.font("Courier New", peso, tamano));
        l.setTextFill(Color.web(color));
        return l;
    }

    /** Título de sección con halo. */
    public static Label titulo(String texto, String color) {
        Label l = etiqueta(texto, color, 18, FontWeight.BOLD);
        l.setEffect(new Glow(0.5));
        return l;
    }

    /** Etiqueta pequeña de estado (subtítulos, captions). */
    public static Label caption(String texto, String color) {
        return etiqueta(texto, color, 10, FontWeight.BOLD);
    }

    /** Halo suave reutilizable para etiquetas y paneles. */
    public static Glow halo(double intensidad) {
        return new Glow(intensidad);
    }

    // --- Utilidades -------------------------------------------------------

    /**
     * Convierte "#00f2fe" en "0, 242, 254" para interpolarlo dentro de rgba().
     */
    public static String rgb(String hex) {
        return String.format("%d, %d, %d",
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }
}