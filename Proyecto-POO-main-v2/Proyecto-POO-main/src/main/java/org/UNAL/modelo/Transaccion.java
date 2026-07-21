package org.UNAL.modelo;

import javafx.beans.property.*;
import java.util.Date;

public abstract class Transaccion {
    protected final StringProperty id;
    protected final DoubleProperty monto;
    protected final ObjectProperty<Date> fecha;
    protected final StringProperty descripcion;

    public Transaccion(String id, double monto, Date fecha, String descripcion) {
        this.id = new SimpleStringProperty(id);
        this.monto = new SimpleDoubleProperty(monto);
        this.fecha = new SimpleObjectProperty<>(fecha);
        this.descripcion = new SimpleStringProperty(descripcion);
    }

    public abstract void ejecutar();
    public abstract void deshacer();


    public StringProperty idProperty() { return id; }
    public DoubleProperty montoProperty() { return monto; }
    public ObjectProperty<Date> fechaProperty() { return fecha; }
    public StringProperty descripcionProperty() { return descripcion; }


    public String getId() { return id.get(); }
    public double getMonto() { return monto.get(); }
    public Date getFecha() { return fecha.get(); }
    public String getDescripcion() { return descripcion.get(); }
}