package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.repository.KursRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String insertSql = """
                insert into KURSE (TYP, LEHRER, TAG, ID_KURS, ID_SCHUELER)
                values (?, ?, ?, ?, ?)
                """;
        String updateHoursSql = """
                update SCHUELER
                set THEORIESTUNDE = nvl(THEORIESTUNDE, 0) + ?
                where ID_SCHUELER = ?
                """;
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            String id = JdbcSupport.nextId(connection, "KURSE", "ID_KURS", "KTB");
            try (PreparedStatement insert = connection.prepareStatement(insertSql);
                 PreparedStatement updateHours = connection.prepareStatement(updateHoursSql)) {
                insert.setString(1, request.thema());
                insert.setString(2, request.dozent());
                insert.setString(3, request.termin());
                insert.setString(4, id);
                insert.setString(5, request.schuelerId());
                insert.executeUpdate();

                updateHours.setDouble(1, request.dauerMinuten() / 60.0);
                updateHours.setString(2, request.schuelerId());
                updateHours.executeUpdate();
                connection.commit();
                return new Kurs(id, request.schuelerId(), request.thema(), request.dozent(), request.termin());
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

