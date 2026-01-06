package at.technikum_wien.mrp.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnectionIF {
    Connection getConnection() throws SQLException;
}