package org.UNAL.config;

import java.util.regex.Pattern;
import java.util.List;

public class Validador {

    // Regex para email estándar
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

    public static void validarEmail(String email) throws Exception {
        if (!Pattern.matches(EMAIL_REGEX, email)) {
            throw new Exception("El formato del email no es válido (ejemplo@correo.com).");
        }
    }

    public static void validarNombre(String nombre) throws Exception {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new Exception("El nombre debe tener al menos 3 caracteres.");
        }
    }

    public static void validarMonto(double monto) throws Exception {
        if (monto <= 0) {
            throw new Exception("El monto debe ser un número positivo.");
        }
    }

    // Para evitar el error IndexOutOfBoundsException que tuviste
    public static int validarSeleccionMenu(int seleccion, int tamañoLista) throws Exception {
        if (seleccion < 1 || seleccion > tamañoLista) {
            throw new Exception("Selección fuera de rango. Elija una opción entre 1 y " + tamañoLista + ".");
        }
        return seleccion - 1; // Retorna el índice real para la lista (0-based)
    }
}