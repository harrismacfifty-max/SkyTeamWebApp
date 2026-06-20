package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.repository.AusbildungsVertragRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcAusbildungsVertragRepository implements AusbildungsVertragRepository {
    private final JdbcConnectionFactory connections;

    public JdbcAusbildungsVertragRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public Optional<AusbildungsVertrag> findBySchuelerId(String id) {
        String sql = """
                select av.ID_AUSBILDUNG_VERTRAG, av.ID_SCHULE, av.STARTZEIT, av.ENDZEIT,
                       av.STATUS, av.NOTIZ
                from AUSBILDUNG_VERTRAG av
                join SCHUELER s on s.ID_AUSBILDUNG_VERTRAG = av.ID_AUSBILDUNG_VERTRAG
                where s.ID_SCHUELER = ?
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
            return JdbcSupport.nextId(connection, "AUSBILDUNG_VERTRAG", "ID_AUSBILDUNG_VERTRAG", "AV");
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public AusbildungsVertrag save(AusbildungsVertrag vertrag) {
        String sql = """
                merge into AUSBILDUNG_VERTRAG target
                using (
                    select ? as ID_AUSBILDUNG_VERTRAG,
                           ? as ID_SCHULE,
                           ? as STARTZEIT,
                           ? as ENDZEIT,
                           ? as STATUS,
                           ? as NOTIZ
                    from dual
                ) source
                on (target.ID_AUSBILDUNG_VERTRAG = source.ID_AUSBILDUNG_VERTRAG)
                when matched then update set
                    target.ID_SCHULE = source.ID_SCHULE,
                    target.STARTZEIT = source.STARTZEIT,
                    target.ENDZEIT = source.ENDZEIT,
                    target.STATUS = source.STATUS,
                    target.NOTIZ = source.NOTIZ
                when not matched then insert (
                    ID_AUSBILDUNG_VERTRAG, ID_SCHULE, STARTZEIT, ENDZEIT, STATUS, NOTIZ
                ) values (
                    source.ID_AUSBILDUNG_VERTRAG, source.ID_SCHULE, source.STARTZEIT,
                    source.ENDZEIT, source.STATUS, source.NOTIZ
                )
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vertrag.id());
            statement.setString(2, vertrag.schuleId());
            statement.setTimestamp(3, JdbcSupport.timestamp(vertrag.startzeit()));
            statement.setTimestamp(4, JdbcSupport.timestamp(vertrag.endzeit()));
            statement.setString(5, vertrag.status());
            statement.setString(6, vertrag.notiz());
            statement.executeUpdate();
            return vertrag;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public void updateStatus(String id, String status) {
        String sql = """
                update AUSBILDUNG_VERTRAG
                set STATUS = ?
                where ID_AUSBILDUNG_VERTRAG = ?
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static AusbildungsVertrag map(ResultSet resultSet) throws SQLException {
        return new AusbildungsVertrag(
                resultSet.getString("ID_AUSBILDUNG_VERTRAG"),
                resultSet.getString("ID_SCHULE"),
                JdbcSupport.dateTime(resultSet, "STARTZEIT"),
                JdbcSupport.dateTime(resultSet, "ENDZEIT"),
                resultSet.getString("STATUS"),
                resultSet.getString("NOTIZ")
        );
    }
}
