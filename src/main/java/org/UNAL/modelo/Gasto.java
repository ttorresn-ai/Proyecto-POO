package org.UNAL.modelo;

import javafx.beans.property.*;
import java.util.Date;

public class Gasto extends Transaccion {
    private final ObjectProperty<Categoria> categoria;

    public Gasto(String id, double monto, Date fecha, String descripcion, Categoria categoria) {
        super(id, monto, fecha, descripcion);
        this.categoria = new SimpleObjectProperty<>(categoria);
    }

    @Override
    public void ejecutar() { /* Lógica de resta */ }

    @Override
    public void deshacer() { /* Lógica de suma */ }

    public ObjectProperty<Categoria> categoriaProperty() { return categoria; }
    public Categoria getCategoria() { return categoria.get(); }
}
