package org.UNAL.views;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BienvenidaView {

    private MediaPlayer videoPlayer;
    private MediaPlayer musicaPlayer;
    private Timeline cronometro;
    private boolean musicaMuteada = false;

    private AudioClip sfxSelect;
    private AudioClip sfxPress;

    // Se agrega el tercer parámetro 'onVideoReady' para sincronizar la transición
    public Scene getScene(Runnable onLogin, Runnable onRegistro, Runnable onVideoReady) {
        // CONTENEDOR RAÍZ PRINCIPAL
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #05050a;");

        // CARGA DE EFECTOS DE SONIDO (SFX)
        try {
            URL selectUrl = Thread.currentThread().getContextClassLoader().getResource("views/select.mp3");
            URL pressUrl = Thread.currentThread().getContextClassLoader().getResource("views/press.mp3");
            if (selectUrl != null) sfxSelect = new AudioClip(selectUrl.toString());
            if (pressUrl != null) sfxPress = new AudioClip(pressUrl.toString());
        } catch (Exception e) {
            // Silencioso
        }

        // =====================================================================
        // CAPA 1: VIDEO DE FONDO
        // =====================================================================
        MediaView mediaView = new MediaView();
        mediaView.setPreserveRatio(false);
        mediaView.setSmooth(false); // Optimización CPU

        try {
            URL videoUrl = Thread.currentThread().getContextClassLoader().getResource("views/artemis-2.mp4");
            if (videoUrl != null) {
                Media media = new Media(videoUrl.toString());
                videoPlayer = new MediaPlayer(media);
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                videoPlayer.setMute(true);
                mediaView.setMediaPlayer(videoPlayer);

                // SINCRONIZACIÓN: Avisa a la app principal cuando el video cargó por completo
                videoPlayer.setOnReady(() -> {
                    if (onVideoReady != null) {
                        javafx.application.Platform.runLater(onVideoReady);
                    }
                });

                videoPlayer.play();
            }
        } catch (Exception e) {
            // Si falla el video, dejamos pasar la app para que no se quede congelada
            if (onVideoReady != null) {
                javafx.application.Platform.runLater(onVideoReady);
            }
        }

        Pane videoContainer = new Pane(mediaView);
        videoContainer.setCache(true);
        videoContainer.setCacheHint(javafx.scene.CacheHint.SPEED);
        videoContainer.setMouseTransparent(true); // El mouse ignora esta capa de video

        mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
        mediaView.fitHeightProperty().bind(videoContainer.heightProperty());

        // REPRODUCCIÓN DE LA MÚSICA DE FONDO
        try {
            URL musicaUrl = Thread.currentThread().getContextClassLoader().getResource("views/Space.mp3");
            if (musicaUrl != null) {
                Media audioMedia = new Media(musicaUrl.toString());
                musicaPlayer = new MediaPlayer(audioMedia);
                musicaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicaPlayer.setVolume(0.4);
                musicaPlayer.play();
            }
        } catch (Exception e) {
            // Silencioso
        }

        // =====================================================================
        // CAPA 2: INTERFAZ GRÁFICA ACTIVA (BOTONES Y MENÚS)
        // =====================================================================
        AnchorPane capaInterfaz = new AnchorPane();
        capaInterfaz.setPickOnBounds(false);

        String estiloBotonBase =
                "-fx-background-color: rgba(0, 242, 254, 0.05);" +
                        "-fx-text-fill: #e0e0e0;" +
                        "-fx-font-size: 1.1em;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: rgba(0, 242, 254, 0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 4;" +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;";

        String estiloBotonHover =
                "-fx-background-color: rgba(0, 242, 254, 0.2);" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-border-color: #00f2fe;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,242,254,0.4), 10, 0, 0, 0);";

        VBox menuLateral = new VBox(20);
        menuLateral.setAlignment(Pos.CENTER);
        menuLateral.setPrefWidth(260);
        menuLateral.setMaxWidth(260);
        menuLateral.setStyle(
                "-fx-background-color: rgba(10, 15, 30, 0.65);" +
                        "-fx-border-color: rgba(0, 242, 254, 0.2);" +
                        "-fx-border-width: 0 1 0 0;"
        );
        menuLateral.setPadding(new Insets(40, 20, 40, 20));

        Label logoN = new Label("N");
        logoN.setFont(Font.font("Times New Roman", FontWeight.BOLD, 90));
        logoN.setTextFill(Color.WHITE);
        logoN.setEffect(new Glow(0.8));

        Label titulo = new Label("NEXUS FINANCE");
        titulo.setTextFill(Color.web("#00f2fe"));
        titulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titulo.setEffect(new Glow(0.5));

        Label subtitulo = new Label("SYSTEM ONLINE");
        subtitulo.setTextFill(Color.web("#13C784"));
        subtitulo.setFont(Font.font("Courier New", FontWeight.BOLD, 12));

        Region espaciador = new Region();
        VBox.setVgrow(espaciador, Priority.ALWAYS);

        Button loginBtn = new Button("LOG IN");
        loginBtn.setPrefWidth(200);
        loginBtn.setPrefHeight(45);
        loginBtn.setStyle(estiloBotonBase);
        loginBtn.setOnMouseEntered(e -> {
            loginBtn.setStyle(estiloBotonHover);
            if (sfxSelect != null && !musicaMuteada) sfxSelect.play();
        });
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(estiloBotonBase));
        loginBtn.setOnAction(e -> {
            if (sfxPress != null && !musicaMuteada) sfxPress.play();
            if (onLogin != null) onLogin.run();
        });

        Button registroBtn = new Button("REGISTER");
        registroBtn.setPrefWidth(200);
        registroBtn.setPrefHeight(45);
        registroBtn.setStyle(estiloBotonBase);
        registroBtn.setOnMouseEntered(e -> {
            registroBtn.setStyle(estiloBotonHover);
            if (sfxSelect != null && !musicaMuteada) sfxSelect.play();
        });
        registroBtn.setOnMouseExited(e -> registroBtn.setStyle(estiloBotonBase));
        registroBtn.setOnAction(e -> {
            if (sfxPress != null && !musicaMuteada) sfxPress.play();
            if (onRegistro != null) onRegistro.run();
        });

        menuLateral.getChildren().addAll(logoN, titulo, subtitulo, espaciador, loginBtn, registroBtn);

        Button audioBtn = new Button("AUDIO: ON 🔊");
        audioBtn.setStyle(estiloBotonBase + "-fx-font-size: 0.9em; -fx-padding: 8 15 8 15;");
        audioBtn.setOnMouseEntered(e -> audioBtn.setStyle(estiloBotonHover + "-fx-font-size: 0.9em; -fx-padding: 8 15 8 15;"));
        audioBtn.setOnMouseExited(e -> audioBtn.setStyle(estiloBotonBase + "-fx-font-size: 0.9em; -fx-padding: 8 15 8 15;"));
        audioBtn.setOnAction(e -> {
            musicaMuteada = !musicaMuteada;
            if (musicaPlayer != null) musicaPlayer.setMute(musicaMuteada);
            audioBtn.setText(musicaMuteada ? "AUDIO: OFF 🔇" : "AUDIO: ON 🔊");
        });

        HBox topBar = new HBox(audioBtn);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(25));
        topBar.setPickOnBounds(false);

        VBox hudReloj = new VBox(2);
        hudReloj.setAlignment(Pos.BOTTOM_RIGHT);
        hudReloj.setPadding(new Insets(25));

        Label lblReloj = new Label();
        lblReloj.setTextFill(Color.web("#00f2fe"));
        lblReloj.setFont(Font.font("Courier New", FontWeight.BOLD, 32));
        lblReloj.setEffect(new Glow(0.6));

        Label lblTag = new Label("LOCAL TIME // SECURE_CONN");
        lblTag.setTextFill(Color.web("#666688"));
        lblTag.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        hudReloj.getChildren().addAll(lblReloj, lblTag);

        DateTimeFormatter formateador = DateTimeFormatter.ofPattern("HH:mm:ss");
        cronorchestra(lblReloj, formateador);

        AnchorPane.setLeftAnchor(menuLateral, 0.0);
        AnchorPane.setTopAnchor(menuLateral, 0.0);
        AnchorPane.setBottomAnchor(menuLateral, 0.0);

        AnchorPane.setTopAnchor(topBar, 0.0);
        AnchorPane.setRightAnchor(topBar, 0.0);

        AnchorPane.setBottomAnchor(hudReloj, 0.0);
        AnchorPane.setRightAnchor(hudReloj, 0.0);

        capaInterfaz.getChildren().addAll(menuLateral, topBar, hudReloj);

        root.getChildren().addAll(videoContainer, capaInterfaz);

        return new Scene(root, 1024, 576);
    }

    private void cronorchestra(Label lblReloj, DateTimeFormatter formateador) {
        cronometro = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblReloj.setText(LocalTime.now().format(formateador));
        }));
        cronometro.setCycleCount(Animation.INDEFINITE);
        cronometro.play();
    }

    public void detenerMultimedia() {
        try {
            if (cronometro != null) {
                cronometro.stop();
                cronometro = null;
            }
        } catch (Exception ex) {
            // silencioso
        }

        try {
            if (videoPlayer != null) {
                // Desacoplar del MediaView antes de dispose
                // (si tienes acceso al MediaView desde aquí, haz mediaView.setMediaPlayer(null);)
                videoPlayer.stop();
                videoPlayer.dispose(); // forzar liberación nativa
                videoPlayer = null;
            }
        } catch (Exception e) {
            System.err.println("// TRACE: Error cerrando video -> " + e.getMessage());
        }

        try {
            if (musicaPlayer != null) {
                musicaPlayer.stop();
                musicaPlayer.dispose();
                musicaPlayer = null;
            }
        } catch (Exception e) {
            System.err.println("// TRACE: Error cerrando música -> " + e.getMessage());
        }
    }
}