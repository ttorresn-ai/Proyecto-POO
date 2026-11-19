package org.UNAL.views;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.UNAL.DAO.TransaccionDAO;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * Módulo de reportes: distribución de gastos por categoría e ingresos por origen.
 */
public class ReporteView extends ModuloAutenticado {

    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    private TabPane tabPane;
    private DatePicker dateInicio;
    private DatePicker dateFin;
    private ComboBox<String> rangoRapido;
    private Label lblIngresos;
    private Label lblGastos;
    private Label lblBalance;
    private VBox panelGastos;
    private VBox panelIngresos;

    public ReporteView(Usuario usuario) {
        super(usuario);
    }

    @Override
    protected Scene construirEscena() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + CyberTheme.FONDO + ";");

        root.setTop(construirCabecera());
        root.setCenter(construirGraficas());
        root.setBottom(construirAcciones());

        aplicarRango();
        generar();

        return new Scene(root, 980, 680);
    }

    // -----------------------------------------------------------------------
    //  Cabecera con filtros
    // -----------------------------------------------------------------------

    private VBox construirCabecera() {
        VBox cabecera = new VBox(14);
        cabecera.setPadding(new Insets(20, 25, 15, 25));

        HBox titulos = new HBox(15);
        titulos.setAlignment(Pos.CENTER_LEFT);

        VBox textos = new VBox(2);
        textos.getChildren().add(CyberTheme.titulo("DATA_ANALYTICS", CyberTheme.CIAN));
        textos.getChildren().add(CyberTheme.caption("// ANALISIS DE MOVIMIENTOS", CyberTheme.GRIS_OSCURO));
        titulos.getChildren().add(textos);

        cabecera.getChildren().add(titulos);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(0,242,254,0.2);");
        cabecera.getChildren().add(sep);

        // --- Filtros de periodo
        dateInicio = new DatePicker();
        dateFin = new DatePicker();
        dateInicio.setPrefWidth(140);
        dateFin.setPrefWidth(140);
        estiloDatePicker(dateInicio);
        estiloDatePicker(dateFin);

        rangoRapido = new ComboBox<>(FXCollections.observableArrayList(
                "Mes actual", "Mes anterior", "Últimos 3 meses", "Año actual", "Personalizado"));
        rangoRapido.setValue("Mes actual");
        rangoRapido.setPrefWidth(170);
        rangoRapido.setStyle(CyberTheme.campo(CyberTheme.CIAN));

        // Al elegir un rango predefinido movemos las fechas; "Personalizado" las deja fijas.
        rangoRapido.valueProperty().addListener((obs, previo, actual) -> {
            if (actual != null && !"Personalizado".equals(actual)) {
                aplicarRango();
                generar();
            }
        });

        HBox filtros = new HBox(10,
                CyberTheme.caption("// DESDE", CyberTheme.GRIS_OSCURO), dateInicio,
                CyberTheme.caption("// HASTA", CyberTheme.GRIS_OSCURO), dateFin,
                rangoRapido);
        filtros.setAlignment(Pos.CENTER_LEFT);
        cabecera.getChildren().add(filtros);

        return cabecera;
    }

    // -----------------------------------------------------------------------
    //  Tarjetas de totales
    // -----------------------------------------------------------------------

    private VBox construirGraficas() {
        VBox centro = new VBox(14);
        centro.setPadding(new Insets(0, 25, 10, 25));

        lblIngresos = CyberTheme.caption("// ---", CyberTheme.VERDE);
        lblGastos = CyberTheme.caption("// ---", CyberTheme.ROJO);
        lblBalance = CyberTheme.caption("// ---", CyberTheme.CIAN);

        HBox resumen = new HBox(30, lblIngresos, lblGastos, lblBalance);
        resumen.setPadding(new Insets(8, 14, 8, 14));
        resumen.setStyle(CyberTheme.panel(CyberTheme.CIAN) + " -fx-border-width: 1;");

        panelGastos = new VBox();
        panelIngresos = new VBox();
        panelGastos.setAlignment(Pos.CENTER);
        panelIngresos.setAlignment(Pos.CENTER);

        Tab tabGastos = new Tab("GASTOS_POR_CATEGORIA", panelGastos);
        Tab tabIngresos = new Tab("INGRESOS_POR_ORIGEN", panelIngresos);
        tabGastos.setClosable(false);
        tabIngresos.setClosable(false);

        tabPane = new TabPane(tabGastos, tabIngresos);
        tabPane.setStyle("-fx-background-color: " + CyberTheme.FONDO_ALT + ";"
                + " -fx-font-family: " + CyberTheme.FUENTE + ";"
                + " -fx-font-size: 11px;"
                + ".tab { -fx-background-color: rgba(0,242,254,0.03);"
                + " -fx-text-fill: " + CyberTheme.GRIS + "; -fx-cursor: hand; }"
                + ".tab:selected { -fx-background-color: rgba(0,242,254,0.14);"
                + " -fx-text-fill: " + CyberTheme.CIAN + "; }"
                + ".tab-header-area { -fx-background-color: transparent; }");

        VBox.setVgrow(tabPane, javafx.scene.layout.Priority.ALWAYS);
        centro.getChildren().addAll(resumen, tabPane);

        return centro;
    }

    private HBox construirAcciones() {
        Button btnGenerar = CyberTheme.boton("GENERAR_REPORTE", CyberTheme.CIAN, this::generar, 180, 36);

        Button btnVolver = CyberTheme.boton("← VOLVER_AL_MENU", CyberTheme.VERDE, this::volver, 180, 36);
        btnVolver.setId("btnVolver");

        HBox acciones = new HBox(12, btnGenerar, btnVolver);
        acciones.setAlignment(Pos.CENTER);
        acciones.setPadding(new Insets(5, 25, 20, 25));

        return acciones;
    }

    // -----------------------------------------------------------------------
    //  Lógica
    // -----------------------------------------------------------------------

    private void aplicarRango() {
        YearMonth actual = YearMonth.now();
        String seleccion = rangoRapido.getValue();

        if (seleccion == null || "Personalizado".equals(seleccion)) {
            if (dateInicio.getValue() == null) dateInicio.setValue(actual.atDay(1));
            if (dateFin.getValue() == null) dateFin.setValue(LocalDate.now());
            return;
        }

        switch (seleccion) {
            case "Mes anterior" -> {
                dateInicio.setValue(actual.minusMonths(1).atDay(1));
                dateFin.setValue(actual.minusMonths(1).atEndOfMonth());
            }
            case "Últimos 3 meses" -> {
                dateInicio.setValue(actual.minusMonths(2).atDay(1));
                dateFin.setValue(LocalDate.now());
            }
            case "Año actual" -> {
                dateInicio.setValue(actual.atDay(1));
                dateFin.setValue(LocalDate.now());
            }
            default -> { // Mes actual
                dateInicio.setValue(actual.atDay(1));
                dateFin.setValue(LocalDate.now());
            }
        }
    }

    private void generar() {
        LocalDate inicio = dateInicio.getValue();
        LocalDate fin = dateFin.getValue();

        if (inicio == null || fin == null) {
            CyberAlert.mostrarError("Selecciona un rango de fechas válido.");
            return;
        }
        if (inicio.isAfter(fin)) {
            CyberAlert.mostrarError("La fecha inicial no puede ser posterior a la fecha final.");
            return;
        }

        try {
            Map<String, Double> gastos = transaccionDAO.obtenerGastosPorCategoria(usuarioId(), inicio, fin);
            Map<String, Double> ingresos = transaccionDAO.obtenerIngresosPorOrigen(usuarioId(), inicio, fin);
            double[] totales = transaccionDAO.obtenerTotalesPeriodo(usuarioId(), inicio, fin);

            actualizarResumen(totales[0], totales[1], totales[0] - totales[1]);
            pintarGastos(gastos);
            pintarIngresos(ingresos);

        } catch (SQLException e) {
            CyberAlert.mostrarError("No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private void actualizarResumen(double ingresos, double gastos, double balance) {
        lblIngresos.setText(String.format("INGRESOS  $%,.2f", ingresos));
        lblGastos.setText(String.format("GASTOS  $%,.2f", gastos));
        lblBalance.setText(String.format("BALANCE  $%,.2f", balance));
        lblBalance.setTextFill(Color.web(balance >= 0 ? CyberTheme.VERDE : CyberTheme.ROJO));
    }

    private void pintarGastos(Map<String, Double> datos) {
        panelGastos.getChildren().clear();

        if (datos.isEmpty()) {
            panelGastos.getChildren().add(textoVacio("SIN GASTOS EN EL PERIODO"));
            return;
        }

        PieChart chart = new PieChart();
        datos.forEach((categoria, monto) -> chart.getData().add(new PieChart.Data(categoria, monto)));
        chart.setTitle("DISTRIBUCIÓN DE GASTOS");
        chart.setLegendSide(javafx.geometry.Side.RIGHT);
        chart.setStyle(estiloGrafica());
        panelGastos.getChildren().add(chart);
    }

    private void pintarIngresos(Map<String, Double> datos) {
        panelIngresos.getChildren().clear();

        if (datos.isEmpty()) {
            panelIngresos.getChildren().add(textoVacio("SIN INGRESOS EN EL PERIODO"));
            return;
        }

        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("ORIGEN");
        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("MONTO");

        BarChart<String, Number> chart = new BarChart<>(ejeX, ejeY);
        chart.setTitle("INGRESOS POR ORIGEN");
        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(entry.getKey());
            serie.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            chart.getData().add(serie);
        }
        chart.setStyle(estiloGrafica());
        panelIngresos.getChildren().add(chart);
    }

    private String estiloGrafica() {
        return "-fx-font-family: " + CyberTheme.FUENTE + ";"
                + " -fx-font-size: 10px;"
                + " -fx-background-color: transparent;"
                + " -fx-plot-background-color: transparent;"
                + " -fx-text-background-color: transparent;";
    }

    private Label textoVacio(String texto) {
        return CyberTheme.etiqueta(texto, CyberTheme.GRIS_OSCURO, 13, FontWeight.BOLD);
    }

    private void estiloDatePicker(DatePicker dp) {
        dp.setStyle("-fx-background-color: rgba(255,255,255,0.03);"
                + " -fx-font-family: " + CyberTheme.FUENTE + ";"
                + " -fx-font-size: 11px;"
                + " -fx-text-fill: " + CyberTheme.CIAN + ";"
                + " -fx-border-color: rgba(0,242,254,0.28);"
                + " -fx-border-radius: 4;"
                + " .date-picker-popup { -fx-background-color: " + CyberTheme.FONDO_ALT + "; "
                + "-fx-border-color: " + CyberTheme.CIAN + "; }");
    }
}