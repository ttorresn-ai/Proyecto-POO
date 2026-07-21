package org.UNAL.views;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.media.AudioClip;
import org.UNAL.DAO.UsuarioDAO;
import org.UNAL.modelo.Usuario;
import javafx.util.Duration;
import java.net.URL;
import java.util.Random;
import java.util.function.Consumer;

public class LoginView {

    private AudioClip sfxEscribir;
    private AudioClip sfxBorrar;
    private AudioClip sfxPress;
    private AudioClip sfxCancel;

    private String contrasenaReal = "";
    private final String[] simbolosAlienigenas = {"▰", "▱", "◤", "◢", "█", "⚡", "☣", "Ω", "Ψ", "Ξ", "Ϡ", "⚡", "⚙", "☍"};
    private final Random random = new Random();

    // El Timeline maestro que mutará los símbolos en segundo plano sin gastar CPU
    private Timeline glitchTimeline;

    public VBox getContainer(BienvenidaView bienvenidaView, Consumer<Usuario> onLoginExitoso, Runnable onRegistro, Runnable onVolver) {

        try {
            URL selectUrl = Thread.currentThread().getContextClassLoader().getResource("views/alien_type.mp3");
            URL cancelUrl = Thread.currentThread().getContextClassLoader().getResource("views/alien_delete.mp3");
            URL pressUrl = Thread.currentThread().getContextClassLoader().getResource("views/press.mp3");
            URL backUrl = Thread.currentThread().getContextClassLoader().getResource("views/cancel.mp3");

            if (selectUrl != null) sfxEscribir = new AudioClip(selectUrl.toString());
            if (cancelUrl != null) sfxBorrar = new AudioClip(cancelUrl.toString());
            if (pressUrl != null) sfxPress = new AudioClip(pressUrl.toString());
            if (backUrl != null) sfxCancel = new AudioClip(backUrl.toString());
        } catch (Exception e) {
            System.err.println("Error sonidos login: " + e.getMessage());
        }

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setMaxSize(400, 480);
        root.setPadding(new Insets(35));

        root.setStyle("-fx-background-color: rgba(5, 8, 18, 0.90); " +
                "-fx-border-color: rgba(0, 242, 254, 0.3); " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,242,254,0.15), 20, 0, 0, 0);");

        Label titulo = new Label("LOGIN_SYSTEM_UNIT");
        titulo.setFont(Font.font("Courier New", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#13C784"));
        titulo.setEffect(new Glow(0.7));

        String estiloCampos = "-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: #13C784; -fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-prompt-text-fill: rgba(19, 199, 132, 0.4); -fx-border-color: rgba(19, 199, 132, 0.25); -fx-border-radius: 4; -fx-padding: 10;";

        TextField emailField = new TextField();
        emailField.setPromptText("ENTER_EMAIL_ADDRESS");
        emailField.setStyle(estiloCampos);
        configurarSonidoTecladoNormal(emailField);

        TextField passField = new TextField();
        passField.setPromptText("ENTER_ACCESS_KEY");
        passField.setStyle(estiloCampos);

        Label errorLbl = new Label();
        errorLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        errorLbl.setWrapText(true);

        this.contrasenaReal = "";

        // =====================================================================
        // EFECTO MATRIX ULTRA-OPTIMIZADO (Bucle de mutación cosmética)
        // =====================================================================
        glitchTimeline = new Timeline(new KeyFrame(Duration.millis(120), event -> {
            int longitud = contrasenaReal.length();
            if (longitud > 0) {
                StringBuilder mascaraMutada = new StringBuilder();
                // Regenera símbolos aleatorios basándose puramente en el tamaño de la clave
                for (int i = 0; i < longitud; i++) {
                    mascaraMutada.append(simbolosAlienigenas[random.nextInt(simbolosAlienigenas.length)]);
                }

                // Guardamos la posición actual del cursor por seguridad
                int posicionCursor = passField.getCaretPosition();
                passField.setText(mascaraMutada.toString());
                passField.positionCaret(posicionCursor);
            }
        }));
        glitchTimeline.setCycleCount(Animation.INDEFINITE);
        glitchTimeline.play(); // Comienza a escuchar en segundo plano de forma nativa

        // Interceptación de teclado síncrona
        passField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String caracterTipeado = event.getCharacter();
            event.consume();

            if (caracterTipeado.length() == 0) return;
            char c = caracterTipeado.charAt(0);

            if (c == '\b') {
                if (!contrasenaReal.isEmpty()) {
                    contrasenaReal = contrasenaReal.substring(0, contrasenaReal.length() - 1);
                    if (sfxBorrar != null) { sfxBorrar.stop(); sfxBorrar.play(); }

                    String textoActual = passField.getText();
                    if (!textoActual.isEmpty()) {
                        passField.setText(textoActual.substring(0, textoActual.length() - 1));
                    }
                }
            } else if (c != '\u001b' && c != '\u007f' && c != '\r' && c != '\n') {
                contrasenaReal += c;
                if (sfxEscribir != null) { sfxEscribir.stop(); sfxEscribir.play(); }

                String simboloNuevo = simbolosAlienigenas[random.nextInt(simbolosAlienigenas.length)];
                passField.setText(passField.getText() + simboloNuevo);
            }

            passField.positionCaret(passField.getText().length());
        });

