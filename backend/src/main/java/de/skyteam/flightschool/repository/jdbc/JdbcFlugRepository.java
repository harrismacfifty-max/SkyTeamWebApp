package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.repository.FlugRepository;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class JdbcFlugRepository implements FlugRepository {
    private final JdbcConnectionFactory connections;

    public JdbcFlugRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Flug> findBySchueler(String id) {
        String sql = """
                select ID_FLUG, ID_SCHUELER, ID_FLUGZEUG, DATUM, STARTZEIT, ENDZEIT,
                       START_FLUGHAFEN, ZIEL_FLUGHAFEN, FLUG_ART
                from FLUG
                where ID_SCHUELER = ?
                order by DATUM, ID_FLUG
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Flug> result = new ArrayList<>();
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
    public Flug createPraxisFlug(PraxisBuchungRequest request) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.PRAXIS_BUCHEN)) {
            LocalDateTime start = JdbcSupport.parseDateTime(request.termin());
            LocalDateTime end = start.plusMinutes(request.dauerMinuten());
            statement.setString(1, request.schuelerId());
            statement.setString(2, request.flugzeugId());
            statement.setString(3, request.fluglehrer().trim());
            statement.setTimestamp(4, JdbcSupport.timestamp(start));
            statement.setInt(5, request.dauerMinuten());
            statement.setString(6, "EDDV");
            statement.setString(7, "EDDV");
            statement.setString(8, request.ausbildungsinhalt());
            statement.registerOutParameter(9, Types.VARCHAR);
            statement.execute();
            String id = statement.getString(9);
            return new Flug(
                    id,
                    request.schuelerId(),
                    request.flugzeugId(),
                    start,
                    start,
                    end,
                    "EDDV",
                    "EDDV",
                    request.ausbildungsinhalt()
            );
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean stornierePraxisFlug(String schuelerId, String flugId) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.PRAXIS_STORNIEREN)) {
            statement.setString(1, schuelerId);
            statement.setString(2, flugId);
            statement.registerOutParameter(3, Types.NUMERIC);
            statement.execute();
            return statement.getInt(3) > 0;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public double countFlugstunden(String id) {
        String sql = """
                select nvl(FLUGSTUNDE, 0) as FLUGSTUNDE
                from SCHUELER
                where ID_SCHUELER = ?
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("FLUGSTUNDE");
                }
                return 0.0;
            }
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static Flug map(ResultSet resultSet) throws SQLException {
        return new Flug(
                resultSet.getString("ID_FLUG"),
                resultSet.getString("ID_SCHUELER"),
                resultSet.getString("ID_FLUGZEUG"),
                JdbcSupport.dateTime(resultSet, "DATUM"),
                JdbcSupport.dateTime(resultSet, "STARTZEIT"),
                JdbcSupport.dateTime(resultSet, "ENDZEIT"),
                resultSet.getString("START_FLUGHAFEN"),
                resultSet.getString("ZIEL_FLUGHAFEN"),
                resultSet.getString("FLUG_ART")
        );
    }
}
