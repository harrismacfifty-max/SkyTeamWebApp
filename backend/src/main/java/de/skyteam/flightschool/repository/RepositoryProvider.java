package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.repository.jdbc.JdbcAircraftRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcAusbildungsStatusRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcAusbildungsVertragRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcConnectionFactory;
import de.skyteam.flightschool.repository.jdbc.JdbcFlugRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcFlugzeugRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcKursRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcLessonRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcPilotRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcPruefungRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcSchuelerRepository;
import de.skyteam.flightschool.repository.jdbc.JdbcStudentRepository;
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

public record RepositoryProvider(
        String profile,
        String databaseMode,
        DatabaseHealthCheck databaseHealthCheck,
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
        AusbildungsStatusRepository ausbildungsStatus
) {
    @FunctionalInterface
    public interface DatabaseHealthCheck {
        boolean isReachable();
    }

    public static RepositoryProvider fromEnvironment() {
        String profile = configuredProfile();
        if ("oracle".equalsIgnoreCase(profile)) {
            JdbcConnectionFactory connections = JdbcConnectionFactory.fromEnvironment();
            return new RepositoryProvider(
                    "oracle",
                    "oracle",
                    connections::isReachable,
                    new JdbcStudentRepository(connections),
                    new JdbcAircraftRepository(connections),
                    new JdbcLessonRepository(connections),
                    new JdbcSchuelerRepository(connections),
                    new JdbcAusbildungsVertragRepository(connections),
                    new JdbcKursRepository(connections),
                    new JdbcFlugRepository(connections),
                    new JdbcPruefungRepository(connections),
                    new JdbcPilotRepository(connections),
                    new JdbcFlugzeugRepository(connections),
                    new JdbcWartungRepository(connections),
                    new JdbcAusbildungsStatusRepository(connections)
            );
        }
        if (!"dev".equalsIgnoreCase(profile) && !"demo".equalsIgnoreCase(profile)) {
            throw new IllegalArgumentException("Unsupported APP_PROFILE: " + profile);
        }
        DevFlightSchoolData data = new DevFlightSchoolData();
        return new RepositoryProvider(
                "demo",
                "demo",
                () -> true,
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
                new InMemoryAusbildungsStatusRepository(data)
        );
    }

    private static String configuredProfile() {
        String profile = System.getProperty("app.profile");
        if (profile == null || profile.isBlank()) {
            profile = System.getenv("APP_PROFILE");
        }
        if (profile == null || profile.isBlank()) {
            return "demo";
        }
        return profile.trim();
    }

    public boolean databaseReachable() {
        try {
            return databaseHealthCheck.isReachable();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}


