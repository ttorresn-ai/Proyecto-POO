package org.UNAL.modelo;

import javafx.beans.property.*;

public class Usuario {
    private final StringProperty id;
    private final StringProperty nombre;
    private final StringProperty email;
    private final StringProperty password;

    public Usuario(String id, String nombre, String email, String password) {
        this.id = new SimpleStringProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
        this.email = new SimpleStringProperty(email);
        this.password = new SimpleStringProperty(password);
    }


    public StringProperty idProperty() { return id; }
    public StringProperty nombreProperty() { return nombre; }
    public StringProperty emailProperty() { return email; }
    public StringProperty passwordProperty() { return password; }


    public String getId() { return id.get(); }
    public void setId(String value) { id.set(value); }
    public String getNombre() { return nombre.get(); }
    public void setNombre(String value) { nombre.set(value); }
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public String getPassword() { return password.get(); }
    public void setPassword(String value) { password.set(value); }
}