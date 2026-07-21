package org.UNAL.modelo;

public class Cuenta {

    private String id;
    private String nombre;
    private String tipoCuenta;
    private double balanceActual;


    public Cuenta(String id, String nombre, String tipoCuenta, double balanceActual) {
        this.id = id;
        this.nombre = nombre;
        this.tipoCuenta = tipoCuenta;
        this.balanceActual = balanceActual;
    }


    public void depositar(double monto) {
        if (monto > 0) {
            this.balanceActual += monto;
            System.out.println("Depósito exitoso. Nuevo balance: " + this.balanceActual);
        } else {
            System.out.println("El monto a depositar debe ser mayor a 0.");
        }
    }

    public boolean retirar(double monto) {
        if (monto > 0 && this.balanceActual >= monto) {
            this.balanceActual -= monto;
            System.out.println("Retiro exitoso. Nuevo balance: " + this.balanceActual);
            return true;
        } else {
            System.out.println("Fondos insuficientes o monto inválido.");
            return false;
        }
    }

    public boolean transferir(Cuenta destino, double monto) {
        if (this.retirar(monto)) {
            destino.depositar(monto);
            System.out.println("Transferencia de " + monto + " a la cuenta " + destino.getNombre() + " exitosa.");
            return true;
        } else {
            System.out.println("Error en la transferencia.");
            return false;
        }
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    public double getBalanceActual() { return balanceActual; }
    public void setBalanceActual(double balanceActual) { this.balanceActual = balanceActual; }
    @Override
    public String toString() {
        return this.nombre; // O this.getNombre() si la variable es privada
    }
}