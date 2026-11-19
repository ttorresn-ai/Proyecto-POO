package org.UNAL.config;

/**
 * Excepción de dominio para reglas de negocio incumplidas.
 * Reemplaza el uso de {@code throws Exception} genérico.
 */
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}