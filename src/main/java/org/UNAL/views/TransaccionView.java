package org.UNAL.views;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import org.UNAL.DAO.*;
import org.UNAL.modelo.*;
import java.util.Date;

public class TransaccionView {
    private final Usuario usuario;
    private final boolean esIngreso;
    private final CuentaDAO cuentaDAO = new CuentaDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final TransaccionDAO transaccionDAO = new TransaccionDAO();

    public TransaccionView(Usuario usuario, boolean esIngreso) {
        this.usuario = usuario;
        this.esIngreso = esIngreso;
    }

    public Scene getScene(Runnable onFinalizar) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #232526;");

        Label titulo = new Label(esIngreso ? "Registrar Ingreso" : "Registrar Gasto");
        titulo.setFont(Font.font("Arial", 24));
        titulo.setTextFill(esIngreso ? Color.LIGHTGREEN : Color.web("#ff6666"));

        ComboBox<Cuenta> comboCuentas = new ComboBox<>();
        comboCuentas.setPromptText("Seleccione Cuenta");
        comboCuentas.setItems(FXCollections.observableArrayList(cuentaDAO.obtenerCuentasPorUsuario(Integer.parseInt(usuario.getId()))));
        comboCuentas.setPrefWidth(300);

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Monto $");
        txtMonto.setMaxWidth(300);

        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Descripción");
        txtDesc.setMaxWidth(300);

        // Campos específicos
        TextField txtExtra = new TextField();
        ComboBox<Categoria> comboCat = new ComboBox<>();

        if (esIngreso) {
            txtExtra.setPromptText("Origen (ej: Salario)");
            txtExtra.setMaxWidth(300);
            root.getChildren().add(txtExtra);
        } else {
            comboCat.setPromptText("Categoría");
            comboCat.setItems(FXCollections.observableArrayList(categoriaDAO.obtenerTodas()));
            comboCat.setPrefWidth(300);
            root.getChildren().add(comboCat);
        }

        Button btnGuardar = new Button("Confirmar");
        btnGuardar.setStyle("-fx-background-color: #2d8cf0; -fx-text-fill: white; -fx-background-radius: 20;");
        btnGuardar.setPrefWidth(150);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> onFinalizar.run());

        btnGuardar.setOnAction(e -> {
            try {
                Cuenta c = comboCuentas.getValue();
                double monto = Double.parseDouble(txtMonto.getText());
                String desc = txtDesc.getText();

                Transaccion t;
                if (esIngreso) {
                    t = new Ingreso("0", monto, new Date(), desc, txtExtra.getText());
                } else {
                    t = new Gasto("0", monto, new Date(), desc, comboCat.getValue());
                }

                if (transaccionDAO.registrarTransaccion(t, Integer.parseInt(c.getId()))) {
                    onFinalizar.run();
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });

        HBox btns = new HBox(15, btnGuardar, btnCancelar);
        btns.setAlignment(Pos.CENTER);

        root.getChildren().add(0, titulo);
        root.getChildren().addAll(comboCuentas, txtMonto, txtDesc, btns);

        return new Scene(root, 450, 500);
    }
}