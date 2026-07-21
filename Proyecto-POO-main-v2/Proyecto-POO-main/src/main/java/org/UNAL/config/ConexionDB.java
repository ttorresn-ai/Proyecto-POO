package org.UNAL.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {
    public static Connection conectar() {
        String url = "jdbc:mysql://localhost:3306/nexus_finance";
        String usuario = "root";
        String password = "password";

        try { 
            return DriverManager.getConnection(url, usuario, password);
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}