package de.skyteam.flightschool.repository.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
                throw new IllegalStateException("DB_DRIVER class was not found: " + driver + ". Check EXTRA_CLASSPATH or the Docker image.");
            }
        }

        requireAll("DB_URL", "DB_USER", "DB_PASSWORD");
        String url = System.getenv("DB_URL").trim();
        String user = System.getenv("DB_USER").trim();
        String password = System.getenv("DB_PASSWORD").trim();
        return new JdbcConnectionFactory(url, user, password);
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean isReachable() {
        try (Connection connection = open()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }

    private static void requireAll(String... names) {
        List<String> missing = new ArrayList<>();
        for (String name : names) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "APP_PROFILE=oracle requires DB_URL, DB_USER and DB_PASSWORD. Missing: " + String.join(", ", missing)
            );
        }
    }
}


