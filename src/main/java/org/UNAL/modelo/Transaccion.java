package org.UNAL.modelo;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Superclase de la jerarquía de movimientos financieros.
 *
 * <p>Almacena únicamente los atributos comunes a {@link Ingreso} y
 * {@link Gasto}. Cada subclase aporta sus atributos propios, que en el modelo
 * relacional viven en tablas separadas ({@code ingresos} y {@code gastos}).
 */
public abstract class Transaccion {

    protected final StringProperty id;
    protected final DoubleProperty monto;
    protected final ObjectProperty<LocalDate> fecha;
    protected final StringProperty descripcion;

    public Transaccion(String id, double monto, LocalDate fecha, String descripcion) {
        this.id = new SimpleStringProperty(id);
        this.monto = new SimpleDoubleProperty(monto);
        this.fecha = new SimpleObjectProperty<>(fecha == null ? LocalDate.now() : fecha);
        this.descripcion = new SimpleStringProperty(descripcion);
    }

    /**
     * Aplica el movimiento sobre una cuenta.
     * Los datos ya están en memoria; la persistencia la resuelve el DAO.
     */
    public abstract void ejecutar();

    /** Revierte el efecto del movimiento. */
    public abstract void deshacer();

    public StringProperty idProperty() { return id; }
    public DoubleProperty montoProperty() { return monto; }
    public ObjectProperty<LocalDate> fechaProperty() { return fecha; }
    public StringProperty descripcionProperty() { return descripcion; }

    public String getId() { return id.get(); }
    public double getMonto() { return monto.get(); }
    public LocalDate getFecha() { return fecha.get(); }
    public String getDescripcion() { return descripcion.get(); }
}