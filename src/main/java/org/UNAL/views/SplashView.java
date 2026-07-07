package org.UNAL.views;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class SplashView {

    private final double centerX = 400;
    private final double centerY = 300;
    private MediaPlayer mediaPlayer;

    public Scene getScene(Runnable onFinished) {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: black;");

        Text letraN = crearN();
        Text nombreProyecto = crearNombreProyecto();

        root.getChildren().addAll(letraN, nombreProyecto);

        reproducirSonido();

        // Cronómetro maestro de 8 segundos exactos
        PauseTransition temporizadorSplash = new PauseTransition(Duration.seconds(8));
        temporizadorSplash.setOnFinished(ev -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        temporizadorSplash.play();

        // Animación del trazo de líneas
        Timeline dibujo = new Timeline();
        for (int i = 0; i < 360; i++) {
            final int angulo = i;
            dibujo.getKeyFrames().add(
                    new KeyFrame(
                            Duration.millis(i * 20),
                            e -> dibujarPaso(root, angulo)
                    )
            );
        }
        dibujo.play();

        // Transiciones de desvanecimiento para los textos
        FadeTransition fadeN = new FadeTransition(Duration.seconds(4), letraN);
        fadeN.setFromValue(0);
        fadeN.setToValue(1);
        fadeN.setDelay(Duration.seconds(0.5));

        FadeTransition fadeNombre = new FadeTransition(Duration.seconds(3), nombreProyecto);
        fadeNombre.setFromValue(0);
        fadeNombre.setToValue(1);
        fadeNombre.setDelay(Duration.seconds(1.5));

        fadeN.play();
        fadeNombre.play();

        return new Scene(root, 800, 600);
    }

    private void reproducirSonido() {
        try {
            URL audioUrl = getClass().getResource("/views/sonido_splash.mp3");
            if (audioUrl != null) {
                Media media = new Media(audioUrl.toString());
                this.mediaPlayer = new MediaPlayer(media);
                this.mediaPlayer.play();
            }
        } catch (Exception e) {
            // Manejo silencioso del error para mantener limpia la consola
        }
    }

    private void dibujarPaso(Pane root, int i) {
        double angle = Math.toRadians(i);

        double x0 = centerX;
        double y0 = centerY;

        double x1 = x0 + Math.cos(angle) * 100;
        double y1 = y0 + Math.sin(angle) * 100;

        double x2 = x1 + Math.cos(angle) * (i * 0.6);
        double y2 = y1 + Math.sin(angle) * (i * 0.6);

        double secondAngle = angle + Math.toRadians(765);

        double x3 = x2 + Math.cos(secondAngle) * (i * 0.4);
        double y3 = y2 + Math.sin(secondAngle) * (i * 0.4);

        Color color;
        if (i < 120) {
            color = Color.DODGERBLUE;
        } else if (i < 240) {
            color = Color.RED;
        } else {
            color = Color.GOLD;
        }

        Line l1 = new Line(x1, y1, x2, y2);
        l1.setStroke(color);
        l1.setStrokeWidth(2);

        Line l2 = new Line(x2, y2, x3, y3);
        l2.setStroke(color);
        l2.setStrokeWidth(2);

        root.getChildren().addAll(l1, l2);
    }

    private Text crearN() {
        Text n = new Text("N");
        n.setFont(Font.font("Times New Roman", 120));
        n.setFill(Color.WHITE);
        n.setLayoutX(centerX - 45);
        n.setLayoutY(centerY + 45);
        n.setOpacity(0);

        Glow glow = new Glow(0.9);
        n.setEffect(glow);

        return n;
    }

    private Text crearNombreProyecto() {
        Text nombre = new Text("NEXUS FINANCE");
        nombre.setFill(Color.WHITE);
        nombre.setFont(Font.font("Arial", 30));
        nombre.setLayoutX(280);
        nombre.setLayoutY(530);
        nombre.setOpacity(0);

        return nombre;
    }
}