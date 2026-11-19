package org.UNAL.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Pruebas de las validaciones de entrada, que no requieren base de datos.
 */
class ValidadorTest {

    @Test
    @DisplayName("Acepta emails con formato válido")
    void aceptaEmailsValidos() {
        assertDoesNotThrow(() -> Validador.validarEmail("usuario@correo.com"));
        assertDoesNotThrow(() -> Validador.validarEmail("nombre.apellido+etiqueta@sub.dominio.co"));
    }

    @Test
    @DisplayName("Rechaza emails mal formados")
    void rechazaEmailsInvalidos() {
        assertThrows(ValidacionException.class, () -> Validador.validarEmail("sinarroba.com"));
        assertThrows(ValidacionException.class, () -> Validador.validarEmail("@correo.com"));
        assertThrows(ValidacionException.class, () -> Validador.validarEmail(""));
        assertThrows(ValidacionException.class, () -> Validador.validarEmail(null));
    }

    @Test
    @DisplayName("Exige nombres de al menos 3 caracteres")
    void validaNombre() {
        assertDoesNotThrow(() -> Validador.validarNombre("Ana"));
        assertThrows(ValidacionException.class, () -> Validador.validarNombre("Ab"));
        assertThrows(ValidacionException.class, () -> Validador.validarNombre("   "));
    }

    @Test
    @DisplayName("Exige contraseñas de al menos 6 caracteres")
    void validaPassword() {
        assertDoesNotThrow(() -> Validador.validarPassword("secreto1"));
        assertThrows(ValidacionException.class, () -> Validador.validarPassword("12345"));
    }

    @Test
    @DisplayName("Solo admite montos positivos")
    void validaMonto() {
        assertDoesNotThrow(() -> Validador.validarMonto(0.01));
        assertThrows(ValidacionException.class, () -> Validador.validarMonto(0));
        assertThrows(ValidacionException.class, () -> Validador.validarMonto(-10));
    }

    @Test
    @DisplayName("Parsea montos con punto o coma decimal, sin importar el locale")
    void parseaMontosConAmbosSeparadores() throws ValidacionException {
        assertEquals(1500.50, Validador.parsearMonto("1500.50"), 0.001);
        assertEquals(1500.50, Validador.parsearMonto("1500,50"), 0.001);
        assertEquals(1500.50, Validador.parsearMonto("1500.5"), 0.001);
        assertEquals(1500000.00, Validador.parsearMonto("1.500.000,00"), 0.001);
        assertEquals(1234.56, Validador.parsearMonto("1,234.56"), 0.001);
        assertEquals(10.00, Validador.parsearMonto(" 10 "), 0.001);
    }

    @Test
    @DisplayName("Rechaza montos no numéricos")
    void rechazaMontosInvalidos() {
        assertThrows(ValidacionException.class, () -> Validador.parsearMonto("abc"));
        assertThrows(ValidacionException.class, () -> Validador.parsearMonto(""));
        assertThrows(ValidacionException.class, () -> Validador.parsearMonto(null));
    }

    @Test
    @DisplayName("Convierte la selección de menú a índice 0-based")
    void validaSeleccionMenu() throws ValidacionException {
        assertEquals(0, Validador.validarSeleccionMenu(1, 5));
        assertEquals(4, Validador.validarSeleccionMenu(5, 5));
        assertThrows(ValidacionException.class, () -> Validador.validarSeleccionMenu(0, 5));
        assertThrows(ValidacionException.class, () -> Validador.validarSeleccionMenu(6, 5));
    }
}