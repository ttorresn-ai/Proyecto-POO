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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;
import java.net.URL;

public class DashboardView {

    private MediaPlayer videoPlayer;
    private AudioClip sfxSelect;
    private AudioClip sfxPress;
    private MediaView mediaView;
    private ProgressBar progressBar;

        public Scene getScene(String nombreUsuario,
                              double balanceTotal,
                              Runnable onCuentas,
                              Runnable onIngreso,
                              Runnable onGasto,
                              Runnable onPresupuestos,
                              Runnable onReportes,
                              Runnable onLogout) {

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #020208;");

        // CARGA CONTROLADA DE EFECTOS
        try {
            URL selectUrl = Thread.currentThread().getContextClassLoader().getResource("views/select.mp3");
            URL pressUrl = Thread.currentThread().getContextClassLoader().getResource("views/press.mp3");
            if (selectUrl != null) sfxSelect = new AudioClip(selectUrl.toString());
            if (pressUrl != null) sfxPress = new AudioClip(pressUrl.toString());
        } catch (Exception e) {
            System.err.println("Error cargando audios dashboard: " + e.getMessage());
        }

        mediaView = new MediaView();
        mediaView.setPreserveRatio(false);

        Pane videoContainer = new Pane(mediaView);
        mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
        mediaView.fitHeightProperty().bind(videoContainer.heightProperty());

        // DISEÑO DEL MENÚ TÁCTICO HUD
        VBox hudLayout = new VBox(25);
        hudLayout.setPadding(new Insets(40));
        hudLayout.setAlignment(Pos.TOP_LEFT);
        hudLayout.setVisible(false); // Oculto hasta que acabe la carga masiva

        Label lblWelcome = new Label("OPERATOR // " + nombreUsuario.toUpperCase());
        lblWelcome.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        lblWelcome.setTextFill(Color.web("#13C784"));

        Label lblBalance = new Label("NET_WORTH: $" + String.format("%.2fx", balanceTotal));
        lblBalance.setFont(Font.font("Courier New", FontWeight.BOLD, 38));
        lblBalance.setTextFill(Color.web("#00f2fe"));
        lblBalance.setEffect(new Glow(0.7));

        GridPane gridBotones = new GridPane();
        gridBotones.setHgap(25);
        gridBotones.setVgap(25);

        String estiloMódulos = "-fx-background-color: rgba(0, 242, 254, 0.02); -fx-text-fill: #a2b0b8; -fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: rgba(0, 242, 254, 0.2); -fx-border-width: 1.5; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;";

        Button btnCuentas = crearBotonMenu("TACTICAL_ACCOUNTS", estiloMódulos, onCuentas);
        Button btnIngreso = crearBotonMenu("CREDIT_INBOUND", estiloMódulos, onIngreso);
        Button btnGasto = crearBotonMenu("DEBIT_OUTBOUND", estiloMódulos, onGasto);
        Button btnPresupuesto = crearBotonMenu("BUDGET_CORES", estiloMódulos, onPresupuestos);
        Button btnReporte = crearBotonMenu("DATA_ANALYTICS", estiloMódulos, onReportes);

        Button btnLogout = new Button("TERMINATE_SESSION");
        btnLogout.setPrefSize(230, 90);
        btnLogout.setStyle("-fx-background-color: rgba(255,51,102,0.02); -fx-text-fill: #ff3366; -fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: rgba(255,51,102,0.3); -fx-border-width: 1.5; -fx-border-radius: 4; -fx-cursor: hand;");
        btnLogout.setOnMouseEntered(e -> { btnLogout.setStyle("-fx-background-color: rgba(255,51,102,0.12); -fx-text-fill: #ffffff; -fx-border-color: #ff3366; -fx-effect: dropshadow(three-pass-box, rgba(255,51,102,0.4), 12, 0, 0, 0);"); if (sfxSelect != null) sfxSelect.play(); });
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle("-fx-background-color: rgba(255,51,102,0.02); -fx-text-fill: #ff3366; -fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: rgba(255,51,102,0.3); -fx-border-width: 1.5; -fx-border-radius: 4;"));
        btnLogout.setOnAction(e -> { if (sfxPress != null) sfxPress.play(); detenerMultimedia(); onLogout.run(); });

        gridBotones.add(btnCuentas, 0, 0);
        gridBotones.add(btnIngreso, 1, 0);
        gridBotones.add(btnGasto, 2, 0);
        gridBotones.add(btnPresupuesto, 0, 1);
        gridBotones.add(btnReporte, 1, 1);
        gridBotones.add(btnLogout, 2, 1);

