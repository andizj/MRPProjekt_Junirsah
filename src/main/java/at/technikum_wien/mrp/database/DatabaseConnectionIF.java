package at.technikum_wien.mrp.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnectionIF {
    Connection getConnection() throws SQLException;
}