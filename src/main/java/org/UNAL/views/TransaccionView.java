package org.UNAL.views;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.UNAL.DAO.CategoriaDAO;
import org.UNAL.DAO.CuentaDAO;
import org.UNAL.DAO.TransaccionDAO;
import org.UNAL.config.ValidacionException;
import org.UNAL.config.Validador;
import org.UNAL.modelo.Categoria;
import org.UNAL.modelo.Cuenta;
import org.UNAL.modelo.Gasto;
import org.UNAL.modelo.Ingreso;
import org.UNAL.modelo.Transaccion;
import org.UNAL.modelo.Usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Módulo de registro de ingresos y gastos.
 */
public class TransaccionView extends ModuloAutenticado {

    private final boolean esIngreso;
    private final String color;

    private final CuentaDAO cuentaDAO = new CuentaDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    private ComboBox<Cuenta> comboCuentas;
    private ComboBox<Categoria> comboCategorias;
    private TextField txtMonto;
    private TextField txtDescripcion;
    private TextField txtOrigen;
    private Label mensaje;
    private Label lblBalanceCuenta;
    private Button btnGuardar;

    public TransaccionView(Usuario usuario, boolean esIngreso) {
        super(usuario);
        this.esIngreso = esIngreso;
        this.color = esIngreso ? CyberTheme.VERDE : CyberTheme.ROJO;
    }

    @Override
    protected Scene construirEscena() {
        String titulo = esIngreso ? "CREDIT_INBOUND" : "DEBIT_OUTBOUND";
        String subtitulo = esIngreso
                ? "// REGISTRO DE INGRESO // SUMA AL BALANCE"
                : "// REGISTRO DE GASTO // RESTA DEL BALANCE";

        // Tarjeta central flotante sobre el fondo oscuro.
        VBox tarjeta = new VBox(16);
        tarjeta.setPadding(new Insets(28, 32, 28, 32));
        tarjeta.setMaxWidth(470);
        tarjeta.setStyle(CyberTheme.panel(color) + " -fx-effect: dropshadow(three-pass-box, rgba("
                + CyberTheme.rgb(color) + ", 0.18), 24, 0, 0, 0);");
        tarjeta.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = CyberTheme.titulo(titulo, color);
        lblTitulo.setFont(javafx.scene.text.Font.font("Courier New",
                javafx.scene.text.FontWeight.BOLD, 20));
        lblTitulo.setEffect(CyberTheme.halo(0.6));

        tarjeta.getChildren().addAll(lblTitulo, CyberTheme.caption(subtitulo, CyberTheme.GRIS_OSCURO));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(" + CyberTheme.rgb(color) + ", 0.25);");
        tarjeta.getChildren().add(sep);

        // --- Campos
        tarjeta.getChildren().add(etiquetaCampo("CUENTA_DESTINO", CyberTheme.GRIS_OSCURO));

        comboCuentas = new ComboBox<>();
        comboCuentas.setPromptText("SELECCIONA_CUENTA");
        comboCuentas.setMaxWidth(Double.MAX_VALUE);
        comboCuentas.setStyle(CyberTheme.campo(CyberTheme.CIAN));
        comboCuentas.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cuenta c) {
                return c == null ? "" : c.getNombre() + "  ($" + String.format("%,.2f", c.getBalanceActual()) + ")";
            }

