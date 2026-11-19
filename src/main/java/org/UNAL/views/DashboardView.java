package org.UNAL.views;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.Glow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.UNAL.DAO.UsuarioDAO;
import org.UNAL.modelo.Usuario;

import java.net.URL;

/**
 * Menú principal del área autenticada.
 *
 * <p>Recibe la sesión del usuario y callbacks de navegación, de modo que no
 * conoce las vistas concretas que abren sus botones.
 */
public class DashboardView {

    private static final String COLOR_CIAN = "#00f2fe";
    private static final String COLOR_VERDE = "#13C784";
    private static final String COLOR_ROJO = "#ff3366";

    private MediaPlayer videoPlayer;
    private AudioClip sfxSelect;
    private AudioClip sfxPress;
    private MediaView mediaView;
    private Timeline timeoutCarga;
    private Timeline animacionCarga;

    private Usuario usuario;
    private Runnable onCuentas;
    private Runnable onIngreso;
    private Runnable onGasto;
    private Runnable onPresupuestos;
    private Runnable onReportes;
    private Runnable onLogout;

    /**
     * @param usuario       sesión activa
     * @param onCuentas     abre el módulo de cuentas
     * @param onIngreso     abre el registro de ingresos
     * @param onGasto       abre el registro de gastos
     * @param onPresupuestos abre el módulo de presupuestos
     * @param onReportes    abre el módulo de reportes
     * @param onLogout      cierra la sesión
     */
    public Scene getScene(Usuario usuario,
                          Runnable onCuentas,
                          Runnable onIngreso,
                          Runnable onGasto,
                          Runnable onPresupuestos,
                          Runnable onReportes,
                          Runnable onLogout) {

        this.usuario = usuario;
        this.onCuentas = onCuentas;
        this.onIngreso = onIngreso;
        this.onGasto = onGasto;
        this.onPresupuestos = onPresupuestos;
        this.onReportes = onReportes;
        this.onLogout = onLogout;

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #020208;");

        cargarEfectosSonido();

        // --- Capa 1: video de fondo
        mediaView = new MediaView();
        mediaView.setPreserveRatio(false);
        mediaView.setSmooth(false);

        Pane videoContainer = new Pane(mediaView);
        videoContainer.setCache(true);
        videoContainer.setCacheHint(javafx.scene.CacheHint.SPEED);
        videoContainer.setMouseTransparent(true);
        mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
        mediaView.fitHeightProperty().bind(videoContainer.heightProperty());

        // --- Capa 2: HUD
        Label lblWelcome = new Label("OPERATOR // " + usuario.getNombre().toUpperCase());
        lblWelcome.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        lblWelcome.setTextFill(Color.web(COLOR_VERDE));

        Label lblBalance = new Label("NET_WORTH: $" + formatear(cargarBalance()));
        lblBalance.setFont(Font.font("Courier New", FontWeight.BOLD, 38));
        lblBalance.setTextFill(Color.web(COLOR_CIAN));
        lblBalance.setEffect(new Glow(0.7));

        GridPane gridBotones = new GridPane();
        gridBotones.setHgap(25);
        gridBotones.setVgap(25);

        String estiloModulos = "-fx-background-color: rgba(0, 242, 254, 0.02); "
                + "-fx-text-fill: #a2b0b8; -fx-font-family: 'Courier New'; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-border-color: rgba(0, 242, 254, 0.2); "
                + "-fx-border-width: 1.5; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;";

        Button btnCuentas = crearBotonMenu("TACTICAL_ACCOUNTS", estiloModulos, onCuentas);
        Button btnIngreso = crearBotonMenu("CREDIT_INBOUND", estiloModulos, onIngreso);
        Button btnGasto = crearBotonMenu("DEBIT_OUTBOUND", estiloModulos, onGasto);
        Button btnPresupuesto = crearBotonMenu("BUDGET_CORES", estiloModulos, onPresupuestos);
        Button btnReporte = crearBotonMenu("DATA_ANALYTICS", estiloModulos, onReportes);

        Button btnLogout = new Button("TERMINATE_SESSION");
        btnLogout.setPrefSize(230, 90);
        btnLogout.setStyle(estiloBoton(COLOR_ROJO, 0.02, 0.3));
        btnLogout.setOnMouseEntered(e -> {
            btnLogout.setStyle(estiloBotonHover(COLOR_ROJO));
            if (sfxSelect != null) sfxSelect.play();
        });
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle(estiloBoton(COLOR_ROJO, 0.02, 0.3)));
        btnLogout.setOnAction(e -> {
            if (sfxPress != null) sfxPress.play();
            onLogout.run();
        });

