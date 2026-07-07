package org.UNAL.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    public static Connection conectar() {
        String url = "DB URL";
        String usuario = "DB USUARIO";
        String password = "DB PASSWORD";

        try { 
            return DriverManager.getConnection(url, usuario, password);
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}