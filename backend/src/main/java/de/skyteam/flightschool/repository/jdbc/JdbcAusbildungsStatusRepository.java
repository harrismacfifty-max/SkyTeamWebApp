package de.skyteam.flightschool.repository.jdbc;

import de.skyteam.flightschool.model.AusbildungsStatus;
import de.skyteam.flightschool.repository.AusbildungsStatusRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcAusbildungsStatusRepository implements AusbildungsStatusRepository {
    private final JdbcConnectionFactory connections;

    public JdbcAusbildungsStatusRepository(JdbcConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public Optional<AusbildungsStatus> findBySchuelerId(String id) {
        String sql = """
                select s.ID_SCHUELER,
                       s.ID_AUSBILDUNG_VERTRAG,
                       av.STATUS as VERTRAGS_STATUS,
                       nvl(s.THEORIESTUNDE, 0) as THEORIESTUNDE,
                       nvl(s.FLUGSTUNDE, 0) as FLUGSTUNDE,
                       (
                           select count(*)
                           from PRUEFUNG p
                           where p.ID_SCHUELER = s.ID_SCHUELER
                             and lower(p.TYP) like '%theorie%'
                             and lower(p.TYP) like '%bestanden%'
                             and lower(p.TYP) not like '%nicht bestanden%'
                       ) as THEORIE_BESTANDEN,
                       (
                           select count(*)
                           from PRUEFUNG p
                           where p.ID_SCHUELER = s.ID_SCHUELER
                             and (lower(p.TYP) like '%praxis%' or lower(p.TYP) like '%praktisch%')
                             and lower(p.TYP) like '%bestanden%'
                             and lower(p.TYP) not like '%nicht bestanden%'
                       ) as PRAXIS_BESTANDEN
                from SCHUELER s
                left join AUSBILDUNG_VERTRAG av on av.ID_AUSBILDUNG_VERTRAG = s.ID_AUSBILDUNG_VERTRAG
                where s.ID_SCHUELER = ?
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    double theorieStunden = resultSet.getDouble("THEORIESTUNDE");
                    double flugStunden = resultSet.getDouble("FLUGSTUNDE");
                    boolean theorieBestanden = resultSet.getInt("THEORIE_BESTANDEN") > 0;
                    boolean praxisBestanden = resultSet.getInt("PRAXIS_BESTANDEN") > 0;
                    return Optional.of(new AusbildungsStatus(
                            resultSet.getString("ID_SCHUELER"),
                            resultSet.getString("ID_AUSBILDUNG_VERTRAG"),
                            resultSet.getString("VERTRAGS_STATUS"),
                            theorieStunden,
                            flugStunden,
                            theorieBestanden,
                            praxisBestanden,
                            theorieStunden >= 120.0 && flugStunden >= 45.0 && theorieBestanden
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw JdbcSupport.failure(exception);
        }
    }
}

