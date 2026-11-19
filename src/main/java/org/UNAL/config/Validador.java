package org.UNAL.config;

import java.util.regex.Pattern;

/**
 * Validaciones de entrada compartidas por todas las vistas.
 * Lanza {@link ValidacionException} para que la UI pueda mostrar el mensaje
 * directamente sin capturar {@code Exception} genérica.
 */
public final class Validador {

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validador() {
        // Clase utilitaria: no instanciable.
    }

    public static void validarEmail(String email) throws ValidacionException {
        if (email == null || email.isBlank()) {
            throw new ValidacionException("El correo electrónico es obligatorio.");
        }
        if (!EMAIL_REGEX.matcher(email.trim()).matches()) {
            throw new ValidacionException("El formato del email no es válido (ejemplo@correo.com).");
        }
    }

    public static void validarNombre(String nombre) throws ValidacionException {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new ValidacionException("El nombre debe tener al menos 3 caracteres.");
        }
    }

    public static void validarPassword(String password) throws ValidacionException {
        if (password == null || password.length() < 6) {
            throw new ValidacionException("La clave debe tener al menos 6 caracteres.");
        }
    }

    public static void validarMonto(double monto) throws ValidacionException {
        if (monto <= 0) {
            throw new ValidacionException("El monto debe ser un número positivo.");
        }
    }

    /**
     * Convierte texto a número aceptando tanto "1500.50" como "1500,50",
     * independientemente del locale del sistema.
     *
     * @throws ValidacionException si el texto no representa un número.
     */
    public static double parsearMonto(String texto) throws ValidacionException {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("Ingrese un monto.");
        }

        // Quitamos espacios y el símbolo de moneda.
        String s = texto.trim()
                .replaceAll("[\\s\\u00A0]", "")
                .replace("$", "")
                .replace("%", "");

        if (s.isEmpty()) {
            throw new ValidacionException("Ingrese un monto.");
        }

        int coma = s.lastIndexOf(',');
        int punto = s.lastIndexOf('.');

        // El separador decimal es el que aparece más a la derecha, siempre que
        // lo que le sigue tenga 1 o 2 dígitos (o sea, decimales y no miles).
        int posDecimal = -1;
        if (coma >= 0 && coma > punto) {
            if (s.length() - coma - 1 <= 2) {
                posDecimal = coma;
            }
        } else if (punto >= 0) {
            if (s.length() - punto - 1 <= 2) {
                posDecimal = punto;
            }
        }

        String entero;
        String decimal = "";
        if (posDecimal >= 0) {
            entero = s.substring(0, posDecimal);
            decimal = s.substring(posDecimal + 1);
            // Solo se admiten dígitos alrededor del separador.
            entero = entero.replaceAll("[.,]", "");
            decimal = decimal.replaceAll("[.,]", "");
        } else {
            entero = s.replaceAll("[.,]", "");
        }

        if (entero.isEmpty() || !entero.matches("\\d+")
                || (!decimal.isEmpty() && !decimal.matches("\\d{1,2}"))) {
            throw new ValidacionException("El monto debe ser un número válido.");
        }

        String normalizado = entero + (decimal.isEmpty() ? "" : "." + decimal);
        try {
            return Double.parseDouble(normalizado);
        } catch (NumberFormatException e) {
            throw new ValidacionException("El monto debe ser un número válido.");
        }
    }

    /**
     * Valida una selección de menú y devuelve el índice 0-based de la lista.
     *
     * @throws ValidacionException si la opción está fuera de rango.
     */
    public static int validarSeleccionMenu(int seleccion, int tamanoLista) throws ValidacionException {
        if (seleccion < 1 || seleccion > tamanoLista) {
            throw new ValidacionException("Selección fuera de rango. Elija una opción entre 1 y " + tamanoLista + ".");
        }
        return seleccion - 1;
    }
}