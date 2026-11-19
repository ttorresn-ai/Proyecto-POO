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
import javafx.scene.control.ButtonBar;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.media.AudioClip;
import java.net.URL;

public class CyberAlert {

    private static AudioClip sfxError;

    /**
     * Diálogo de confirmación con la misma estética que los avisos de error.
     *
     * @return true si el usuario confirma la acción.
     */
    public static boolean confirmar(String mensaje) {
        Stage window = newStageBase();
        reproducirSfx();

        Button btnOk = new Button("CONFIRM");
        btnOk.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        btnOk.setPrefWidth(150);
        btnOk.setPrefHeight(30);
        btnOk.setStyle("-fx-background-color: transparent; -fx-text-fill: #00f2fe; "
                + "-fx-border-color: rgba(0, 242, 254, 0.4); -fx-border-width: 1; "
                + "-fx-border-radius: 4; -fx-cursor: hand;");

        Button btnCancelar = new Button("ABORT");
        btnCancelar.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        btnCancelar.setPrefWidth(150);
        btnCancelar.setPrefHeight(30);
        btnCancelar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff3366; "
                + "-fx-border-color: rgba(255, 51, 102, 0.4); -fx-border-width: 1; "
                + "-fx-border-radius: 4; -fx-cursor: hand;");

        // Stage no tiene setResult(): usamos un holder booleano.
        final boolean[] confirmado = {false};

        btnOk.setOnAction(e -> {
            confirmado[0] = true;
            window.close();
        });
        btnCancelar.setOnAction(e -> {
            confirmado[0] = false;
            window.close();
        });

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPadding(new Insets(10));
        buttonBar.getButtons().addAll(btnOk, btnCancelar);

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
        lblMensaje.setTextFill(Color.web("#c0d0d8"));
        lblMensaje.setWrapText(true);
        lblMensaje.setAlignment(Pos.CENTER);

        VBox layout = new VBox(15, lblMensaje, buttonBar);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: rgba(10, 3, 15, 0.98); "
                + "-fx-border-color: #00f2fe; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 4; "
                + "-fx-background-radius: 4;");

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        window.setScene(scene);
        window.showAndWait();

        return confirmado[0];
    }

    /**
     * Aviso informativo de operación exitosa, con el mismo estilo visual.
     */
    public static void mostrarInfo(String mensaje) {
        Stage window = newStageBase();

        Label lblHeader = new Label("// OPERATION_COMPLETED");
        lblHeader.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        lblHeader.setTextFill(Color.web("#13C784"));
        lblHeader.setEffect(new Glow(0.6));

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
        lblMensaje.setTextFill(Color.web("#c0d0d8"));
        lblMensaje.setWrapText(true);
        lblMensaje.setAlignment(Pos.CENTER);

        Button btnCerrar = new Button("ACKNOWLEDGE");
        btnCerrar.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        btnCerrar.setPrefWidth(180);
        btnCerrar.setPrefHeight(30);

        String estiloBtn = "-fx-background-color: transparent; -fx-text-fill: #13C784; "
                + "-fx-border-color: rgba(19, 199, 132, 0.4); -fx-border-width: 1; "
                + "-fx-border-radius: 4; -fx-cursor: hand;";
        String estiloHover = "-fx-background-color: rgba(19, 199, 132, 0.12); "
                + "-fx-text-fill: #ffffff; -fx-border-color: #13C784;";

        btnCerrar.setStyle(estiloBtn);
        btnCerrar.setOnMouseEntered(e -> btnCerrar.setStyle(estiloHover));
        btnCerrar.setOnMouseExited(e -> btnCerrar.setStyle(estiloBtn));
        btnCerrar.setOnAction(e -> window.close());

        VBox layout = new VBox(15, lblHeader, lblMensaje, btnCerrar);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: rgba(5, 20, 12, 0.98); "
                + "-fx-border-color: #13C784; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 4; "
                + "-fx-background-radius: 4; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(19,199,132,0.3), 15, 0, 0, 0);");

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        window.setScene(scene);
        window.showAndWait();
    }

    private static Stage newStageBase() {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.initStyle(StageStyle.UNDECORATED);
        window.setMinWidth(420);
        window.setMinHeight(200);
        return window;
    }

    private static void reproducirSfx() {
        if (sfxError == null) {
            try {
                URL cancelUrl = Thread.currentThread().getContextClassLoader().getResource("views/cancel.mp3");
                if (cancelUrl != null) sfxError = new AudioClip(cancelUrl.toString());
            } catch (Exception e) {
                System.err.println("Error cargando SFX alerta: " + e.getMessage());
            }
        }
        if (sfxError != null) sfxError.play();
    }

    public static void mostrarError(String mensaje) {
        reproducirSfx();

        Stage window = newStageBase();

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