package at.technikum_wien.mrp.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection implements DatabaseConnectionIF {

    private final String URL;
    private final String USER;
    private final String PASSWORD;
    // TRUNCATE TABLE media RESTART IDENTITY;
    // um datenbank neuzustarten
    public DatabaseConnection() {
        this.URL = "jdbc:postgresql://localhost:5432/mrpdb";
        this.USER = "mrp";
        this.PASSWORD = "mrp123";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found.", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}