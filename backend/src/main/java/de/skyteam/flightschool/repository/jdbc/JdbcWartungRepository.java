package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Wartung;
import de.skyteam.flightschool.repository.WartungRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcWartungRepository implements WartungRepository {
    private final JdbcConnectionFactory connections;

    public JdbcWartungRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Wartung> findAll() {
        String sql = """
                select w.ID_WARTUNG, w.DATUM, w.TYP, w.NOTIZ, w.ID_BUCHUNG, wf.ID_FLUGZEUG
                from WARTUNG w
                left join WARTUNG_UND_FLUGZEUG wf on wf.ID_WARTUNG = w.ID_WARTUNG
                order by w.DATUM, w.ID_WARTUNG
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Wartung> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public List<Wartung> findByFlugzeug(String flugzeugId) {
        String sql = """
                select w.ID_WARTUNG, w.DATUM, w.TYP, w.NOTIZ, w.ID_BUCHUNG, wf.ID_FLUGZEUG
                from WARTUNG w
                join WARTUNG_UND_FLUGZEUG wf on wf.ID_WARTUNG = w.ID_WARTUNG
                where wf.ID_FLUGZEUG = ?
                order by w.DATUM, w.ID_WARTUNG
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, flugzeugId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Wartung> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(map(resultSet));
                }
                return result;
            }
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public Optional<Wartung> findById(String id) {
        String sql = """
                select w.ID_WARTUNG, w.DATUM, w.TYP, w.NOTIZ, w.ID_BUCHUNG, wf.ID_FLUGZEUG
                from WARTUNG w
                left join WARTUNG_UND_FLUGZEUG wf on wf.ID_WARTUNG = w.ID_WARTUNG
                where w.ID_WARTUNG = ?
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

    private static Wartung map(ResultSet resultSet) throws SQLException {
        return new Wartung(
                resultSet.getString("ID_WARTUNG"),
                JdbcSupport.dateTime(resultSet, "DATUM"),
                resultSet.getString("TYP"),
                resultSet.getString("NOTIZ"),
                resultSet.getString("ID_BUCHUNG"),
                resultSet.getString("ID_FLUGZEUG")
        );
    }
}

