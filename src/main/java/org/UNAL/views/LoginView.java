package org.UNAL.views;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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

    private static URL recurso(String ruta) {
        return Thread.currentThread().getContextClassLoader().getResource(ruta);
    }

    private AudioClip sfxEscribir;
    private AudioClip sfxBorrar;
    private AudioClip sfxPress;
    private AudioClip sfxCancel;

    private String contrasenaReal = "";
    /**
     * Cuando es {@code true} el campo de clave deja de enmascararse y muestra el
     * texto real. El login siempre usa {@link #contrasenaReal}, de modo que el
     * modo visible no altera lo que se envia al DAO.
     */
    private boolean contrasenaVisible = false;
    private final String[] simbolosAlienigenas = {"▰", "▱", "◤", "◢", "█", "⚡", "☣", "Ω", "Ψ", "Ξ", "Ϡ", "⚡", "⚙", "☍"};
    private final Random random = new Random();

    // El Timeline maestro que mutará los símbolos en segundo plano sin gastar CPU
    private Timeline glitchTimeline;

    public VBox getContainer(BienvenidaView bienvenidaView, Consumer<Usuario> onLoginExitoso, Runnable onRegistro, Runnable onVolver) {

        try {
            // alien_type.mp3 y alien_delete.mp3 no existen en el proyecto:
            // reutilizamos los efectos de tecleo disponibles.
            URL selectUrl = recurso("views/select.mp3");
            URL cancelUrl = recurso("views/cancel.mp3");
            URL pressUrl = recurso("views/press.mp3");
            URL backUrl = recurso("views/cancel.mp3");

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
        // La animación solo hace trabajo cuando hay algo que enmascarar. Antes
        // corría un Timeline INDEFINITE desde el arranque que repintaba el
        // campo 8 veces por segundo aunque estuviera vacío.
        Timeline motorGlitch = new Timeline(new KeyFrame(Duration.millis(120), event -> {
            int longitud = contrasenaReal.length();
            if (longitud > 0) {
                StringBuilder mascaraMutada = new StringBuilder(longitud);
                for (int i = 0; i < longitud; i++) {
                    mascaraMutada.append(simbolosAlienigenas[random.nextInt(simbolosAlienigenas.length)]);
                }
                int posicionCursor = passField.getCaretPosition();
                passField.setText(mascaraMutada.toString());
                passField.positionCaret(Math.max(0, posicionCursor));
            }
        }));
        motorGlitch.setCycleCount(Animation.INDEFINITE);
        this.glitchTimeline = motorGlitch;

        // Interceptación de teclado síncrona
        passField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            // En modo visible no interceptamos nada: el campo se comporta como
            // un TextField normal y el listener de texto mantiene contrasenaReal.
            if (contrasenaVisible) {
                if (sfxEscribir != null) { sfxEscribir.stop(); sfxEscribir.play(); }
                return;
            }

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

                // Arrancamos el motor de glitch la primera vez que se escribe.
                if (motorGlitch.getStatus() != Animation.Status.RUNNING) {
                    motorGlitch.play();
                }
            }

            passField.positionCaret(passField.getText().length());
        });

        // El motor de glitch solo consume CPU mientras el campo tiene foco,
        // y jamas mientras la clave este en modo visible.
        passField.focusedProperty().addListener(
                (javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean tieneFoco, Boolean prev) -> {
            if (tieneFoco) {
                if (motorGlitch.getStatus() == Animation.Status.PAUSED
                        && !contrasenaReal.isEmpty()
                        && !contrasenaVisible) {
                    motorGlitch.play();
                }
            } else if (motorGlitch.getStatus() == Animation.Status.RUNNING) {
                motorGlitch.pause();
            }
        });

        // Arrancar el motor al escribir evita tener que hacerlo en el teclado.
        contrasenaReal = "";

        // =====================================================================
        // VISUALIZAR / OCULTAR LA CLAVE
        // =====================================================================
        // Con la clave visible el campo ya no se enmascara, asi que basta con
        // copiar lo que se escriba. El listener se activa solo en ese modo:
        // en modo enmascarado el motor de glitch llama a setText() sin parar y
        // sincronizaria el texto alienigena en lugar de la contrasena real.
        passField.textProperty().addListener((obs, viejo, nuevo) -> {
            if (contrasenaVisible) {
                contrasenaReal = nuevo == null ? "" : nuevo;
            }
        });

        Label verClave = new Label("VER CLAVE");
        verClave.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        verClave.setTextFill(Color.web("#00f2fe"));
        verClave.setCursor(Cursor.HAND);
        verClave.setOnMouseClicked(e -> {
            contrasenaVisible = !contrasenaVisible;

            if (contrasenaVisible) {
                // Mostrar: se apaga el motor de glitch y se saca el texto real.
                if (motorGlitch.getStatus() == Animation.Status.RUNNING) {
                    motorGlitch.pause();
                }
                passField.setText(contrasenaReal);
                verClave.setText("OCULTAR CLAVE");
                verClave.setTextFill(Color.web("#ff3366"));
            } else {
                // Volver a enmascarar: el campo retoma los simbolos alienigenas.
                StringBuilder mascara = new StringBuilder();
                for (int i = 0; i < contrasenaReal.length(); i++) {
                    mascara.append(simbolosAlienigenas[random.nextInt(simbolosAlienigenas.length)]);
                }
                passField.setText(mascara.toString());
                verClave.setText("VER CLAVE");
                verClave.setTextFill(Color.web("#00f2fe"));

                // El motor solo reanuda con el foco puesto y con algo que ocultar.
                if (!contrasenaReal.isEmpty() && passField.isFocused()) {
                    motorGlitch.play();
                }
            }
        });

        HBox filaClave = new HBox(10, passField, verClave);
        filaClave.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS);

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
            Usuario usuario;
            try {
                usuario = dao.iniciarSesion(email, contrasenaReal);
            } catch (java.sql.SQLException ex) {
                // Fallo de infraestructura: lo distinguimos de credenciales inválidas.
                if (sfxCancel != null) sfxCancel.play();
                errorLbl.setTextFill(Color.web("#ff3366"));
                errorLbl.setText("// DB_CONNECTION_ERROR");
                loginBtn.setDisable(false);
                ex.printStackTrace();
                return;
            }

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

        root.getChildren().addAll(titulo, emailField, filaClave, btnBox, errorLbl);

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