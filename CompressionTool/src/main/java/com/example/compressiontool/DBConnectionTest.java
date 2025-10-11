package com.example.compressiontool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionTest {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://dpg-d3ka6el6ubrc73dq7ccg-a.singapore-postgres.render.com:5432/compressdb?sslmode=require";
        String username = "compressdb_user";
        String password = "ewmxgW3UHPRwyJbiU7o1LGOAtvALKSKU";

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("Database connection successful!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
