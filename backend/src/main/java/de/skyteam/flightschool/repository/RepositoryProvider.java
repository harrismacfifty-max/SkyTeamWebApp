package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.repository.jdbc.JdbcAusbildungsStatusRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcAusbildungsVertragRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcConnectionFactory;
import de.skyteam.flightschool.repository.jdbc.JdbcFlugRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcFlugzeugRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcKursRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcPilotRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcPruefungRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcSchuelerRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcWartungRepository;
import de.skyteam.flightschool.repository.memory.DevFlightSchoolData;
import de.skyteam.flightschool.repository.memory.InMemoryAircraftRepository;
import de.skyteam.flightschool.repository.memory.InMemoryAusbildungsStatusRepository;
import de.skyteam.flightschool.repository.memory.InMemoryAusbildungsVertragRepository;
import de.skyteam.flightschool.repository.memory.InMemoryFlugRepository;
import de.skyteam.flightschool.repository.memory.InMemoryFlugzeugRepository;
import de.skyteam.flightschool.repository.memory.InMemoryKursRepository;
import de.skyteam.flightschool.repository.memory.InMemoryLessonRepository;
import de.skyteam.flightschool.repository.memory.InMemoryPilotRepository;
import de.skyteam.flightschool.repository.memory.InMemoryPruefungRepository;
import de.skyteam.flightschool.repository.memory.InMemorySchuelerRepository;
import de.skyteam.flightschool.repository.memory.InMemoryStudentRepository;
import de.skyteam.flightschool.repository.memory.InMemoryWartungRepository;
import java.util.List;

public record RepositoryProvider(
        String profile,
        StudentRepository students,
        AircraftRepository aircraft,
        LessonRepository lessons,
        SchuelerRepository schueler,
        AusbildungsVertragRepository ausbildungsVertraege,
        KursRepository kurse,
        FlugRepository fluege,
        PruefungRepository pruefungen,
        PilotRepository piloten,
        FlugzeugRepository flugzeuge,
        WartungRepository wartungen,
        AusbildungsStatusRepository ausbildungsStatus,
        JdbcConnectionFactory databaseConnections
) {
    private static final List<String> ORACLE_TABLES = List.of(
            "SCHUELER",
            "AUSBILDUNG_VERTRAG",
            "SCHULE",
            "KURSE",
            "PRUEFUNG",
            "FLUG",
            "PILOT",
            "FLUGZEUG",
            "WARTUNG",
            "SCHUELER_UND_PILOT",
            "FLUG_UND_PILOT",
            "WARTUNG_UND_FLUGZEUG"
    );

    public static RepositoryProvider fromEnvironment() {
        String profile = configuredProfile();
        if ("oracle".equalsIgnoreCase(profile)) {
            JdbcConnectionFactory connections = JdbcConnectionFactory.fromEnvironment();
            return new RepositoryProvider(
                    "oracle",
                    new InMemoryStudentRepository(),
                    new InMemoryAircraftRepository(),
                    new InMemoryLessonRepository(),
                    new JdbcSchuelerRepository(connections),
                    new JdbcAusbildungsVertragRepository(connections),
                    new JdbcKursRepository(connections),
                    new JdbcFlugRepository(connections),
                    new JdbcPruefungRepository(connections),
                    new JdbcPilotRepository(connections),
                    new JdbcFlugzeugRepository(connections),
                    new JdbcWartungRepository(connections),
                    new JdbcAusbildungsStatusRepository(connections),
                    connections
            );
        }
        if (!"dev".equalsIgnoreCase(profile)) {
            throw new IllegalArgumentException("Unsupported APP_PROFILE: " + profile);
        }
        DevFlightSchoolData data = new DevFlightSchoolData();
        return new RepositoryProvider(
                "dev",
                new InMemoryStudentRepository(),
                new InMemoryAircraftRepository(),
                new InMemoryLessonRepository(),
                new InMemorySchuelerRepository(data),
                new InMemoryAusbildungsVertragRepository(data),
                new InMemoryKursRepository(data),
                new InMemoryFlugRepository(data),
                new InMemoryPruefungRepository(data),
                new InMemoryPilotRepository(data),
                new InMemoryFlugzeugRepository(data),
                new InMemoryWartungRepository(data),
                new InMemoryAusbildungsStatusRepository(data),
                null
        );
    }

    public void verifyDatabaseConnection() {
        if (databaseConnections != null) {
            databaseConnections.verifyConnection();
        }
    }

    public boolean databaseConnected() {
        return databaseConnections != null && databaseConnections.isConnected();
    }

    public List<String> inspectDatabaseSchema() {
        return databaseConnections == null
                ? List.of()
                : databaseConnections.inspectSchema(ORACLE_TABLES);
    }

    private static String configuredProfile() {
        String profile = System.getProperty("app.profile");
        if (profile == null || profile.isBlank()) {
            profile = System.getenv("APP_PROFILE");
        }
        if (profile == null || profile.isBlank()) {
            return "dev";
        }
        return profile.trim();
    }
}


