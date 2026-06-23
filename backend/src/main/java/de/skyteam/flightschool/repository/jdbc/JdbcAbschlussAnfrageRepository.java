package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.AbschlussAnfrage;
import de.skyteam.flightschool.model.AbschlussAnfrageStatus;
import de.skyteam.flightschool.repository.AbschlussAnfrageRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcAbschlussAnfrageRepository implements AbschlussAnfrageRepository {
    private final JdbcConnectionFactory connections;

    public JdbcAbschlussAnfrageRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public Optional<AbschlussAnfrage> findById(String id) {
        String sql = """
                select ID_ABSCHLUSS_ANFRAGE, ID_SCHUELER, STATUS, BEGRUENDUNG, ANGEFRAGT_AM, GEPRUEFT_AM
                from ABSCHLUSS_ANFRAGE
                where ID_ABSCHLUSS_ANFRAGE = ?
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
    public Optional<AbschlussAnfrage> findLatestBySchuelerId(String schuelerId) {
        String sql = """
                select ID_ABSCHLUSS_ANFRAGE, ID_SCHUELER, STATUS, BEGRUENDUNG, ANGEFRAGT_AM, GEPRUEFT_AM
                from ABSCHLUSS_ANFRAGE
                where ID_SCHUELER = ?
                order by ANGEFRAGT_AM desc
                fetch first 1 rows only
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schuelerId);
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
    public List<AbschlussAnfrage> findAll() {
        String sql = """
                select ID_ABSCHLUSS_ANFRAGE, ID_SCHUELER, STATUS, BEGRUENDUNG, ANGEFRAGT_AM, GEPRUEFT_AM
                from ABSCHLUSS_ANFRAGE
                order by ANGEFRAGT_AM desc
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<AbschlussAnfrage> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public List<AbschlussAnfrage> findOffene() {
        String sql = """
                select ID_ABSCHLUSS_ANFRAGE, ID_SCHUELER, STATUS, BEGRUENDUNG, ANGEFRAGT_AM, GEPRUEFT_AM
                from ABSCHLUSS_ANFRAGE
                where STATUS in ('ANGEFRAGT', 'IN_PRUEFUNG')
                order by ANGEFRAGT_AM desc
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<AbschlussAnfrage> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public String nextId() {
        try (Connection connection = connections.open()) {
            return JdbcSupport.nextId(connection, "ABSCHLUSS_ANFRAGE", "ID_ABSCHLUSS_ANFRAGE", "AA");
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public AbschlussAnfrage save(AbschlussAnfrage anfrage) {
        String sql = """
                merge into ABSCHLUSS_ANFRAGE target
                using (
                    select ? as ID_ABSCHLUSS_ANFRAGE,
                           ? as ID_SCHUELER,
                           ? as STATUS,
                           ? as BEGRUENDUNG,
                           ? as ANGEFRAGT_AM,
                           ? as GEPRUEFT_AM
                    from dual
                ) source
                on (target.ID_ABSCHLUSS_ANFRAGE = source.ID_ABSCHLUSS_ANFRAGE)
                when matched then update set
                    target.ID_SCHUELER = source.ID_SCHUELER,
                    target.STATUS = source.STATUS,
                    target.BEGRUENDUNG = source.BEGRUENDUNG,
                    target.ANGEFRAGT_AM = source.ANGEFRAGT_AM,
                    target.GEPRUEFT_AM = source.GEPRUEFT_AM
                when not matched then insert (
                    ID_ABSCHLUSS_ANFRAGE, ID_SCHUELER, STATUS, BEGRUENDUNG, ANGEFRAGT_AM, GEPRUEFT_AM
                ) values (
                    source.ID_ABSCHLUSS_ANFRAGE, source.ID_SCHUELER, source.STATUS,
                    source.BEGRUENDUNG, source.ANGEFRAGT_AM, source.GEPRUEFT_AM
                )
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, anfrage.id());
            statement.setString(2, anfrage.schuelerId());
            statement.setString(3, anfrage.status().name());
            statement.setString(4, anfrage.begruendung());
            statement.setTimestamp(5, JdbcSupport.timestamp(anfrage.angefragtAm()));
            statement.setTimestamp(6, JdbcSupport.timestamp(anfrage.geprueftAm()));
            statement.executeUpdate();
            return anfrage;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static AbschlussAnfrage map(ResultSet resultSet) throws SQLException {
        return new AbschlussAnfrage(
                resultSet.getString("ID_ABSCHLUSS_ANFRAGE"),
                resultSet.getString("ID_SCHUELER"),
                AbschlussAnfrageStatus.valueOf(resultSet.getString("STATUS")),
                resultSet.getString("BEGRUENDUNG"),
                JdbcSupport.dateTime(resultSet, "ANGEFRAGT_AM"),
                JdbcSupport.dateTime(resultSet, "GEPRUEFT_AM")
        );
    }
}
