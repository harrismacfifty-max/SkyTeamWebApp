package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Aircraft;
import de.skyteam.flightschool.repository.AircraftRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcAircraftRepository implements AircraftRepository {
    private final JdbcConnectionFactory connections;

    public JdbcAircraftRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Aircraft> findAll() {
        String sql = "select id, registration, model, status, total_hours from fs_aircraft order by registration";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Aircraft> aircraft = new ArrayList<>();
            while (resultSet.next()) {
                aircraft.add(map(resultSet));
            }
            return aircraft;
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public Optional<Aircraft> findById(long id) {
        String sql = "select id, registration, model, status, total_hours from fs_aircraft where id = ?";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    @Override
    public Aircraft create(Aircraft aircraft) {
        String sql = """
                insert into fs_aircraft (id, registration, model, status, total_hours)
                values (fs_aircraft_seq.nextval, ?, ?, ?, ?)
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, aircraft.registration());
            statement.setString(2, aircraft.model());
            statement.setString(3, aircraft.status());
            statement.setInt(4, aircraft.totalHours());
            statement.executeUpdate();
            long id = currentSequenceValue(connection, "fs_aircraft_seq");
            return new Aircraft(id, aircraft.registration(), aircraft.model(), aircraft.status(), aircraft.totalHours());
        } catch (SQLException exception) {
            throw failure(exception);
        }
    }

    private static Aircraft map(ResultSet resultSet) throws SQLException {
        return new Aircraft(
                resultSet.getLong("id"),
                resultSet.getString("registration"),
                resultSet.getString("model"),
                resultSet.getString("status"),
                resultSet.getInt("total_hours")
        );
    }

    private static long currentSequenceValue(Connection connection, String sequence) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select " + sequence + ".currval from dual")) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new SQLException("Sequence did not return a value: " + sequence);
        }
    }

    private static IllegalStateException failure(SQLException exception) {
        return new IllegalStateException("Database operation failed.", exception);
    }
}


