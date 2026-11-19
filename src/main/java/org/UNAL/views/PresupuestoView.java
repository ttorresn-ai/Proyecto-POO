package org.UNAL.views;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.UNAL.DAO.CategoriaDAO;
import org.UNAL.DAO.PresupuestoDAO;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Categoria;
import org.UNAL.modelo.Presupuesto;
import org.UNAL.modelo.Usuario;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulo de presupuestos por categoría y periodo mensual.
 *
 * <p>Cada fila muestra el límite, lo gastado y una barra que cambia de color
 * al acercarse o superar el límite.
 */
public class PresupuestoView extends ModuloAutenticado {

    private static final String[] MESES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    // Cachés pobladas una vez por recarga: evitan consultas en cada repintado.
    private Map<Integer, String> cacheNombres = new HashMap<>();
    private Map<Integer, Double> cacheGasto = new HashMap<>();

    private TableView<Presupuesto> tabla;
    private ComboBox<Categoria> comboCategoria;
    private TextField montoField;
    private Label mensaje;
    private Label lblResumen;

    public PresupuestoView(Usuario usuario) {
        super(usuario);
    }

    @Override
    protected Scene construirEscena() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + CyberTheme.FONDO + ";");

        root.setTop(construirCabecera());
        root.setCenter(construirPanelTabla());
        root.setRight(construirPanelFormulario());

        return new Scene(root, 960, 580);
    }

    private VBox construirCabecera() {
        VBox cabecera = new VBox(6);
        cabecera.setPadding(new Insets(20, 25, 15, 25));

        HBox fila = new HBox(15);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox textos = new VBox(2);
        textos.getChildren().add(CyberTheme.titulo("BUDGET_CORES", CyberTheme.AMBAR));
        textos.getChildren().add(CyberTheme.caption(
                "// PRESUPUESTOS DEL OPERADOR // " + periodoActual(), CyberTheme.GRIS_OSCURO));

        javafx.scene.layout.Region separador = new javafx.scene.layout.Region();
        HBox.setHgrow(separador, javafx.scene.layout.Priority.ALWAYS);

        lblResumen = CyberTheme.caption("// SIN DATOS", CyberTheme.VERDE);

        fila.getChildren().addAll(textos, separador, lblResumen);
        cabecera.getChildren().add(fila);

        return cabecera;
    }

    private VBox construirPanelTabla() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 25, 20, 25));

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("SIN PRESUPUESTOS CONFIGURADOS"));
        tabla.getStyleClass().add("tabla-neon");

        TableColumn<Presupuesto, String> catCol = new TableColumn<>("CATEGORIA");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(
                cacheNombres.getOrDefault(d.getValue().getCategoriaId(), "ID " + d.getValue().getCategoriaId())));
        catCol.setPrefWidth(32);

        TableColumn<Presupuesto, String> periodoCol = new TableColumn<>("PERIODO");
        periodoCol.setCellValueFactory(d -> new SimpleStringProperty(
                MESES[d.getValue().getMes() - 1] + " " + d.getValue().getAnio()));
        periodoCol.setPrefWidth(15);

        TableColumn<Presupuesto, Number> limiteCol = new TableColumn<>("LIMITE");
        limiteCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getMontoLimite()));
        limiteCol.setPrefWidth(18);

        TableColumn<Presupuesto, Number> gastadoCol = new TableColumn<>("GASTADO");
        gastadoCol.setCellValueFactory(d -> new SimpleDoubleProperty(
                cacheGasto.getOrDefault(claveDe(d.getValue()), 0.0)));
        gastadoCol.setPrefWidth(18);

        TableColumn<Presupuesto, Number> progresoCol = new TableColumn<>("PROGRESO");
        progresoCol.setCellValueFactory(d -> {
            double limite = d.getValue().getMontoLimite();
            double gastado = cacheGasto.getOrDefault(claveDe(d.getValue()), 0.0);
            return new SimpleDoubleProperty(limite > 0 ? Math.min(gastado / limite, 1.0) : 0);
        });
        progresoCol.setPrefWidth(17);
        progresoCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar barra = new ProgressBar(0);

            @Override
            protected void updateItem(Number valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setGraphic(null);
                    return;
                }
                double uso = valor.doubleValue();
                barra.setProgress(uso);
                String color = uso >= 1.0 ? CyberTheme.ROJO
                        : (uso >= 0.8 ? CyberTheme.AMBAR : CyberTheme.VERDE);
                barra.setStyle("-fx-accent: " + color + ";");
                setGraphic(barra);
            }
        });

        tabla.getColumns().addAll(catCol, periodoCol, limiteCol, gastadoCol, progresoCol);

        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(tabla);
        scroller.setFitToWidth(true);
        scroller.setStyle(CyberTheme.scroll(CyberTheme.AMBAR) + " -fx-background-color: transparent;");
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroller, javafx.scene.layout.Priority.ALWAYS);
        panel.getChildren().add(scroller);

        return panel;
    }

    private VBox construirPanelFormulario() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(0, 25, 25, 25));
        panel.setPrefWidth(310);
        panel.setStyle(CyberTheme.panel(CyberTheme.AMBAR) + " -fx-border-width: 0 0 0 1.5;");

        Label titulo = CyberTheme.etiqueta("NUEVO_PRESUPUESTO", CyberTheme.AMBAR, 13, FontWeight.BOLD);
        titulo.setEffect(CyberTheme.halo(0.6));

        comboCategoria = new ComboBox<>();
        comboCategoria.setPromptText("SELECCIONA_CATEGORIA");
        comboCategoria.setMaxWidth(Double.MAX_VALUE);
        comboCategoria.setStyle(CyberTheme.campo(CyberTheme.AMBAR));
        comboCategoria.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categoria c) {
                return c == null ? "" : c.getNombre();
            }

            @Override
            public Categoria fromString(String s) {
                return null;
            }
        });

        montoField = new TextField();
        montoField.setPromptText("MONTO_LIMITE (ej: 50000)");
        montoField.setMaxWidth(Double.MAX_VALUE);
        montoField.setStyle(CyberTheme.campo(CyberTheme.CIAN));

        Label periodo = CyberTheme.caption("// PERIODO_ACTIVO: " + periodoActual(), CyberTheme.GRIS_OSCURO);

        Button btnGuardar = CyberTheme.boton("GUARDAR_PRESUPUESTO", CyberTheme.CIAN,
                this::guardar, 190, 38);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);

        Button btnEliminar = CyberTheme.boton("ELIMINAR_SELECCIONADO", CyberTheme.ROJO,
                this::eliminar, 190, 34);
        btnEliminar.setMaxWidth(Double.MAX_VALUE);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,165,0,0.25);");

        Button btnVolver = CyberTheme.boton("← VOLVER_AL_MENU", CyberTheme.CIAN, this::volver, 190, 34);
        btnVolver.setId("btnVolver");
        btnVolver.setMaxWidth(Double.MAX_VALUE);

        mensaje = new Label();
        mensaje.setWrapText(true);
        mensaje.setFont(Font.font("Courier New", 11));
        mensaje.setMinHeight(30);

        panel.getChildren().addAll(
                titulo,
                CyberTheme.caption("// SI YA EXISTE, SE ACTUALIZA", CyberTheme.GRIS_OSCURO),
                new Separator(),
                comboCategoria, montoField, periodo,
                btnGuardar, btnEliminar,
                sep,
                btnVolver, mensaje
        );

        return panel;
    }

    // -----------------------------------------------------------------------
    //  Lógica
    // -----------------------------------------------------------------------

    private void guardar() {
        mensaje.setText("");
        try {
            Categoria categoria = comboCategoria.getValue();
            if (categoria == null) {
                throw new ValidacionException("Debes seleccionar una categoría.");
            }

            double monto = Validador.parsearMonto(montoField.getText());
            Validador.validarMonto(monto);

            YearMonth periodo = YearMonth.now();
            Presupuesto p = new Presupuesto("0", monto, periodo.getMonthValue(), periodo.getYear());

            if (presupuestoDAO.guardarPresupuesto(p, usuarioId(), Integer.parseInt(categoria.getId()))) {
                mostrarInfo("Presupuesto guardado.");
                montoField.clear();
                comboCategoria.setValue(null);
                cargarDatos();
            } else {
                mostrarError("No se pudo guardar el presupuesto.");
            }
        } catch (ValidacionException | SQLException e) {
            mostrarError(e.getMessage());
        }
    }

    private void eliminar() {
        Presupuesto seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mostrarError("Selecciona un presupuesto de la tabla.");
            return;
        }

        if (!CyberAlert.confirmar("¿Eliminar el presupuesto de "
                + periodoDe(YearMonth.of(seleccion.getAnio(), seleccion.getMes())) + "?")) {
            return;
        }

        try {
            if (presupuestoDAO.eliminarPresupuesto(Integer.parseInt(seleccion.getId()))) {
                mostrarInfo("Presupuesto eliminado.");
                cargarDatos();
            } else {
                mostrarError("No se pudo eliminar el presupuesto.");
            }
        } catch (SQLException e) {
            mostrarError(e.getMessage());
        }
    }

    /**
     * Recarga tabla y cachés.
     *
     * <p>Dos consultas en total: una para los presupuestos y otra única que
     * calcula el gasto de todos a la vez. Antes eran 1 + N consultas.
     */
    private void cargarDatos() {
        try {
            List<Presupuesto> presupuestos = presupuestoDAO.obtenerPresupuestosPorUsuario(usuarioId());

            List<Integer> ids = presupuestos.stream()
                    .map(Presupuesto::getCategoriaId)
                    .distinct()
                    .toList();

            cacheNombres = categoriaDAO.mapearNombres(ids);
            cacheGasto = presupuestoDAO.obtenerGastoPorPresupuesto(
                    usuarioId(), YearMonth.now().getYear(), YearMonth.now().getMonthValue());

            comboCategoria.setItems(FXCollections.observableArrayList(categoriaDAO.obtenerTodas()));

            tabla.setItems(FXCollections.observableArrayList(presupuestos));
            tabla.refresh();

            actualizarResumen(presupuestos);
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los presupuestos: " + e.getMessage());
        }
    }

    /** Resumen del periodo actual: total límite vs total gastado. */
    private void actualizarResumen(List<Presupuesto> presupuestos) {
        YearMonth ahora = YearMonth.now();
        double limite = 0;
        double gastado = 0;

        for (Presupuesto p : presupuestos) {
            if (p.getAnio() == ahora.getYear() && p.getMes() == ahora.getMonthValue()) {
                limite += p.getMontoLimite();
                gastado += cacheGasto.getOrDefault(claveDe(p), 0.0);
            }
        }

        if (presupuestos.isEmpty()) {
            lblResumen.setText("// SIN DATOS");
            lblResumen.setTextFill(Color.web(CyberTheme.GRIS_OSCURO));
            return;
        }

        double uso = limite > 0 ? gastado / limite : 0;
        String color = uso >= 1.0 ? CyberTheme.ROJO : (uso >= 0.8 ? CyberTheme.AMBAR : CyberTheme.VERDE);

        lblResumen.setText(String.format("// LIMITE $%,.0f  |  GASTO $%,.0f  (%.0f%%)",
                limite, gastado, uso * 100));
        lblResumen.setTextFill(Color.web(color));
    }

    private static Integer claveDe(Presupuesto p) {
        return Integer.parseInt(p.getId());
    }

    private void mostrarError(String texto) {
        mensaje.setTextFill(Color.web(CyberTheme.ROJO));
        mensaje.setText("// " + (texto == null || texto.isBlank() ? "ERROR DESCONOCIDO" : texto.toUpperCase()));
        FadeTransition ft = new FadeTransition(Duration.millis(280), mensaje);
        ft.setFromValue(0.25);
        ft.setToValue(1.0);
        ft.play();
    }

    private void mostrarInfo(String texto) {
        mensaje.setTextFill(Color.web(CyberTheme.VERDE));
        mensaje.setText("// " + texto.toUpperCase());
    }

    private static String periodoActual() {
        YearMonth a = YearMonth.now();
        return MESES[a.getMonthValue() - 1] + " " + a.getYear();
    }

    private static String periodoDe(YearMonth periodo) {
        return MESES[periodo.getMonthValue() - 1] + " " + periodo.getYear();
    }
}