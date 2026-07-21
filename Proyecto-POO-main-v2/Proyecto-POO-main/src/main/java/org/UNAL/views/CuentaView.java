package org.UNAL.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import org.UNAL.DAO.CuentaDAO;
import org.UNAL.modelo.Cuenta;
import org.UNAL.modelo.Usuario;
import java.util.List;

public class CuentaView {

    private final Usuario usuario;
    private final CuentaDAO cuentaDAO = new CuentaDAO();

    private static final String VERDE = "#13C784";
    private static final String CIAN = "#00f2fe";
    private static final String ROJO = "#ff3366";
    private static final String FONDO = "#020208";

    public CuentaView(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene getScene(Runnable onVolver) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + FONDO + ";");
        root.setPadding(new Insets(30));

        // --- ENCABEZADO ---
        Label titulo = new Label("TACTICAL_ACCOUNTS // MODULE");
        titulo.setFont(Font.font("Courier New", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web(VERDE));
        titulo.setEffect(new Glow(0.6));

        Button btnVolver = new Button("< BACK_TO_HUD");
        btnVolver.setStyle(estiloBtn(ROJO));
        btnVolver.setOnMouseEntered(e -> btnVolver.setStyle(estiloBtnHover(ROJO)));
        btnVolver.setOnMouseExited(e -> btnVolver.setStyle(estiloBtn(ROJO)));
        btnVolver.setOnAction(e -> onVolver.run());

        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(titulo);
        headerPane.setRight(btnVolver);
        BorderPane.setAlignment(btnVolver, Pos.CENTER_RIGHT);

        root.setTop(headerPane);
        BorderPane.setMargin(headerPane, new Insets(0, 0, 25, 0));

        // --- LADO IZQUIERDO: TABLA DE CUENTAS ---
        VBox leftBox = new VBox(15);

        TableView<Cuenta> tabla = new TableView<>();
        tabla.setStyle("-fx-background-color: rgba(0,242,254,0.02); -fx-control-inner-background: " + FONDO + "; " +
                "-fx-border-color: rgba(0,242,254,0.25); -fx-border-width: 1;");

        TableColumn<Cuenta, String> colNombre = new TableColumn<>("NOMBRE");
        colNombre.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Cuenta, String> colTipo = new TableColumn<>("TIPO");
        colTipo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTipoCuenta()));

        TableColumn<Cuenta, Number> colSaldo = new TableColumn<>("SALDO");
        colSaldo.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getBalanceActual()));

        tabla.getColumns().addAll(colNombre, colTipo, colSaldo);
        estilizarColumnas(colNombre, colTipo, colSaldo);
        actualizarTabla(tabla);

        leftBox.getChildren().addAll(tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // --- LADO DERECHO: FORMULARIO ---
        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(25));
        formBox.setPrefWidth(260);
        formBox.setStyle("-fx-background-color: rgba(19,199,132,0.03); -fx-border-color: rgba(19,199,132,0.3); " +
                "-fx-border-width: 1.5; -fx-border-radius: 4; -fx-background-radius: 4;");
        formBox.setAlignment(Pos.TOP_CENTER);

        Label formTitle = new Label("NEW_ACCOUNT_ENTRY");
        formTitle.setFont(Font.font("Courier New", FontWeight.BOLD, 14));
        formTitle.setTextFill(Color.web(CIAN));

        String estiloCampos = "-fx-background-color: rgba(255,255,255,0.03); -fx-text-fill: " + VERDE + "; " +
                "-fx-font-family: 'Courier New'; -fx-font-size: 13px; -fx-prompt-text-fill: rgba(19,199,132,0.4); " +
                "-fx-border-color: rgba(19,199,132,0.25); -fx-border-radius: 4; -fx-padding: 10;";

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("ACCOUNT_NAME");
        txtNombre.setStyle(estiloCampos);

        ComboBox<String> comboTipo = new ComboBox<>(FXCollections.observableArrayList("Ahorros", "Efectivo", "Tarjeta de Crédito"));
        comboTipo.setPromptText("ACCOUNT_TYPE");
        comboTipo.setMaxWidth(Double.MAX_VALUE);
        comboTipo.setStyle(estiloCampos);

        TextField txtSaldo = new TextField();
        txtSaldo.setPromptText("INITIAL_BALANCE");
        txtSaldo.setStyle(estiloCampos);

        Button btnGuardar = new Button("EXECUTE_CREATE");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        btnGuardar.setStyle(estiloBtn(VERDE));
        btnGuardar.setOnMouseEntered(e -> btnGuardar.setStyle(estiloBtnHover(VERDE)));
        btnGuardar.setOnMouseExited(e -> btnGuardar.setStyle(estiloBtn(VERDE)));

        Label msg = new Label();
        msg.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        msg.setWrapText(true);

        btnGuardar.setOnAction(e -> {
            try {
                String nom = txtNombre.getText();
                String tipo = comboTipo.getValue();
                if (nom == null || nom.isBlank()) throw new Exception("EMPTY_NAME");
                if (tipo == null) throw new Exception("SELECT_TYPE");
                double saldo = Double.parseDouble(txtSaldo.getText());

                Cuenta nueva = new Cuenta("0", nom, tipo, saldo);
                if (cuentaDAO.crearCuenta(nueva, Integer.parseInt(usuario.getId()))) {
                    msg.setTextFill(Color.web(VERDE));
                    msg.setText("// ACCOUNT_CREATED_OK");
                    actualizarTabla(tabla);
                    txtNombre.clear();
                    txtSaldo.clear();
                    comboTipo.setValue(null);
                } else {
                    msg.setTextFill(Color.web(ROJO));
                    msg.setText("// DB_WRITE_FAILED");
                }
            } catch (NumberFormatException nfe) {
                msg.setTextFill(Color.web(ROJO));
                msg.setText("// INVALID_BALANCE_FORMAT");
            } catch (Exception ex) {
                msg.setTextFill(Color.web(ROJO));
                msg.setText("// " + ex.getMessage());
            }
        });

        formBox.getChildren().addAll(formTitle, txtNombre, comboTipo, txtSaldo, btnGuardar, msg);

        HBox contenido = new HBox(25, leftBox, formBox);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        root.setCenter(contenido);

        return new Scene(root, 1024, 640);
    }

    private void estilizarColumnas(TableColumn<?, ?>... columnas) {
        for (TableColumn<?, ?> col : columnas) {
            col.setStyle("-fx-text-fill: " + CIAN + "; -fx-font-family: 'Courier New';");
        }
    }

    private String estiloBtn(String color) {
        return "-fx-background-color: rgba(0,0,0,0.3); -fx-text-fill: " + color + "; " +
                "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-border-color: " + color + "; -fx-border-width: 1; -fx-border-radius: 4; " +
                "-fx-padding: 8 16; -fx-cursor: hand;";
    }

    private String estiloBtnHover(String color) {
        return estiloBtn(color) + " -fx-background-color: derive(" + color + ", -80%); " +
                "-fx-effect: dropshadow(three-pass-box, " + color + ", 12, 0, 0, 0);";
    }

    private void actualizarTabla(TableView<Cuenta> tabla) {
        List<Cuenta> lista = cuentaDAO.obtenerCuentasPorUsuario(Integer.parseInt(usuario.getId()));
        tabla.setItems(FXCollections.observableArrayList(lista));
    }
}
