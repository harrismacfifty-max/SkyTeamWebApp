package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Pilot;
import de.skyteam.flightschool.repository.PilotRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcPilotRepository implements PilotRepository {
    private final JdbcConnectionFactory connections;

    public JdbcPilotRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Pilot> findAll() {
        String sql = """
                select ID_PILOT, LIZENZ, GEHALT, LEHRER, VERFUEGBAR, NAME, VORNAME, TELEFON, EMAIL
                from PILOT
                order by NAME, VORNAME, ID_PILOT
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Pilot> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public Optional<Pilot> findById(String id) {
        String sql = """
                select ID_PILOT, LIZENZ, GEHALT, LEHRER, VERFUEGBAR, NAME, VORNAME, TELEFON, EMAIL
                from PILOT
                where ID_PILOT = ?
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static Pilot map(ResultSet resultSet) throws SQLException {
        return new Pilot(
                resultSet.getString("ID_PILOT"),
                resultSet.getString("LIZENZ"),
                resultSet.getBigDecimal("GEHALT"),
                resultSet.getString("LEHRER"),
                resultSet.getString("VERFUEGBAR"),
                resultSet.getString("NAME"),
                resultSet.getString("VORNAME"),
                resultSet.getString("TELEFON"),
                resultSet.getString("EMAIL")
        );
    }
}

