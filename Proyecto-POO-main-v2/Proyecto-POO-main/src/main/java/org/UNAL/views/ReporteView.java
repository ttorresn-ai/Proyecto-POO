package org.UNAL.views;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.UNAL.DAO.TransaccionDAO;
import org.UNAL.modelo.Usuario;
import java.time.LocalDate;
import java.util.Map;

public class ReporteView {
    private final Usuario usuario;
    private final TransaccionDAO tDAO = new TransaccionDAO();

    public ReporteView(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene getScene(Runnable onVolver) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Label titulo = new Label("Reporte de Gastos por Categoría");
        titulo.setTextFill(Color.WHITE);
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Selectores de fecha
        HBox datePickers = new HBox(15);
        datePickers.setAlignment(Pos.CENTER);
        DatePicker start = new DatePicker(LocalDate.now().minusMonths(1));
        DatePicker end = new DatePicker(LocalDate.now());
        Button btnGenerar = new Button("Generar Gráfica");
        datePickers.getChildren().addAll(new Label("Desde:"), start, new Label("Hasta:"), end, btnGenerar);

        // Contenedor de la gráfica
        StackPane chartContainer = new StackPane();
        chartContainer.setPrefHeight(400);

        btnGenerar.setOnAction(e -> {
            chartContainer.getChildren().clear();
            // Lógica para obtener datos: debe existir un método en tDAO que devuelva Map<Categoria, SumaMonto>
            Map<String, Double> datos = tDAO.obtenerGastosPorCategoria(Integer.parseInt(usuario.getId()), start.getValue(), end.getValue());

            PieChart chart = new PieChart();
            datos.forEach((cat, monto) -> chart.getData().add(new PieChart.Data(cat, monto)));
            chart.setTitle("Distribución de Gastos");
            chartContainer.getChildren().add(chart);
        });

        Button btnVolver = new Button("← Volver");
        btnVolver.setOnAction(e -> onVolver.run());

        root.getChildren().addAll(titulo, datePickers, chartContainer, btnVolver);
        return new Scene(root, 800, 600);
    }
}