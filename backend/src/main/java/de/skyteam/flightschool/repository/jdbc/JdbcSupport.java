package de.skyteam.flightschool.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class JdbcSupport {
    private JdbcSupport() {
    }

    static LocalDateTime dateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    static Timestamp timestamp(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Timestamp.valueOf(value);
    }

    static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed);
    }

    static int count(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        }
    }

    static String nextId(Connection connection, String table, String idColumn, String prefix) throws SQLException {
        String sql = "select ? || lpad(nvl(max(to_number(regexp_substr(" + idColumn + ", '[0-9]+$'))), 0) + 1, 6, '0') from "
                + table
                + " where "
                + idColumn
                + " like ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, prefix);
            statement.setString(2, prefix + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
                return prefix + "000001";
            }
        }
    }

    static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Database operation failed.", exception);
    }
}

