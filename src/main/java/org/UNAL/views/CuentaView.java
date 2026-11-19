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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.UNAL.DAO.CuentaDAO;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Cuenta;
import org.UNAL.modelo.Usuario;

import java.sql.SQLException;
import java.util.List;

/**
 * Módulo de cuentas: alta, listado y eliminación.
 */
public class CuentaView extends ModuloAutenticado {

    private static final List<String> TIPOS_CUENTA =
            List.of("Ahorros", "Efectivo", "Tarjeta de Crédito");

    private final CuentaDAO cuentaDAO = new CuentaDAO();

    private TableView<Cuenta> tabla;
    private TextField txtNombre;
    private ComboBox<String> comboTipo;
    private TextField txtSaldo;
    private Label mensaje;
    private Label lblContador;
    private Button btnGuardar;

    public CuentaView(Usuario usuario) {
        super(usuario);
    }

    @Override
    protected Scene construirEscena() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + CyberTheme.FONDO + ";");

        root.setTop(construirCabecera());
        root.setCenter(construirPanelTabla());
        root.setRight(construirPanelFormulario());

        return new Scene(root, 900, 560);
    }

    // -----------------------------------------------------------------------
    //  Cabecera
    // -----------------------------------------------------------------------

    private VBox construirCabecera() {
        VBox cabecera = new VBox(6);
        cabecera.setPadding(new Insets(20, 25, 15, 25));

        HBox fila = new HBox(15);
        fila.setAlignment(Pos.CENTER_LEFT);

        VBox textos = new VBox(2);
        textos.getChildren().add(CyberTheme.titulo("TACTICAL_ACCOUNTS", CyberTheme.CIAN));
        textos.getChildren().add(CyberTheme.caption("// GESTION DE CUENTAS DEL OPERADOR", CyberTheme.GRIS_OSCURO));

        RegionSeparador separador = new RegionSeparador();
        HBox.setHgrow(separador, javafx.scene.layout.Priority.ALWAYS);

        lblContador = CyberTheme.caption("// 0 NODES", CyberTheme.VERDE);

        fila.getChildren().addAll(textos, separador, lblContador);
        cabecera.getChildren().add(fila);

        return cabecera;
    }

    /** Espaciador elástico para empujar elementos hacia los extremos. */
    private static final class RegionSeparador extends javafx.scene.layout.Region {
        private RegionSeparador() {
            setMinWidth(0);
        }
    }

    // -----------------------------------------------------------------------
    //  Panel de la tabla
    // -----------------------------------------------------------------------

    private VBox construirPanelTabla() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 25, 20, 25));
        panel.setStyle("-fx-background-color: transparent;");

        tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tabla.getStyleClass().add("tabla-neon");
        tabla.setPlaceholder(new Label("SIN CUENTAS REGISTRADAS // USA EL PANEL DERECHO"));
        tabla.setPrefHeight(9999); // crece dentro del ScrollPane

        tabla.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                eliminarCuentaSeleccionada();
            }
        });

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Cuenta, String> colNombre = new TableColumn<>("NOMBRE");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colNombre.setPrefWidth(45);

        TableColumn<Cuenta, String> colTipo = new TableColumn<>("TIPO");
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoCuenta()));
        colTipo.setPrefWidth(25);

        TableColumn<Cuenta, Number> colSaldo = new TableColumn<>("SALDO");
        colSaldo.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getBalanceActual()));
        colSaldo.setPrefWidth(30);
        colSaldo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setText(null);
                    setStyle("");
                } else {
                    double saldo = valor.doubleValue();
                    setText(String.format("$%,.2f", saldo));
                    // Verde si hay saldo, rojo si está en negativo.
                    setStyle("-fx-text-fill: " + (saldo < 0 ? CyberTheme.ROJO : CyberTheme.VERDE)
                            + "; -fx-font-family: " + CyberTheme.FUENTE + ";");
                }
            }
        });

        tabla.getColumns().addAll(colNombre, colTipo, colSaldo);

        // ScrollPane para que la tabla pueda desbordar en ventanas pequeñas.
        javafx.scene.control.ScrollPane scroller = new javafx.scene.control.ScrollPane(tabla);
        scroller.setFitToWidth(true);
        scroller.setStyle(CyberTheme.scroll(CyberTheme.CIAN)
                + " -fx-background-color: transparent;");
        scroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        javafx.scene.layout.VBox.setVgrow(scroller, javafx.scene.layout.Priority.ALWAYS);
        panel.getChildren().add(scroller);

        return panel;
    }

    // -----------------------------------------------------------------------
    //  Panel del formulario
    // -----------------------------------------------------------------------

    private VBox construirPanelFormulario() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(0, 25, 25, 25));
        panel.setPrefWidth(320);
        panel.setStyle(CyberTheme.panel(CyberTheme.VERDE) + " -fx-border-width: 0 0 0 1.5;");

        Label titulo = CyberTheme.etiqueta("NUEVA_CUENTA", CyberTheme.VERDE, 13, javafx.scene.text.FontWeight.BOLD);
        titulo.setEffect(CyberTheme.halo(0.6));

        Label subtitulo = CyberTheme.caption("// REGISTRO DE NODO FINANCIERO", CyberTheme.GRIS_OSCURO);

        txtNombre = new TextField();
        txtNombre.setPromptText("NOMBRE (ej: Ahorros Bancolombia)");
        txtNombre.setMaxWidth(Double.MAX_VALUE);
        txtNombre.setStyle(CyberTheme.campo(CyberTheme.VERDE));

        comboTipo = new ComboBox<>(FXCollections.observableArrayList(TIPOS_CUENTA));
        comboTipo.setPromptText("TIPO_DE_CUENTA");
        comboTipo.setMaxWidth(Double.MAX_VALUE);
        comboTipo.setStyle(CyberTheme.campo(CyberTheme.VERDE));

        txtSaldo = new TextField();
        txtSaldo.setPromptText("SALDO_INICIAL");
        txtSaldo.setMaxWidth(Double.MAX_VALUE);
        txtSaldo.setStyle(CyberTheme.campo(CyberTheme.VERDE));

        btnGuardar = CyberTheme.boton("CREAR_CUENTA", CyberTheme.VERDE,
                this::crearCuenta, 190, 38);
        btnGuardar.setMaxWidth(Double.MAX_VALUE);

        Button btnEliminar = CyberTheme.boton("ELIMINAR_SELECCIONADA", CyberTheme.ROJO,
                this::eliminarCuentaSeleccionada, 190, 34);
        btnEliminar.setMaxWidth(Double.MAX_VALUE);

        mensaje = new Label();
        mensaje.setWrapText(true);
        mensaje.setFont(javafx.scene.text.Font.font("Courier New", 11));
        mensaje.setMinHeight(30);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(19,199,132,0.25);");

        Button btnVolver = CyberTheme.boton("← VOLVER_AL_MENU", CyberTheme.CIAN,
                this::volver, 190, 34);
        btnVolver.setId("btnVolver");
        btnVolver.setMaxWidth(Double.MAX_VALUE);

        panel.getChildren().addAll(
                titulo, subtitulo,
                new Separator(),
                CyberTheme.etiqueta("IDENTIFICACION", CyberTheme.GRIS_OSCURO, 9, javafx.scene.text.FontWeight.BOLD),
                txtNombre, comboTipo,
                CyberTheme.etiqueta("FONDOS", CyberTheme.GRIS_OSCURO, 9, javafx.scene.text.FontWeight.BOLD),
                txtSaldo,
                btnGuardar, btnEliminar,
                sep,
                btnVolver, mensaje
        );

        return panel;
    }

    // -----------------------------------------------------------------------
    //  Lógica
    // -----------------------------------------------------------------------

    private void crearCuenta() {
        limpiarMensaje();
        try {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                throw new ValidacionException("El nombre de la cuenta es obligatorio.");
            }
            if (comboTipo.getValue() == null) {
                throw new ValidacionException("Selecciona el tipo de cuenta.");
            }

            double saldo = Validador.parsearMonto(txtSaldo.getText());
            if (saldo < 0) {
                throw new ValidacionException("El saldo inicial no puede ser negativo.");
            }

            Cuenta nueva = new Cuenta("0", nombre, comboTipo.getValue(), saldo);
            if (cuentaDAO.crearCuenta(nueva, usuarioId())) {
                mostrarInfo("Cuenta creada correctamente.");
                txtNombre.clear();
                txtSaldo.clear();
                comboTipo.setValue(null);
                cargarCuentas();
            } else {
                mostrarError("No se pudo crear la cuenta.");
            }
        } catch (ValidacionException | SQLException e) {
            mostrarError(e.getMessage());
        }
    }

    private void eliminarCuentaSeleccionada() {
        Cuenta seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mostrarError("Selecciona una cuenta de la tabla.");
            return;
        }

        boolean confirmado = CyberAlert.confirmar(
                "¿Eliminar la cuenta \"" + seleccion.getNombre() + "\"?\n"
                + "Se borrarán también sus transacciones asociadas.");
        if (!confirmado) {
            return;
        }

        try {
            if (cuentaDAO.eliminarCuenta(Integer.parseInt(seleccion.getId()))) {
                mostrarInfo("Cuenta eliminada.");
                cargarCuentas();
            } else {
                mostrarError("No se pudo eliminar la cuenta.");
            }
        } catch (SQLException e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarCuentas() {
        try {
            List<Cuenta> lista = cuentaDAO.obtenerCuentasPorUsuario(usuarioId());
            tabla.setItems(FXCollections.observableArrayList(lista));
            if (lblContador != null) {
                lblContador.setText("// " + lista.size() + (lista.size() == 1 ? " NODE" : " NODES"));
            }
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar las cuentas: " + e.getMessage());
        }
    }

    private void limpiarMensaje() {
        mensaje.setText("");
    }

    private void mostrarError(String texto) {
        mensaje.setTextFill(javafx.scene.paint.Color.web(CyberTheme.ROJO));
        mensaje.setText("// " + normalizar(texto));
        parpadear(mensaje);
    }

    private void mostrarInfo(String texto) {
        mensaje.setTextFill(javafx.scene.paint.Color.web(CyberTheme.VERDE));
        mensaje.setText("// " + normalizar(texto));
    }

    private static String normalizar(String texto) {
        return texto == null || texto.isBlank() ? "ERROR DESCONOCIDO" : texto.toUpperCase();
    }

    /** Destello rojo breve para señalar un error sin chamarcar la pantalla. */
    private void parpadear(Label etiqueta) {
        FadeTransition ft = new FadeTransition(Duration.millis(280), etiqueta);
        ft.setFromValue(0.25);
        ft.setToValue(1.0);
        ft.play();
    }
}