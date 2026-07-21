package org.UNAL.modelo;

import javafx.beans.property.*;
import java.util.Date;

public class Ingreso extends Transaccion {
    private final StringProperty origen;

    public Ingreso(String id, double monto, Date fecha, String descripcion, String origen) {
        super(id, monto, fecha, descripcion);
        this.origen = new SimpleStringProperty(origen);
    }

    @Override
    public void ejecutar() { /* Lógica de suma */ }

    @Override
    public void deshacer() { /* Lógica de resta */ }

    public StringProperty origenProperty() { return origen; }
    public String getOrigen() { return origen.get(); }
}