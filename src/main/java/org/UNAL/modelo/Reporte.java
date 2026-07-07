package org.UNAL.modelo;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Reporte {

    private final ObjectProperty<LocalDate> fechaInicio;
    private final ObjectProperty<LocalDate> fechaFin;
    private final StringProperty resumen;

    public Reporte(LocalDate fechaInicio, LocalDate fechaFin) {
        this.fechaInicio = new SimpleObjectProperty<>(fechaInicio);
        this.fechaFin = new SimpleObjectProperty<>(fechaFin);
        this.resumen = new SimpleStringProperty("");
    }


    public ObjectProperty<LocalDate> fechaInicioProperty() { return fechaInicio; }
    public ObjectProperty<LocalDate> fechaFinProperty() { return fechaFin; }
    public StringProperty resumenProperty() { return resumen; }


    public LocalDate getFechaInicio() { return fechaInicio.get(); }
    public void setFechaInicio(LocalDate fecha) { this.fechaInicio.set(fecha); }

    public LocalDate getFechaFin() { return fechaFin.get(); }
    public void setFechaFin(LocalDate fecha) { this.fechaFin.set(fecha); }

    public String getResumen() { return resumen.get(); }
    public void setResumen(String texto) { this.resumen.set(texto); }
}