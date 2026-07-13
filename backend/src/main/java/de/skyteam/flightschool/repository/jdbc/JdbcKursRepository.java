package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.repository.KursRepository;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class JdbcKursRepository implements KursRepository {
    private final JdbcConnectionFactory connections;

    public JdbcKursRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Kurs> findBySchueler(String id) {
        String sql = """
                select TYP, LEHRER, TAG, ID_KURS, ID_SCHUELER
                from KURSE
                where ID_SCHUELER = ?
                order by TAG, ID_KURS
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Kurs> result = new ArrayList<>();
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
    public Kurs createTheorieKurs(TheorieBuchungRequest request) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.THEORIE_BUCHEN)) {
            statement.setString(1, request.schuelerId());
            statement.setString(2, request.thema());
            statement.setString(3, request.dozent());
            statement.setString(4, request.termin());
            statement.setInt(5, request.dauerMinuten());
            statement.registerOutParameter(6, Types.VARCHAR);
            statement.execute();
            String id = statement.getString(6);
            return new Kurs(id, request.schuelerId(), request.thema(), request.dozent(), request.termin(), request.dauerMinuten());
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public boolean storniereTheorieKurs(String schuelerId, String kursId) {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall(JdbcProcedureCalls.THEORIE_STORNIEREN)) {
            statement.setString(1, schuelerId);
            statement.setString(2, kursId);
            statement.registerOutParameter(3, Types.NUMERIC);
            statement.execute();
            return statement.getInt(3) > 0;
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    @Override
    public double countTheorieStunden(String id) {
        String sql = """
                select nvl(THEORIESTUNDE, 0) as THEORIESTUNDE
                from SCHUELER
                where ID_SCHUELER = ?
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("THEORIESTUNDE");
                }
                return 0.0;
            }
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }

    private static Kurs map(ResultSet resultSet) throws SQLException {
        return new Kurs(
                resultSet.getString("ID_KURS"),
                resultSet.getString("ID_SCHUELER"),
                resultSet.getString("TYP"),
                resultSet.getString("LEHRER"),
                resultSet.getString("TAG")
        );
    }
}
