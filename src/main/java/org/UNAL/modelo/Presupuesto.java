package org.UNAL.modelo;

public class Presupuesto {
    private String id;
    private double montoLimite;
    private int mes;
    private int anio;

    private int categoriaId;
    public Presupuesto(String id, double montoLimite, int mes, int anio) {
        this.id = id;
        this.montoLimite = montoLimite;
        this.mes = mes;
        this.anio = anio;
    }


    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    // --- EL GETTER QUE USA LA TABLA ---
    public int getCategoriaId() {
        return categoriaId;
    }

    // Resto de getters y setters (id, montoLimite, etc.)
    public String getId() { return id; }
    public double getMontoLimite() { return montoLimite; }
    public int getMes() { return mes; }
    public int getAnio() { return anio; }
}