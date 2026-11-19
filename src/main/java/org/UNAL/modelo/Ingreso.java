package org.UNAL.modelo;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Subclase de {@link Transaccion} para los ingresos.
 * Atributo propio: {@code origen}.
 */
public class Ingreso extends Transaccion {

    private final StringProperty origen;

    public Ingreso(String id, double monto, LocalDate fecha, String descripcion, String origen) {
        super(id, monto, fecha, descripcion);
        this.origen = new SimpleStringProperty(origen);
    }

    @Override
    public void ejecutar() { /* El ajuste de saldo lo realiza TransaccionDAO. */ }

    @Override
    public void deshacer() { /* El ajuste de saldo lo realiza TransaccionDAO. */ }

    public StringProperty origenProperty() { return origen; }
    public String getOrigen() { return origen.get(); }
    public void setOrigen(String value) { origen.set(value); }
}