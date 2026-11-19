package org.UNAL.modelo;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;

/**
 * Subclase de {@link Transaccion} para los gastos.
 * Atributo propio: la categoría a la que se asigna.
 */
public class Gasto extends Transaccion {

    private final ObjectProperty<Categoria> categoria;

    public Gasto(String id, double monto, LocalDate fecha, String descripcion, Categoria categoria) {
        super(id, monto, fecha, descripcion);
        this.categoria = new SimpleObjectProperty<>(categoria);
    }

    @Override
    public void ejecutar() { /* El ajuste de saldo lo realiza TransaccionDAO. */ }

    @Override
    public void deshacer() { /* El ajuste de saldo lo realiza TransaccionDAO. */ }

    public ObjectProperty<Categoria> categoriaProperty() { return categoria; }
    public Categoria getCategoria() { return categoria.get(); }
    public void setCategoria(Categoria value) { categoria.set(value); }
}