        hudLayout.getChildren().addAll(lblWelcome, lblBalance, new Label(), gridBotones);

        // CONTENEDOR DE CARGA (LOADING SCREEN OVERLAY)
        VBox loadingHUD = new VBox(15);
        loadingHUD.setAlignment(Pos.CENTER);
        loadingHUD.setMaxSize(400, 150);
        loadingHUD.setStyle("-fx-background-color: rgba(5,7,15,0.85); -fx-border-color: #00f2fe; -fx-border-width: 1; -fx-border-radius: 4;");

        Label lblCarga = new Label("DECRYPTING_SECURE_NODE...");
        lblCarga.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        lblCarga.setTextFill(Color.web("#00f2fe"));
        lblCarga.setEffect(new Glow(0.5));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.setStyle("-fx-accent: #00f2fe; -fx-control-inner-background: rgba(255,255,255,0.05);");

        loadingHUD.getChildren().addAll(lblCarga, progressBar);

        root.getChildren().addAll(videoContainer, hudLayout, loadingHUD);

        // DISPARADOR SECUENCIAL EN DIFERIDO
        Platform.runLater(() -> {
            // Detenemos cualquier rastro multimedia viejo por si las moscas
            detenerMultimedia();

            Timeline retrasoInicial = new Timeline(new KeyFrame(Duration.millis(15), e ->
                    cargarYReproducirSecuencia(root, hudLayout, loadingHUD)
            ));
            retrasoInicial.play();
        });

        return new Scene(root, 1024, 640);
    }

    private void cargarYReproducirSecuencia(StackPane root, VBox hudLayout, VBox loadingHUD) {
        try {
            URL transicionUrl = Thread.currentThread().getContextClassLoader().getResource("views/Transicion.mp4");
            if (transicionUrl != null) {
                Media mediaTransicion = new Media(transicionUrl.toString());
                videoPlayer = new MediaPlayer(mediaTransicion);
                mediaView.setMediaPlayer(videoPlayer);
                videoPlayer.play();

                videoPlayer.setOnEndOfMedia(() -> {
                    URL federacionUrl = Thread.currentThread().getContextClassLoader().getResource("views/Federacion.mp4");
                    if (federacionUrl != null) {

                        // CORRECCIÓN CRÍTICA DE CPU: Desechar hilos nativos decodificadores de la transición
                        videoPlayer.stop();
                        videoPlayer.dispose(); // <--- IMPRESCINDIBLE PARA LIBERAR LA CPU AL 100%

                        videoPlayer = new MediaPlayer(new Media(federacionUrl.toString()));
                        videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        mediaView.setMediaPlayer(videoPlayer);
                        videoPlayer.play();
                    }

                    // Lanzar simulación de la barra cyberpunk
                    Timeline loadingTimeline = new Timeline();
                    int totalPasos = 40;
                    for (int i = 1; i <= totalPasos; i++) {
                        final double progreso = (double) i / totalPasos;
                        KeyFrame frame = new KeyFrame(Duration.millis(i * 20), e -> progressBar.setProgress(progreso));
                        loadingTimeline.getKeyFrames().add(frame);
                    }

                    loadingTimeline.setOnFinished(e -> {
                        root.getChildren().remove(loadingHUD);
                        hudLayout.setVisible(true);
                    });
                    loadingTimeline.play();
                });
            }
        } catch (Exception ex) {
            System.err.println("Error en secuencia multimedia: " + ex.getMessage());
        }
    }

    private Button crearBotonMenu(String texto, String estilo, Runnable accion) {
        Button btn = new Button(texto);
        btn.setStyle(estilo);
        btn.setPrefSize(230, 90);

        String estiloHover = "-fx-background-color: rgba(0, 242, 254, 0.18); -fx-text-fill: #ffffff; -fx-border-color: #00f2fe; -fx-effect: dropshadow(three-pass-box, rgba(0,242,254,0.4), 12, 0, 0, 0);";

        btn.setOnMouseEntered(e -> { btn.setStyle(estilo + estiloHover); if (sfxSelect != null) sfxSelect.play(); });
        btn.setOnMouseExited(e -> btn.setStyle(estilo));
        btn.setOnAction(e -> { if (sfxPress != null) sfxPress.play(); detenerMultimedia(); accion.run(); });

        return btn;
    }

    public void detenerMultimedia() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose(); // <--- Evita fugas acumulativas de memoria nativa de video
            videoPlayer = null;
        }
        if (mediaView != null) {
            mediaView.setMediaPlayer(null);
        }
    }
}