package org.UNAL.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.media.AudioClip;
import java.net.URL;

public class CyberAlert {

    private static AudioClip sfxError;

    public static void mostrarError(String mensaje) {
        // Carga perezosa del sonido de fallo del sistema
        if (sfxError == null) {
            try {
                URL cancelUrl = Thread.currentThread().getContextClassLoader().getResource("views/cancel.mp3");
                if (cancelUrl != null) sfxError = new AudioClip(cancelUrl.toString());
            } catch (Exception e) {
                System.err.println("Error cargando SFX alerta: " + e.getMessage());
            }
        }

        if (sfxError != null) {
            sfxError.play();
        }

        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana de atrás
        window.initStyle(StageStyle.UNDECORATED); // Quita los botones clásicos de Windows/Mac
        window.setMinWidth(380);
        window.setMinHeight(180);

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: rgba(10, 3, 15, 0.98); " +
                "-fx-border-color: #ff3366; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(255,51,102,0.3), 15, 0, 0, 0);");

        Label lblHeader = new Label("// SYSTEM_CRITICAL_EXCEPTION");
        lblHeader.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        lblHeader.setTextFill(Color.web("#ff3366"));
        lblHeader.setEffect(new Glow(0.6));

        Label lblMensaje = new Label(mensaje.toUpperCase());
        lblMensaje.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
        lblMensaje.setTextFill(Color.web("#c0d0d8"));
        lblMensaje.setWrapText(true);
        lblMensaje.setAlignment(Pos.CENTER);

        Button btnCerrar = new Button("ACKNOWLEDGE_ERROR");
        btnCerrar.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        btnCerrar.setPrefWidth(180);
        btnCerrar.setPrefHeight(30);

        String estiloBtn = "-fx-background-color: transparent; -fx-text-fill: #ff3366; -fx-border-color: rgba(255, 51, 102, 0.4); -fx-border-width: 1; -fx-border-radius: 4; -fx-cursor: hand;";
        String estiloHover = "-fx-background-color: rgba(255, 51, 102, 0.12); -fx-text-fill: #ffffff; -fx-border-color: #ff3366;";

        btnCerrar.setStyle(estiloBtn);
        btnCerrar.setOnMouseEntered(e -> btnCerrar.setStyle(estiloHover));
        btnCerrar.setOnMouseExited(e -> btnCerrar.setStyle(estiloBtn));
        btnCerrar.setOnAction(e -> window.close());

        layout.getChildren().addAll(lblHeader, lblMensaje, btnCerrar);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        window.setScene(scene);
        window.showAndWait(); // Pausa la ejecución del programa hasta que el usuario la cierre
    }
}