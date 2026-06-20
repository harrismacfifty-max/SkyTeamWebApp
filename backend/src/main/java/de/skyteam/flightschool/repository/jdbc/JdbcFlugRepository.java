package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.repository.FlugRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String insertFlightSql = """
                insert into FLUG (
                    ID_FLUG, ID_SCHUELER, ID_FLUGZEUG, DATUM, STARTZEIT, ENDZEIT,
                    START_FLUGHAFEN, ZIEL_FLUGHAFEN, FLUG_ART
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertPilotSql = """
                insert into FLUG_UND_PILOT (ID_FLUG, ID_PILOT)
                values (?, ?)
                """;
        String updateHoursSql = """
                update SCHUELER
                set FLUGSTUNDE = nvl(FLUGSTUNDE, 0) + ?
                where ID_SCHUELER = ?
                """;
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            String id = JdbcSupport.nextId(connection, "FLUG", "ID_FLUG", "FL");
            LocalDateTime start = JdbcSupport.parseDateTime(request.termin());
            LocalDateTime end = start.plusMinutes(request.dauerMinuten());
            try (PreparedStatement insertFlight = connection.prepareStatement(insertFlightSql);
                 PreparedStatement insertPilot = connection.prepareStatement(insertPilotSql);
                 PreparedStatement updateHours = connection.prepareStatement(updateHoursSql)) {
                insertFlight.setString(1, id);
                insertFlight.setString(2, request.schuelerId());
                insertFlight.setString(3, request.flugzeugId());
                insertFlight.setTimestamp(4, JdbcSupport.timestamp(start));
                insertFlight.setTimestamp(5, JdbcSupport.timestamp(start));
                insertFlight.setTimestamp(6, JdbcSupport.timestamp(end));
                insertFlight.setString(7, "EDDV");
                insertFlight.setString(8, "EDDV");
                insertFlight.setString(9, request.ausbildungsinhalt());
                insertFlight.executeUpdate();

                if (request.fluglehrer() != null && !request.fluglehrer().isBlank()) {
                    insertPilot.setString(1, id);
                    insertPilot.setString(2, request.fluglehrer().trim());
                    insertPilot.executeUpdate();
                }

                updateHours.setDouble(1, request.dauerMinuten() / 60.0);
                updateHours.setString(2, request.schuelerId());
                updateHours.executeUpdate();
                connection.commit();
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
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean stornierePraxisFlug(String schuelerId, String flugId) {
        String selectSql = """
                select STARTZEIT, ENDZEIT
                from FLUG
                where ID_FLUG = ? and ID_SCHUELER = ?
                """;
        String deletePilotSql = """
                delete from FLUG_UND_PILOT
                where ID_FLUG = ?
                """;
        String deleteFlugSql = """
                delete from FLUG
                where ID_FLUG = ? and ID_SCHUELER = ?
                """;
        String updateHoursSql = """
                update SCHUELER
                set FLUGSTUNDE = greatest(0, nvl(FLUGSTUNDE, 0) - ?)
                where ID_SCHUELER = ?
                """;
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(selectSql);
                 PreparedStatement deletePilot = connection.prepareStatement(deletePilotSql);
                 PreparedStatement deleteFlug = connection.prepareStatement(deleteFlugSql);
                 PreparedStatement updateHours = connection.prepareStatement(updateHoursSql)) {
                select.setString(1, flugId);
                select.setString(2, schuelerId);
                double hours;
                try (ResultSet resultSet = select.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return false;
                    }
                    LocalDateTime start = JdbcSupport.dateTime(resultSet, "STARTZEIT");
                    LocalDateTime end = JdbcSupport.dateTime(resultSet, "ENDZEIT");
                    if (start == null || end == null || end.isBefore(start)) {
                        hours = 0.0;
                    } else {
                        hours = java.time.Duration.between(start, end).toMinutes() / 60.0;
                    }
                }

                deletePilot.setString(1, flugId);
                deletePilot.executeUpdate();

                deleteFlug.setString(1, flugId);
                deleteFlug.setString(2, schuelerId);
                boolean deleted = deleteFlug.executeUpdate() > 0;

                updateHours.setDouble(1, hours);
                updateHours.setString(2, schuelerId);
                updateHours.executeUpdate();
                connection.commit();
                return deleted;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
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
