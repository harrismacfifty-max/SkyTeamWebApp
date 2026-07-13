package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcSchuelerRepository implements SchuelerRepository {
    private final JdbcConnectionFactory connections;

    public JdbcSchuelerRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Schueler> findAll() {
        String sql = """
                select ID_SCHUELER, ID_AUSBILDUNG_VERTRAG, STARTZEIT, ENDZEIT,
                       FLUGSTUNDE, THEORIESTUNDE, NOTIZ, NAME, VORNAME
                from SCHUELER
                order by NAME, VORNAME, ID_SCHUELER
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Schueler> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public Optional<Schueler> findById(String id) {
        String sql = """
                select ID_SCHUELER, ID_AUSBILDUNG_VERTRAG, STARTZEIT, ENDZEIT,
                       FLUGSTUNDE, THEORIESTUNDE, NOTIZ, NAME, VORNAME
                from SCHUELER
                where ID_SCHUELER = ?
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

    @Override
    public String nextId() {
        try (Connection connection = connections.open()) {
            return JdbcSupport.nextId(connection, "SCHUELER", "ID_SCHUELER", "SC");
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public Schueler save(Schueler schueler) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.SCHUELER_SPEICHERN)) {
            statement.setString(1, schueler.id());
            statement.setString(2, schueler.ausbildungsVertragId());
            statement.setTimestamp(3, JdbcSupport.timestamp(schueler.startzeit()));
            statement.setTimestamp(4, JdbcSupport.timestamp(schueler.endzeit()));
            statement.setDouble(5, schueler.flugStunden());
            statement.setDouble(6, schueler.theorieStunden());
            statement.setString(7, schueler.notiz());
            statement.setString(8, schueler.name());
            statement.setString(9, schueler.vorname());
            statement.execute();
            return schueler;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.SCHUELER_LOESCHEN)) {
            statement.setString(1, id);
            statement.registerOutParameter(2, Types.NUMERIC);
            statement.execute();
            return statement.getInt(2) > 0;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static Schueler map(ResultSet resultSet) throws SQLException {
        return new Schueler(
                resultSet.getString("ID_SCHUELER"),
                resultSet.getString("ID_AUSBILDUNG_VERTRAG"),
                JdbcSupport.dateTime(resultSet, "STARTZEIT"),
                JdbcSupport.dateTime(resultSet, "ENDZEIT"),
                resultSet.getDouble("FLUGSTUNDE"),
                resultSet.getDouble("THEORIESTUNDE"),
                resultSet.getString("NOTIZ"),
                resultSet.getString("NAME"),
                resultSet.getString("VORNAME")
        );
    }

}
