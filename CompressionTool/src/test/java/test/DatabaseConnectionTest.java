package test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionTest {
    private static final String URL = "jdbc:postgresql://dpg-d3ka6el6ubrc73dq7ccg-a.singapore-postgres.render.com:5432/compressdb?sslmode=require";
    private static final String USERNAME = "compressdb_user";
    private static final String PASSWORD = "ewmxgW3UHPRwyJbiU7o1LGOAtvALKSKU";

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
                if (connection != null) {
                    System.out.println("Successfully connected to the PostgreSQL database!");
                } else {
                    System.out.println("Failed to connect to the database.");
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Connection failed. Check the URL, username, and password.");
            e.printStackTrace();
        }
    }
}
