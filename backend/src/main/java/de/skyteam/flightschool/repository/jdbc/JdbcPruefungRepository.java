package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.repository.PruefungRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class JdbcPruefungRepository implements PruefungRepository {
    private final JdbcConnectionFactory connections;

    public JdbcPruefungRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Pruefung> findBySchueler(String id) {
        String sql = """
                select DATUM, TYP, ID_PRUEFUNG, ID_SCHUELER
                from PRUEFUNG
                where ID_SCHUELER = ?
                order by DATUM, ID_PRUEFUNG
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Pruefung> result = new ArrayList<>();
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
    public Pruefung createPruefung(PruefungAnmeldungRequest request) {
        String sql = """
                insert into PRUEFUNG (DATUM, TYP, ID_PRUEFUNG, ID_SCHUELER)
                values (?, ?, ?, ?)
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String id = JdbcSupport.nextId(connection, "PRUEFUNG", "ID_PRUEFUNG", "PRB");
            LocalDateTime datum = JdbcSupport.parseDateTime(request.wunschtermin());
            statement.setTimestamp(1, JdbcSupport.timestamp(datum));
            statement.setString(2, request.pruefungsart());
            statement.setString(3, id);
            statement.setString(4, request.schuelerId());
            statement.executeUpdate();
            return new Pruefung(id, request.schuelerId(), datum, request.pruefungsart());
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public void saveErgebnis(PruefungsErgebnisRequest request) {
        String sql = """
                update PRUEFUNG
                set TYP = ?, DATUM = ?
                where ID_PRUEFUNG = ?
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String status = request.bestanden() ? "bestanden" : "nicht bestanden";
            String typ = request.pruefungsart() == null || request.pruefungsart().isBlank()
                    ? "Pruefung - " + status
                    : request.pruefungsart().trim() + " - " + status;
            statement.setString(1, typ);
            statement.setTimestamp(2, JdbcSupport.timestamp(JdbcSupport.parseDateTime(request.datum())));
            statement.setString(3, request.pruefungId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean hasBestandeneTheorie(String id) {
        String sql = """
                select count(*)
                from PRUEFUNG
                where ID_SCHUELER = ?
                  and lower(TYP) like '%theorie%'
                  and lower(TYP) like '%bestanden%'
                  and lower(TYP) not like '%nicht bestanden%'
                """;
        try (Connection connection = connections.open()) {
            return JdbcSupport.count(connection, sql, id) > 0;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean hasBestandenePraxis(String id) {
        String sql = """
                select count(*)
                from PRUEFUNG
                where ID_SCHUELER = ?
                  and (lower(TYP) like '%praxis%' or lower(TYP) like '%praktisch%')
                  and lower(TYP) like '%bestanden%'
                  and lower(TYP) not like '%nicht bestanden%'
                """;
        try (Connection connection = connections.open()) {
            return JdbcSupport.count(connection, sql, id) > 0;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static Pruefung map(ResultSet resultSet) throws SQLException {
        return new Pruefung(
                resultSet.getString("ID_PRUEFUNG"),
                resultSet.getString("ID_SCHUELER"),
                JdbcSupport.dateTime(resultSet, "DATUM"),
                resultSet.getString("TYP")
        );
    }
}

