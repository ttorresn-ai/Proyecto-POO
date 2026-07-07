package org.UNAL.views;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import org.UNAL.DAO.CuentaDAO;
import org.UNAL.modelo.Cuenta;
import org.UNAL.modelo.Usuario;
import java.util.List;

public class CuentaView {
    private final Usuario usuario;
    private final CuentaDAO cuentaDAO = new CuentaDAO();

    public CuentaView(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene getScene(Runnable onVolver) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1e3c72, #2a5298);");

        // --- LADO IZQUIERDO: TABLA DE CUENTAS ---
        VBox leftBox = new VBox(15);
        leftBox.setPadding(new Insets(25));

        Label titulo = new Label("Mis Cuentas");
        titulo.setFont(Font.font("Arial", 22));
        titulo.setTextFill(Color.WHITE);

        TableView<Cuenta> tabla = new TableView<>();
        TableColumn<Cuenta, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<Cuenta, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTipoCuenta()));

        TableColumn<Cuenta, Number> colSaldo = new TableColumn<>("Saldo");
        colSaldo.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(d.getValue().getBalanceActual()));

        tabla.getColumns().addAll(colNombre, colTipo, colSaldo);
        actualizarTabla(tabla);

        leftBox.getChildren().addAll(titulo, tabla);

        // --- LADO DERECHO: FORMULARIO ---
        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(25));
        formBox.setPrefWidth(250);
        formBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10;");
        formBox.setAlignment(Pos.TOP_CENTER);

        Label formTitle = new Label("Nueva Cuenta");
        formTitle.setTextFill(Color.LIGHTBLUE);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: Ahorros Bancolombia");

        ComboBox<String> comboTipo = new ComboBox<>(FXCollections.observableArrayList("Ahorros", "Efectivo", "Tarjeta de Crédito"));
        comboTipo.setPromptText("Tipo de cuenta");
        comboTipo.setMaxWidth(Double.MAX_VALUE);

        TextField txtSaldo = new TextField();
        txtSaldo.setPromptText("Saldo inicial");

        Button btnGuardar = new Button("Crear Cuenta");
        btnGuardar.setStyle("-fx-background-color: #13C784; -fx-text-fill: white; -fx-background-radius: 15;");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);

        Button btnVolver = new Button("Volver");
        btnVolver.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 15;");
        btnVolver.setMaxWidth(Double.MAX_VALUE);
        btnVolver.setOnAction(e -> onVolver.run());

        Label msg = new Label();

        btnGuardar.setOnAction(e -> {
            try {
                String nom = txtNombre.getText();
                String tipo = comboTipo.getValue();
                double saldo = Double.parseDouble(txtSaldo.getText());

                Cuenta nueva = new Cuenta("0", nom, tipo, saldo);
                if (cuentaDAO.crearCuenta(nueva, Integer.parseInt(usuario.getId()))) {
                    msg.setTextFill(Color.LIGHTGREEN);
                    msg.setText("¡Éxito!");
                    actualizarTabla(tabla);
                    txtNombre.clear(); txtSaldo.clear();
                }
            } catch (Exception ex) {
                msg.setTextFill(Color.ORANGE);
                msg.setText("Datos inválidos");
            }
        });

        formBox.getChildren().addAll(formTitle, txtNombre, comboTipo, txtSaldo, btnGuardar, btnVolver, msg);

        root.setCenter(leftBox);
        root.setRight(formBox);

        return new Scene(root, 750, 450);
    }

    private void actualizarTabla(TableView<Cuenta> tabla) {
        List<Cuenta> lista = cuentaDAO.obtenerCuentasPorUsuario(Integer.parseInt(usuario.getId()));
        tabla.setItems(FXCollections.observableArrayList(lista));
    }
}