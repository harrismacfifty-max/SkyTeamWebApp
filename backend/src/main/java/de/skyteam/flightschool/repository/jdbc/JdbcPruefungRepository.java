package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.repository.PruefungRepository;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.PRUEFUNG_ANMELDEN)) {
            LocalDateTime datum = JdbcSupport.parseDateTime(request.wunschtermin());
            statement.setString(1, request.schuelerId());
            statement.setString(2, request.pruefungsart());
            statement.setTimestamp(3, JdbcSupport.timestamp(datum));
            statement.setString(4, request.pruefer());
            statement.setString(5, request.bemerkung());
            statement.registerOutParameter(6, Types.VARCHAR);
            statement.execute();
            String id = statement.getString(6);
            return new Pruefung(id, request.schuelerId(), datum, request.pruefungsart());
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public void saveErgebnis(PruefungsErgebnisRequest request) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.PRUEFUNG_ERGEBNIS_SPEICHERN)) {
            String typ = request.pruefungsart() == null || request.pruefungsart().isBlank()
                    ? "Pruefung"
                    : request.pruefungsart().trim();
            statement.setString(1, request.pruefungId());
            statement.setString(2, typ);
            statement.setTimestamp(3, JdbcSupport.timestamp(JdbcSupport.parseDateTime(request.datum())));
            statement.setInt(4, request.bestanden() ? 1 : 0);
            statement.setString(5, request.ergebnisText());
            statement.setString(6, request.notizen());
            statement.execute();
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
