package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String sql = """
                merge into SCHUELER target
                using (
                    select ? as ID_SCHUELER,
                           ? as ID_AUSBILDUNG_VERTRAG,
                           ? as STARTZEIT,
                           ? as ENDZEIT,
                           ? as FLUGSTUNDE,
                           ? as THEORIESTUNDE,
                           ? as NOTIZ,
                           ? as NAME,
                           ? as VORNAME
                    from dual
                ) source
                on (target.ID_SCHUELER = source.ID_SCHUELER)
                when matched then update set
                    target.ID_AUSBILDUNG_VERTRAG = source.ID_AUSBILDUNG_VERTRAG,
                    target.STARTZEIT = source.STARTZEIT,
                    target.ENDZEIT = source.ENDZEIT,
                    target.FLUGSTUNDE = source.FLUGSTUNDE,
                    target.THEORIESTUNDE = source.THEORIESTUNDE,
                    target.NOTIZ = source.NOTIZ,
                    target.NAME = source.NAME,
                    target.VORNAME = source.VORNAME
                when not matched then insert (
                    ID_SCHUELER, ID_AUSBILDUNG_VERTRAG, STARTZEIT, ENDZEIT,
                    FLUGSTUNDE, THEORIESTUNDE, NOTIZ, NAME, VORNAME
                ) values (
                    source.ID_SCHUELER, source.ID_AUSBILDUNG_VERTRAG, source.STARTZEIT,
                    source.ENDZEIT, source.FLUGSTUNDE, source.THEORIESTUNDE,
                    source.NOTIZ, source.NAME, source.VORNAME
                )
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schueler.id());
            statement.setString(2, schueler.ausbildungsVertragId());
            statement.setTimestamp(3, JdbcSupport.timestamp(schueler.startzeit()));
            statement.setTimestamp(4, JdbcSupport.timestamp(schueler.endzeit()));
            statement.setDouble(5, schueler.flugStunden());
            statement.setDouble(6, schueler.theorieStunden());
            statement.setString(7, schueler.notiz());
            statement.setString(8, schueler.name());
            statement.setString(9, schueler.vorname());
            statement.executeUpdate();
            return schueler;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String selectContractSql = """
                select ID_AUSBILDUNG_VERTRAG
                from SCHUELER
                where ID_SCHUELER = ?
                """;
        String deletePruefungenSql = "delete from PRUEFUNG where ID_SCHUELER = ?";
        String deleteFlugPilotSql = """
                delete from FLUG_UND_PILOT
                where ID_FLUG in (select ID_FLUG from FLUG where ID_SCHUELER = ?)
                """;
        String deleteFluegeSql = "delete from FLUG where ID_SCHUELER = ?";
        String deleteKurseSql = "delete from KURSE where ID_SCHUELER = ?";
        String deleteSchuelerPilotSql = "delete from SCHUELER_UND_PILOT where ID_SCHUELER = ?";
        String deleteSchuelerSql = "delete from SCHUELER where ID_SCHUELER = ?";
        String deleteContractSql = "delete from AUSBILDUNG_VERTRAG where ID_AUSBILDUNG_VERTRAG = ?";
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                String contractId;
                try (PreparedStatement select = connection.prepareStatement(selectContractSql)) {
                    select.setString(1, id);
                    try (ResultSet resultSet = select.executeQuery()) {
                        if (!resultSet.next()) {
                            connection.rollback();
                            return false;
                        }
                        contractId = resultSet.getString("ID_AUSBILDUNG_VERTRAG");
                    }
                }

                executeDelete(connection, deletePruefungenSql, id);
                executeDelete(connection, deleteFlugPilotSql, id);
                executeDelete(connection, deleteFluegeSql, id);
                executeDelete(connection, deleteKurseSql, id);
                executeDelete(connection, deleteSchuelerPilotSql, id);
                boolean deleted = executeDelete(connection, deleteSchuelerSql, id) > 0;
                if (contractId != null && !contractId.isBlank()) {
                    executeDelete(connection, deleteContractSql, contractId);
                }
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

    private static int executeDelete(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate();
        }
    }
}