            @Override
            public Cuenta fromString(String s) {
                return null; // Solo selección.
            }
        });
        tarjeta.getChildren().add(comboCuentas);

        // Muestra el saldo en vivo de la cuenta elegida.
        lblBalanceCuenta = CyberTheme.caption("// BALANCE_DISPONIBLE: ---", CyberTheme.CIAN);
        tarjeta.getChildren().add(lblBalanceCuenta);
        comboCuentas.valueProperty().addListener((obs, previo, actual) -> {
            if (actual == null) {
                lblBalanceCuenta.setText("// BALANCE_DISPONIBLE: ---");
                lblBalanceCuenta.setTextFill(javafx.scene.paint.Color.web(CyberTheme.CIAN));
            } else {
                double saldo = actual.getBalanceActual();
                lblBalanceCuenta.setText("// BALANCE_DISPONIBLE: $" + String.format("%,.2f", saldo));
                lblBalanceCuenta.setTextFill(javafx.scene.paint.Color.web(
                        saldo < 0 ? CyberTheme.ROJO : CyberTheme.VERDE));
            }
        });

        tarjeta.getChildren().add(etiquetaCampo("MONTO", CyberTheme.GRIS_OSCURO));
        txtMonto = new TextField();
        txtMonto.setPromptText("0.00");
        txtMonto.setMaxWidth(Double.MAX_VALUE);
        txtMonto.setStyle(CyberTheme.campo(color));
        tarjeta.getChildren().add(txtMonto);

        tarjeta.getChildren().add(etiquetaCampo("DESCRIPCION", CyberTheme.GRIS_OSCURO));
        txtDescripcion = new TextField();
        txtDescripcion.setPromptText("DETALLE_DEL_MOVIMIENTO");
        txtDescripcion.setMaxWidth(Double.MAX_VALUE);
        txtDescripcion.setStyle(CyberTheme.campo(CyberTheme.CIAN));
        tarjeta.getChildren().add(txtDescripcion);

        // --- Campo específico del tipo
        if (esIngreso) {
            tarjeta.getChildren().add(etiquetaCampo("ORIGEN", CyberTheme.GRIS_OSCURO));
            txtOrigen = new TextField();
            txtOrigen.setPromptText("ej: SALARIO / FREELANCE / VENTA");
            txtOrigen.setMaxWidth(Double.MAX_VALUE);
            txtOrigen.setStyle(CyberTheme.campo(color));
            tarjeta.getChildren().add(txtOrigen);
        } else {
            tarjeta.getChildren().add(etiquetaCampo("CATEGORIA", CyberTheme.GRIS_OSCURO));
            comboCategorias = new ComboBox<>();
            comboCategorias.setPromptText("SELECCIONA_CATEGORIA");
            comboCategorias.setMaxWidth(Double.MAX_VALUE);
            comboCategorias.setStyle(CyberTheme.campo(color));
            comboCategorias.setConverter(new StringConverter<>() {
                @Override
                public String toString(Categoria c) {
                    return c == null ? "" : c.getNombre();
                }

                @Override
                public Categoria fromString(String s) {
                    return null;
                }
            });
            tarjeta.getChildren().add(comboCategorias);
        }

        mensaje = new Label();
        mensaje.setWrapText(true);
        mensaje.setFont(javafx.scene.text.Font.font("Courier New", 11));
        mensaje.setMinHeight(18);
        tarjeta.getChildren().add(mensaje);

        // --- Acciones
        btnGuardar = CyberTheme.boton(esIngreso ? "CONFIRMAR_INGRESO" : "CONFIRMAR_GASTO",
                color, this::registrar, 175, 40);

        Button btnVolver = CyberTheme.boton("← VOLVER", CyberTheme.GRIS, this::volver, 130, 40);
        btnVolver.setId("btnVolver");

        HBox botones = new HBox(12, btnGuardar, btnVolver);
        botones.setAlignment(Pos.CENTER);
        tarjeta.getChildren().add(botones);

        StackPane root = new StackPane(tarjeta);
        root.setStyle("-fx-background-color: " + CyberTheme.FONDO + ";");
        return new Scene(root, 520, esIngreso ? 640 : 670);
    }

    private Label etiquetaCampo(String texto, String colorTexto) {
        return CyberTheme.etiqueta(texto, colorTexto, 9, javafx.scene.text.FontWeight.BOLD);
    }

    private void cargarDatosIniciales() {
        try {
            List<Cuenta> cuentas = cuentaDAO.obtenerCuentasPorUsuario(usuarioId());
            comboCuentas.setItems(FXCollections.observableArrayList(cuentas));

            if (cuentas.isEmpty()) {
                mostrarError("PRIMERO DEBES CREAR UNA CUENTA DESDE TACTICAL_ACCOUNTS.");
                btnGuardar.setDisable(true);
            } else {
                comboCuentas.getSelectionModel().selectFirst();
            }

            if (comboCategorias != null) {
                comboCategorias.setItems(FXCollections.observableArrayList(categoriaDAO.obtenerTodas()));
            }
        } catch (SQLException e) {
            mostrarError("NO SE PUDIERON CARGAR LOS DATOS.");
            btnGuardar.setDisable(true);
        }
    }

    private void registrar() {
        mensaje.setText("");
        try {
            Cuenta cuenta = comboCuentas.getValue();
            if (cuenta == null) {
                throw new ValidacionException("Selecciona una cuenta.");
            }

            double monto = Validador.parsearMonto(txtMonto.getText());
            Validador.validarMonto(monto);

            String descripcion = txtDescripcion.getText().trim();

            Transaccion t;
            if (esIngreso) {
                t = new Ingreso("0", monto, LocalDate.now(), descripcion, txtOrigen.getText());
            } else {
                if (comboCategorias.getValue() == null) {
                    throw new ValidacionException("Selecciona una categoría para el gasto.");
                }
                t = new Gasto("0", monto, LocalDate.now(), descripcion, comboCategorias.getValue());
            }

            if (transaccionDAO.registrarTransaccion(t, Integer.parseInt(cuenta.getId()), usuarioId())) {
                CyberAlert.mostrarInfo(esIngreso
                        ? "Ingreso registrado correctamente."
                        : "Gasto registrado correctamente.");
                volver();
            } else {
                mostrarError("No se pudo registrar la transacción.");
            }
        } catch (ValidacionException | SQLException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarError(String texto) {
        mensaje.setTextFill(javafx.scene.paint.Color.web(CyberTheme.ROJO));
        mensaje.setText("// " + (texto == null || texto.isBlank() ? "ERROR DESCONOCIDO" : texto.toUpperCase()));
        FadeTransition ft = new FadeTransition(Duration.millis(280), mensaje);
        ft.setFromValue(0.25);
        ft.setToValue(1.0);
        ft.play();
    }
}