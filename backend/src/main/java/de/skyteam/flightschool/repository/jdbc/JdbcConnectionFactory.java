package de.skyteam.flightschool.repository.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        String driver = required("DB_DRIVER");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "JDBC-Treiber fehlt. DB_DRIVER '" + driver + "' wurde nicht im Classpath gefunden.",
                    exception
            );
        }

        String url = required("DB_URL");
        String user = required("DB_USER");
        String password = required("DB_PASSWORD");
        return new JdbcConnectionFactory(url, user, password);
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void verifyConnection() {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("select 1 from dual");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next() || resultSet.getInt(1) != 1) {
                throw new SQLException("Oracle connection test returned an unexpected result.");
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    public boolean isConnected() {
        try {
            verifyConnection();
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    public List<String> inspectSchema(List<String> expectedTables) {
        if (expectedTables == null || expectedTables.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", expectedTables.stream().map(ignored -> "?").toList());
        String tableSql = "select TABLE_NAME from USER_TABLES where TABLE_NAME in (" + placeholders + ") order by TABLE_NAME";
        String columnSql = """
                select TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION,
                       DATA_SCALE, NULLABLE, COLUMN_ID
                from USER_TAB_COLUMNS
                where TABLE_NAME in (%s)
                order by TABLE_NAME, COLUMN_ID
                """.formatted(placeholders);

        try (Connection connection = open()) {
            Set<String> actualTables = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(tableSql)) {
                bindTableNames(statement, expectedTables);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        actualTables.add(resultSet.getString("TABLE_NAME"));
                    }
                }
            }

            List<String> report = new ArrayList<>();
            for (String expectedTable : expectedTables) {
                report.add("TABLE " + expectedTable + " " + (actualTables.contains(expectedTable) ? "FOUND" : "MISSING"));
            }
            try (PreparedStatement statement = connection.prepareStatement(columnSql)) {
                bindTableNames(statement, expectedTables);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        report.add("COLUMN "
                                + resultSet.getString("TABLE_NAME") + "." + resultSet.getString("COLUMN_NAME")
                                + " " + columnType(resultSet)
                                + " NULLABLE=" + resultSet.getString("NULLABLE"));
                    }
                }
            }
            return report;
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private static void bindTableNames(PreparedStatement statement, List<String> tableNames) throws SQLException {
        for (int index = 0; index < tableNames.size(); index++) {
            statement.setString(index + 1, tableNames.get(index));
        }
    }

    private static String columnType(ResultSet resultSet) throws SQLException {
        String dataType = resultSet.getString("DATA_TYPE");
        Number precision = (Number) resultSet.getObject("DATA_PRECISION");
        Number scale = (Number) resultSet.getObject("DATA_SCALE");
        if (precision != null) {
            return scale == null ? dataType + "(" + precision + ")" : dataType + "(" + precision + "," + scale + ")";
        }
        if (dataType.contains("CHAR")) {
            return dataType + "(" + resultSet.getInt("DATA_LENGTH") + ")";
        }
        return dataType;
    }

    static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException(describe(exception), exception);
    }

    private static String describe(SQLException exception) {
        SQLException current = exception;
        while (current != null) {
            int code = Math.abs(current.getErrorCode());
            String message = String.valueOf(current.getMessage()).toUpperCase(Locale.ROOT);
            if (code == 1017 || message.contains("ORA-01017")) {
                return "Benutzername oder Passwort falsch.";
            }
            if (code == 942 || message.contains("ORA-00942")) {
                return "Tabelle oder View nicht vorhanden (ORA-00942).";
            }
            if (code == 904 || message.contains("ORA-00904")) {
                return "Spalte nicht vorhanden oder ungueltig (ORA-00904).";
            }
            if (code == 17002 || code == 12154 || code == 12514 || code == 12541
                    || message.contains("ORA-12154") || message.contains("ORA-12514")
                    || message.contains("ORA-12541") || message.contains("NETWORK ADAPTER")) {
                return "Datenbankserver nicht erreichbar oder DB_URL falsch.";
            }
            current = current.getNextException();
        }
        int code = Math.abs(exception.getErrorCode());
        return code == 0
                ? "Oracle-Datenbankverbindung oder SQL-Ausfuehrung fehlgeschlagen."
                : "Oracle-Datenbankfehler (Fehlercode " + code + ").";
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " ist fuer APP_PROFILE=oracle erforderlich.");
        }
        return value;
    }
}


