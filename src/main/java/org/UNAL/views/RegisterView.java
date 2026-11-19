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
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import javafx.util.Duration;
import java.net.URL;
import java.util.Random;

public class RegisterView {

    private static URL recurso(String ruta) {
        return Thread.currentThread().getContextClassLoader().getResource(ruta);
    }

    private static AudioClip sfxEscribir;
    private static AudioClip sfxBorrar;
    private static AudioClip sfxPress;
    private static AudioClip sfxCancel;

    private String contrasenaReal = "";
    /**
     * Cuando es {@code true} el campo de clave deja de enmascararse y muestra el
     * texto real. El registro siempre usa {@link #contrasenaReal}, de modo que el
     * modo visible no altera lo que se envia al DAO.
     */
    private boolean contrasenaVisible = false;
    private final String[] simbolosAlienigenas = {"▰", "▱", "◤", "◢", "█", "⚡", "☣", "Ω", "Ψ", "Ξ", "Ϡ", "⚙", "☍"};
    private final Random random = new Random();
    private Timeline glitchTimeline;

    public VBox getContainer(Runnable onRegistroExitoso, Runnable onVolver) {

        if (sfxEscribir == null) {
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
            } catch (Exception e) {}
        }

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setMaxSize(400, 520);
        root.setPadding(new Insets(35));

        // BORDE VERDE NEÓN
        root.setStyle("-fx-background-color: rgba(5, 8, 18, 0.95); " +
                "-fx-border-color: #13C784; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(19,199,132,0.3), 20, 0, 0, 0);");

        Label titulo = new Label("REGISTER_SYSTEM_UNIT");
        titulo.setFont(Font.font("Courier New", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#13C784")); // VERDE NEÓN
        titulo.setEffect(new Glow(0.8));

        // ESTILO DE CAMPOS VERDE NEÓN
        String estiloCampos = "-fx-background-color: rgba(255,255,255,0.03); " +
                "-fx-text-fill: #13C784; " +
                "-fx-font-family: 'Courier New'; " +
                "-fx-font-size: 13px; " +
                "-fx-prompt-text-fill: rgba(19, 199, 132, 0.4); " +
                "-fx-border-color: rgba(19, 199, 132, 0.25); " +
                "-fx-border-radius: 4; -fx-padding: 10;";

        TextField nombreField = new TextField();
        nombreField.setPromptText("ENTER_USER_NAME");
        nombreField.setStyle(estiloCampos);

        TextField emailField = new TextField();
        emailField.setPromptText("ENTER_EMAIL_ADDRESS");
        emailField.setStyle(estiloCampos);

        TextField passField = new TextField();
        passField.setPromptText("CREATE_ACCESS_KEY");
        passField.setStyle(estiloCampos);

        Label mensajeLbl = new Label();
        mensajeLbl.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        mensajeLbl.setWrapText(true);

        this.contrasenaReal = "";

        // MOTOR DE MUTACIÓN DINÁMICA
        glitchTimeline = new Timeline(new KeyFrame(Duration.millis(120), event -> {
            if (!contrasenaReal.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < contrasenaReal.length(); i++) {
                    sb.append(simbolosAlienigenas[random.nextInt(simbolosAlienigenas.length)]);
                }
                passField.setText(sb.toString());
                passField.positionCaret(sb.length());
            }
        }));
        glitchTimeline.setCycleCount(Animation.INDEFINITE);
        glitchTimeline.play();

        // CAPTURA DE TECLADO (EVITA INVERSIÓN)
        passField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            // En modo visible no interceptamos nada: el campo se comporta como
            // un TextField normal y el listener de texto mantiene contrasenaReal.
            if (contrasenaVisible) {
                if (sfxEscribir != null) sfxEscribir.play();
                return;
            }

            String c = event.getCharacter();
            event.consume();
            if (c.equals("\b")) {
                if (!contrasenaReal.isEmpty()) {
                    contrasenaReal = contrasenaReal.substring(0, contrasenaReal.length() - 1);
                    if (sfxBorrar != null) sfxBorrar.play();
                }
            } else if (!c.isEmpty() && !c.equals("\r") && !c.equals("\n")) {
                contrasenaReal += c;
                if (sfxEscribir != null) sfxEscribir.play();
            }
        });

        // =====================================================================
        // VISUALIZAR / OCULTAR LA CLAVE
        // =====================================================================
        // El motor de glitch llama a setText() ocho veces por segundo, asi que el
        // listener solo sincroniza contrasenaReal mientras la clave esta visible:
        // en modo enmascarado copiaria el texto alienigena en su lugar.
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
                glitchTimeline.pause();
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

                // El motor se reanuda solo si hay algo que ocultar.
                if (!contrasenaReal.isEmpty()) {
                    glitchTimeline.play();
                }
            }
        });

        HBox filaClave = new HBox(10, passField, verClave);
        filaClave.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS);

        String estiloBtn = "-fx-background-color: transparent; -fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-width: 1; -fx-border-radius: 4; -fx-cursor: hand;";

        Button registrarBtn = new Button("EXECUTE");
        registrarBtn.setPrefWidth(110);
        registrarBtn.setPrefHeight(35);
        registrarBtn.setStyle(estiloBtn + "-fx-border-color: #13C784; -fx-text-fill: #13C784;");

        registrarBtn.setOnAction(e -> {
            try {
                Validador.validarNombre(nombreField.getText());
                Validador.validarEmail(emailField.getText());
                if (contrasenaReal.isEmpty()) {
                    throw new ValidacionException("La clave es obligatoria.");
                }

                Usuario nuevo = new Usuario("0", nombreField.getText(), emailField.getText(), contrasenaReal);
                if (new UsuarioDAO().registrarUsuario(nuevo)) {
                    glitchTimeline.stop();
                    CyberAlert.mostrarInfo("Unidad creada. Ya puedes iniciar sesión.");
                    if (onRegistroExitoso != null) onRegistroExitoso.run();
                } else {
                    mensajeLbl.setTextFill(Color.web("#ff3366"));
                    mensajeLbl.setText("// NO SE PUDO REGISTRAR");
                }
            } catch (ValidacionException ex) {
                mensajeLbl.setTextFill(Color.web("#ff3366"));
                mensajeLbl.setText("// " + ex.getMessage().toUpperCase());
            } catch (java.sql.SQLException ex) {
                // MySQL devuelve 1062 cuando el email ya está registrado.
                String detalle = ex.getMessage() != null && ex.getMessage().contains("Duplicate")
                        ? "Ese correo ya está registrado."
                        : "Error de base de datos: " + ex.getMessage();
                mensajeLbl.setTextFill(Color.web("#ff3366"));
                mensajeLbl.setText("// " + detalle.toUpperCase());
                ex.printStackTrace();
            }
        });

        Button volverBtn = new Button("BACK");
        volverBtn.setPrefWidth(100);
        volverBtn.setStyle(estiloBtn + "-fx-border-color: #ff3366; -fx-text-fill: #ff3366;");
        volverBtn.setOnAction(e -> {
            glitchTimeline.stop();
            if (onVolver != null) onVolver.run();
        });

        HBox btnBox = new HBox(15, volverBtn, registrarBtn);
        btnBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titulo, nombreField, emailField, filaClave, btnBox, mensajeLbl);
        return root;
    }
}