        gridBotones.add(btnCuentas, 0, 0);
        gridBotones.add(btnIngreso, 1, 0);
        gridBotones.add(btnGasto, 2, 0);
        gridBotones.add(btnPresupuesto, 0, 1);
        gridBotones.add(btnReporte, 1, 1);
        gridBotones.add(btnLogout, 2, 1);

        VBox hudLayout = new VBox(25, lblWelcome, lblBalance, new Label(), gridBotones);
        hudLayout.setPadding(new Insets(40));
        hudLayout.setAlignment(Pos.TOP_LEFT);

        // --- Capa 3: overlay de carga
        VBox loadingHUD = new VBox(15);
        loadingHUD.setAlignment(Pos.CENTER);
        loadingHUD.setMaxSize(400, 150);
        loadingHUD.setStyle("-fx-background-color: rgba(5,7,15,0.85); "
                + "-fx-border-color: " + COLOR_CIAN + "; -fx-border-width: 1; -fx-border-radius: 4;");

        Label lblCarga = new Label("DECRYPTING_SECURE_NODE...");
        lblCarga.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        lblCarga.setTextFill(Color.web(COLOR_CIAN));
        lblCarga.setEffect(new Glow(0.5));

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.setStyle("-fx-accent: " + COLOR_CIAN + "; -fx-control-inner-background: rgba(255,255,255,0.05);");

        loadingHUD.getChildren().addAll(lblCarga, progressBar);

        root.getChildren().addAll(videoContainer, hudLayout, loadingHUD);

        Platform.runLater(() -> reproducirSecuencia(root, hudLayout, loadingHUD, progressBar));

