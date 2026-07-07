package org.UNAL.views;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.UNAL.DAO.PresupuestoDAO;
import org.UNAL.DAO.CategoriaDAO;
import org.UNAL.modelo.Presupuesto;
import org.UNAL.modelo.Categoria;
import org.UNAL.modelo.Usuario;
import org.UNAL.config.Validador;

import java.time.YearMonth;
import java.util.List;

public class PresupuestoView {
    private final Usuario usuario;
    private final PresupuestoDAO presupuestoDAO = new PresupuestoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    public PresupuestoView(Usuario usuario) {
        this.usuario = usuario;
    }

    public Scene getScene(Runnable onAtras) {
        // Layout principal
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:linear-gradient(to top,#f3f6fa 95%,#e1e8ed 120%);");

        // --- Título
        Label titulo = new Label("Gestión de Presupuestos");
        titulo.setFont(Font.font("Arial", 22));
        titulo.setTextFill(Color.web("#2d8cf0"));

        // --- Tabla de Presupuestos
        TableView<Presupuesto> tabla = new TableView<>();

        TableColumn<Presupuesto, String> catCol = new TableColumn<>("Categoría");
        catCol.setCellValueFactory(data -> {
            int catId = data.getValue().getCategoriaId();
            // Usamos tu método obtenerPorId
            Categoria cat = categoriaDAO.obtenerPorId(catId);

            return new javafx.beans.property.SimpleStringProperty(
                    cat != null ? cat.getNombre() : "Sin nombre (ID: " + catId + ")"
            );
        });

        TableColumn<Presupuesto, Number> montoCol = new TableColumn<>("Monto límite");
        montoCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getMontoLimite()));

        TableColumn<Presupuesto, String> periCol = new TableColumn<>("Período");
        periCol.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%02d/%d", data.getValue().getMes(), data.getValue().getAnio())
        ));

        tabla.getColumns().addAll(catCol, montoCol, periCol);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        cargarPresupuestos(tabla);

        // --- Formulario de alta
        ComboBox<Categoria> categoriaCombo = new ComboBox<>();
        cargarCategorias(categoriaCombo);
        categoriaCombo.setPromptText("Seleccione categoría");
        categoriaCombo.setMaxWidth(Double.MAX_VALUE);

        TextField montoField = new TextField();
        montoField.setPromptText("Monto límite (ej: 50000)");

        Button guardarBtn = new Button("Crear Presupuesto");
        guardarBtn.setMaxWidth(Double.MAX_VALUE);
        guardarBtn.setStyle("-fx-background-color:#1e90ff;-fx-text-fill:white;-fx-background-radius:15;-fx-font-weight:bold;");

        Label lblMsg = new Label();
        lblMsg.setWrapText(true);

        Button atrasBtn = new Button("← Volver al Menú");
        atrasBtn.setMaxWidth(Double.MAX_VALUE);
        atrasBtn.setStyle("-fx-background-color:#bbb;-fx-background-radius:15;");
        atrasBtn.setOnAction(e -> { if (onAtras != null) onAtras.run(); });

        // Lógica de Guardado
        guardarBtn.setOnAction(e -> {
            lblMsg.setText("");
            try {
                Categoria catSeleccionada = categoriaCombo.getValue();
                String montoStr = montoField.getText().trim();

                if (catSeleccionada == null) throw new Exception("Debe seleccionar una categoría.");
                if (montoStr.isEmpty()) throw new Exception("Ingrese un monto.");

                double monto = Double.parseDouble(montoStr);
                Validador.validarMonto(monto);

                YearMonth hoy = YearMonth.now();
                // Creamos el objeto (Sin setCategoriaNombre porque no existe en el modelo)
                Presupuesto p = new Presupuesto("0", monto, hoy.getMonthValue(), hoy.getYear());

                int uId = Integer.parseInt(usuario.getId());
                int cId = Integer.parseInt(catSeleccionada.getId());

                if (presupuestoDAO.guardarPresupuesto(p, uId, cId)) {
                    lblMsg.setTextFill(Color.web("#23b685"));
                    lblMsg.setText("¡Presupuesto creado con éxito!");
                    montoField.clear();
                    categoriaCombo.setValue(null);
                    cargarPresupuestos(tabla);
                } else {
                    throw new Exception("No se pudo guardar en la base de datos.");
                }
            } catch (NumberFormatException ex) {
                lblMsg.setTextFill(Color.web("#cb4242"));
                lblMsg.setText("El monto debe ser un número válido.");
            } catch (Exception ex) {
                lblMsg.setTextFill(Color.web("#cb4242"));
                lblMsg.setText(ex.getMessage());
            }
        });

        // --- Layouts
        VBox formBox = new VBox(15, new Label("Nuevo Presupuesto"), categoriaCombo, montoField, guardarBtn, lblMsg, new Separator(), atrasBtn);
        formBox.setPadding(new Insets(20));
        formBox.setPrefWidth(220);
        formBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-width: 0 0 0 1;");

        VBox contentBox = new VBox(16, titulo, tabla);
        contentBox.setPadding(new Insets(20));
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        root.setCenter(contentBox);
        root.setRight(formBox);

        return new Scene(root, 800, 500);
    }

    private void cargarCategorias(ComboBox<Categoria> combo) {
        List<Categoria> lista = categoriaDAO.obtenerTodas();
        combo.setItems(FXCollections.observableArrayList(lista));

        // ESTO ES LO QUE HACE QUE SE VEA EL NOMBRE
        combo.setConverter(new javafx.util.StringConverter<Categoria>() {
            @Override
            public String toString(Categoria cat) {
                return (cat == null) ? "" : cat.getNombre();
            }

            @Override
            public Categoria fromString(String string) {
                return null; // No es necesario para un ComboBox de selección
            }
        });
    }

    private void cargarPresupuestos(TableView<Presupuesto> tabla) {
        // Obtenemos los presupuestos directamente del DAO
        List<Presupuesto> lista = presupuestoDAO.obtenerPresupuestosPorUsuario(Integer.parseInt(usuario.getId()));
        tabla.setItems(FXCollections.observableArrayList(lista));
        tabla.refresh(); // Forzamos el refresco para que la columna de categoría busque los nombres
    }
}