        String estiloBtn = "-fx-background-color: transparent; -fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-width: 1; -fx-border-radius: 4; -fx-cursor: hand;";
        String estiloBtnHover = "-fx-background-color: rgba(0, 242, 254, 0.1); -fx-text-fill: #ffffff;";

        Button loginBtn = new Button("ACCESS");
        loginBtn.setPrefWidth(90);
        loginBtn.setPrefHeight(35);
        loginBtn.setStyle(estiloBtn + "-fx-border-color: rgba(19, 199, 132, 0.4); -fx-text-fill: #13C784;");
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(estiloBtnHover + "-fx-border-color: #13C784; -fx-text-fill: #13C784;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(estiloBtn + "-fx-border-color: rgba(19, 199, 132, 0.4); -fx-text-fill: #13C784;"));

        // Modificamos la acción del botón para apagar la animación al salir
        loginBtn.setOnAction(e -> {
            loginBtn.setDisable(true);

            String email = emailField.getText().trim();
            if (email.isEmpty() || contrasenaReal.isEmpty()) {
                if (sfxCancel != null) sfxCancel.play();
                errorLbl.setTextFill(Color.web("#ff3366"));
                errorLbl.setText("// FAILED: EMPTY_FIELDS");
                loginBtn.setDisable(false);
                return;
            }

            UsuarioDAO dao = new UsuarioDAO();
            Usuario usuario = dao.iniciarSesion(email, contrasenaReal);

            if (usuario != null) {
                if (glitchTimeline != null) glitchTimeline.stop(); // Apagamos el motor al cambiar de vista
                if (sfxPress != null) sfxPress.play();
                errorLbl.setTextFill(Color.web("#13C784"));
                errorLbl.setText("// ACCESS_GRANTED");

                if (bienvenidaView != null) {
                    bienvenidaView.detenerMultimedia();
                }

                onLoginExitoso.accept(usuario);
            } else {
                if (sfxCancel != null) sfxCancel.play();
                errorLbl.setTextFill(Color.web("#ff3366"));
                errorLbl.setText("// INVALID_CREDENTIALS");
                loginBtn.setDisable(false);
            }
        });

        Button registroBtn = new Button("CREATE UNIT");
        registroBtn.setPrefWidth(110);
        registroBtn.setPrefHeight(35);
        registroBtn.setStyle(estiloBtn + "-fx-border-color: rgba(0, 242, 254, 0.4); -fx-text-fill: #00f2fe;");
        registroBtn.setOnMouseEntered(e -> registroBtn.setStyle(estiloBtnHover + "-fx-border-color: #00f2fe; -fx-text-fill: #00f2fe;"));
        registroBtn.setOnMouseExited(e -> registroBtn.setStyle(estiloBtn + "-fx-border-color: rgba(0, 242, 254, 0.4); -fx-text-fill: #00f2fe;"));
        registroBtn.setOnAction(e -> {
            if (glitchTimeline != null) glitchTimeline.stop(); // Apagamos el motor
            if (sfxPress != null) sfxPress.play();
            if (onRegistro != null) onRegistro.run();
        });

        Button volverBtn = new Button("BACK");
        volverBtn.setPrefWidth(80);
        volverBtn.setPrefHeight(35);
        volverBtn.setStyle(estiloBtn + "-fx-border-color: rgba(255, 51, 102, 0.4); -fx-text-fill: #ff3366;");
        volverBtn.setOnMouseEntered(e -> volverBtn.setStyle(estiloBtnHover + "-fx-border-color: #ff3366; -fx-text-fill: #ff3366;"));
        volverBtn.setOnMouseExited(e -> volverBtn.setStyle(estiloBtn + "-fx-border-color: rgba(255, 51, 102, 0.4); -fx-text-fill: #ff3366;"));
        volverBtn.setOnAction(e -> {
            if (glitchTimeline != null) glitchTimeline.stop(); // Apagamos el motor
            if (sfxCancel != null) sfxCancel.play();
            if (onVolver != null) onVolver.run();
        });

        HBox btnBox = new HBox(12, volverBtn, registroBtn, loginBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titulo, emailField, passField, btnBox, errorLbl);

        return root;
    }

    private void configurarSonidoTecladoNormal(TextField campo) {
        campo.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > oldValue.length()) {
                if (sfxEscribir != null) { sfxEscribir.stop(); sfxEscribir.play(); }
            } else if (newValue.length() < oldValue.length()) {
                if (sfxBorrar != null) { sfxBorrar.stop(); sfxBorrar.play(); }
            }
        });
    }
}