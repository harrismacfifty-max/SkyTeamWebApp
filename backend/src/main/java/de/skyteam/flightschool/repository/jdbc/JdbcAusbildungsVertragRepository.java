package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.repository.AusbildungsVertragRepository;
import java.sql.CallableStatement;
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
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.AUSBILDUNGSVERTRAG_SPEICHERN)) {
            statement.setString(1, vertrag.id());
            statement.setString(2, vertrag.schuleId());
            statement.setTimestamp(3, JdbcSupport.timestamp(vertrag.startzeit()));
            statement.setTimestamp(4, JdbcSupport.timestamp(vertrag.endzeit()));
            statement.setString(5, vertrag.status());
            statement.setString(6, vertrag.notiz());
            statement.execute();
            return vertrag;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public void updateStatus(String id, String status) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.AUSBILDUNGSVERTRAG_STATUS)) {
            statement.setString(1, id);
            statement.setString(2, status);
            statement.execute();
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
