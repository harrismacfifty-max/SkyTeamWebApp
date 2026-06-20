package de.skyteam.flightschool.repository.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class JdbcConnectionFactory {
    private final String url;
    private final String user;
    private final String password;

    private JdbcConnectionFactory(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static JdbcConnectionFactory fromEnvironment() {
        String driver = System.getenv("DB_DRIVER");
        if (driver != null && !driver.isBlank()) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("DB_DRIVER class was not found: " + driver, exception);
            }
        }

        String url = required("DB_URL");
        String user = required("DB_USER");
        String password = required("DB_PASSWORD");
        return new JdbcConnectionFactory(url, user, password);
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for APP_PROFILE=oracle.");
        }
        return value;
    }
}