        return new Scene(root, 1024, 640);
    }

    /**
     * Reproduce la transición y luego el fondo en bucle, animando la barra de
     * carga. Si el video falla, el HUD se muestra igualmente para no dejar la
     * pantalla bloqueada.
     */
    private void reproducirSecuencia(StackPane root, VBox hudLayout, VBox loadingHUD, ProgressBar progressBar) {
        try {
            URL transicionUrl = recurso("views/Transicion.mp4");
            if (transicionUrl == null) {
                mostrarHud(root, hudLayout, loadingHUD);
                return;
            }

            videoPlayer = new MediaPlayer(new Media(transicionUrl.toString()));
            mediaView.setMediaPlayer(videoPlayer);
            videoPlayer.play();

            videoPlayer.setOnEndOfMedia(() -> {
                try {
                    // Liberamos los hilos nativos del video de transición antes
                    // de cargar el siguiente, para no acumular decodificadores.
                    videoPlayer.stop();
                    videoPlayer.dispose();

                    URL fondoUrl = recurso("views/Federacion.mp4");
                    if (fondoUrl != null) {
                        videoPlayer = new MediaPlayer(new Media(fondoUrl.toString()));
                        videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        videoPlayer.setMute(true);
                        mediaView.setMediaPlayer(videoPlayer);
                        videoPlayer.play();
                    }

                    animarCargaYMostrarHud(root, hudLayout, loadingHUD, progressBar);
                } catch (Exception ex) {
                    System.err.println("Error en la secuencia de video: " + ex.getMessage());
                    mostrarHud(root, hudLayout, loadingHUD);
                }
            });

            // Red de seguridad: si el video no emite fin, mostramos el HUD igual.
            // Guardamos la referencia para poder cancelarla desde detenerMultimedia().
            timeoutCarga = new Timeline(new KeyFrame(
                    Duration.seconds(12), e -> mostrarHud(root, hudLayout, loadingHUD)));
            timeoutCarga.play();

        } catch (Exception ex) {
            System.err.println("Error en la secuencia multimedia: " + ex.getMessage());
            mostrarHud(root, hudLayout, loadingHUD);
        }
    }

    private void animarCargaYMostrarHud(StackPane root, VBox hudLayout, VBox loadingHUD, ProgressBar progressBar) {
        // El timeout ya no es necesario: el video terminó.
        if (timeoutCarga != null) {
            timeoutCarga.stop();
            timeoutCarga = null;
        }

        animacionCarga = new Timeline();
        int totalPasos = 40;
        for (int i = 1; i <= totalPasos; i++) {
            final double progreso = (double) i / totalPasos;
            animacionCarga.getKeyFrames().add(
                    new KeyFrame(Duration.millis(i * 20), e -> progressBar.setProgress(progreso))
            );
        }
        animacionCarga.setOnFinished(e -> {
            animacionCarga = null;
            mostrarHud(root, hudLayout, loadingHUD);
        });
        animacionCarga.play();
    }

    private void mostrarHud(StackPane root, VBox hudLayout, VBox loadingHUD) {
        Platform.runLater(() -> {
            root.getChildren().remove(loadingHUD);
            hudLayout.setVisible(true);
        });
    }

    private void cargarEfectosSonido() {
        try {
            URL selectUrl = recurso("views/select.mp3");
            URL pressUrl = recurso("views/press.mp3");
            if (selectUrl != null) sfxSelect = new AudioClip(selectUrl.toString());
            if (pressUrl != null) sfxPress = new AudioClip(pressUrl.toString());
        } catch (Exception e) {
            System.err.println("Error cargando audios del dashboard: " + e.getMessage());
        }
    }

    private Button crearBotonMenu(String texto, String estilo, Runnable accion) {
        Button btn = new Button(texto);
        btn.setStyle(estilo);
        btn.setPrefSize(230, 90);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(estilo + "-fx-background-color: rgba(0, 242, 254, 0.18); "
                    + "-fx-text-fill: #ffffff; -fx-border-color: " + COLOR_CIAN + "; "
                    + "-fx-effect: dropshadow(three-pass-box, rgba(0,242,254,0.4), 12, 0, 0, 0);");
            if (sfxSelect != null) sfxSelect.play();
        });
        btn.setOnMouseExited(e -> btn.setStyle(estilo));
        btn.setOnAction(e -> {
            if (sfxPress != null) sfxPress.play();
            // Detenemos el video antes de abrir el módulo para liberar la CPU.
            detenerMultimedia();
            accion.run();
        });

        return btn;
    }

    private double cargarBalance() {
        try {
            return new UsuarioDAO().obtenerBalanceTotal(Integer.parseInt(usuario.getId()));
        } catch (Exception e) {
            System.err.println("No se pudo cargar el balance: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Detiene y libera los recursos nativos de video.
     * Debe invocarse antes de salir del dashboard para no dejar decodificadores
     * huérfanos consumiendo CPU.
     */
    public void detenerMultimedia() {
        // Las animaciones se detienen primero: si siguen corriendo, retienen
        // referencias a los nodos yamontados en la escena.
        if (timeoutCarga != null) {
            timeoutCarga.stop();
            timeoutCarga = null;
        }
        if (animacionCarga != null) {
            animacionCarga.stop();
            animacionCarga = null;
        }

        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
                videoPlayer.dispose();
            } catch (Exception e) {
                System.err.println("// TRACE: Error liberando video -> " + e.getMessage());
            }
            videoPlayer = null;
        }
        if (mediaView != null) {
            mediaView.setMediaPlayer(null);
        }

        // Los AudioClip retienen el audio nativo:
        // los paramos para que no sigan sonando tras salir del dashboard.
        if (sfxSelect != null) {
            sfxSelect.stop();
        }
        if (sfxPress != null) {
            sfxPress.stop();
        }
    }

    private static URL recurso(String ruta) {
        return Thread.currentThread().getContextClassLoader().getResource(ruta);
    }

    private static String estiloBoton(String color, double fondo, double borde) {
        return String.format(
                "-fx-background-color: rgba(%s, %.2f); -fx-text-fill: %s; "
                + "-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; "
                + "-fx-border-color: rgba(%s, %.2f); -fx-border-width: 1.5; "
                + "-fx-border-radius: 4; -fx-cursor: hand;",
                sinHash(color), fondo, color, sinHash(color), borde);
    }

    private static String estiloBotonHover(String color) {
        return String.format(
                "-fx-background-color: rgba(%s, 0.12); -fx-text-fill: #ffffff; "
                + "-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; "
                + "-fx-border-color: %s; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(%s, 0.4), 12, 0, 0, 0);",
                sinHash(color), color, sinHash(color));
    }

    /** Convierte "#00f2fe" en "0,242,254" para usarlo dentro de rgba(). */
    private static String sinHash(String hex) {
        return String.format("%d, %d, %d",
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }

    private static String formatear(double monto) {
        return String.format("%,.2f", monto);
    }
}