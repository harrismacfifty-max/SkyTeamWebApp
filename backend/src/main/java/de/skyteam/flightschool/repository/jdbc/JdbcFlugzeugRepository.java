package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Flugzeug;
import de.skyteam.flightschool.repository.FlugzeugRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcFlugzeugRepository implements FlugzeugRepository {
    private final JdbcConnectionFactory connections;

    public JdbcFlugzeugRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Flugzeug> findAll() {
        String sql = """
                select ID_FLUGZEUG, BAUJAHR, VERFUEGBARKEIT, STATUS
                from FLUGZEUG
                order by ID_FLUGZEUG
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Flugzeug> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public Optional<Flugzeug> findById(String id) {
        String sql = """
                select ID_FLUGZEUG, BAUJAHR, VERFUEGBARKEIT, STATUS
                from FLUGZEUG
                where ID_FLUGZEUG = ?
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

    private static Flugzeug map(ResultSet resultSet) throws SQLException {
        return new Flugzeug(
                resultSet.getString("ID_FLUGZEUG"),
                JdbcSupport.dateTime(resultSet, "BAUJAHR"),
                resultSet.getString("VERFUEGBARKEIT"),
                resultSet.getString("STATUS")
        );
    }
}

