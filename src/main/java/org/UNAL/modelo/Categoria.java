package org.UNAL.modelo;

import javafx.beans.property.*;

public class Categoria {
    private final StringProperty id;
    private final StringProperty nombre;


    public Categoria(String id, String nombre) {
        this.id = new SimpleStringProperty(id);
        this.nombre = new SimpleStringProperty(nombre);

    }

    public StringProperty nombreProperty() { return nombre; }
    public String getNombre() { return nombre.get(); }
    public StringProperty idProperty(){return id;}
    public String getId(){return id.get();}
    // (Añadir otros getters/setters/properties según necesites)
    @Override
    public String toString() {
        return this.getNombre(); // O this.getNombre() si la variable es privada
    }
}