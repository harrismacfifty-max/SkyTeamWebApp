package de.skyteam.flightschool.service;

import de.skyteam.flightschool.repository.AusbildungsStatusRepository;
import de.skyteam.flightschool.repository.AusbildungsVertragRepository;
import de.skyteam.flightschool.repository.AbschlussAnfrageRepository;
import de.skyteam.flightschool.repository.FlugRepository;
import de.skyteam.flightschool.repository.FlugzeugRepository;
import de.skyteam.flightschool.repository.KursRepository;
import de.skyteam.flightschool.repository.PilotRepository;
import de.skyteam.flightschool.repository.PruefungRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import de.skyteam.flightschool.repository.WartungRepository;
import de.skyteam.flightschool.repository.memory.DevFlightSchoolData;
import de.skyteam.flightschool.repository.memory.InMemoryAbschlussAnfrageRepository;
import de.skyteam.flightschool.repository.memory.InMemoryAusbildungsStatusRepository;
import de.skyteam.flightschool.repository.memory.InMemoryAusbildungsVertragRepository;
import de.skyteam.flightschool.repository.memory.InMemoryFlugRepository;
import de.skyteam.flightschool.repository.memory.InMemoryFlugzeugRepository;
import de.skyteam.flightschool.repository.memory.InMemoryKursRepository;
import de.skyteam.flightschool.repository.memory.InMemoryPilotRepository;
import de.skyteam.flightschool.repository.memory.InMemoryPruefungRepository;
import de.skyteam.flightschool.repository.memory.InMemorySchuelerRepository;
import de.skyteam.flightschool.repository.memory.InMemoryWartungRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

final class TestContext implements AutoCloseable {
    final SchuelerRepository schuelerRepository;
    final AusbildungsVertragRepository ausbildungsVertragRepository;
    final KursRepository kursRepository;
    final FlugRepository flugRepository;
    final PruefungRepository pruefungRepository;
    final PilotRepository pilotRepository;
    final FlugzeugRepository flugzeugRepository;
    final WartungRepository wartungRepository;
    final AusbildungsStatusRepository ausbildungsStatusRepository;
    final AbschlussAnfrageRepository abschlussAnfrageRepository;
    final SchuelerService schuelerService;
    final TheorieService theorieService;
    final PraxisService praxisService;
    final PruefungsService pruefungsService;
    final AusbildungsstatusService ausbildungsstatusService;
    final AbschlussService abschlussService;
    private final Path dataFile;

    private TestContext(Path dataFile, DevFlightSchoolData data) {
        this.dataFile = dataFile;
        this.schuelerRepository = new InMemorySchuelerRepository(data);
        this.ausbildungsVertragRepository = new InMemoryAusbildungsVertragRepository(data);
        this.kursRepository = new InMemoryKursRepository(data);
        this.flugRepository = new InMemoryFlugRepository(data);
        this.pruefungRepository = new InMemoryPruefungRepository(data);
        this.pilotRepository = new InMemoryPilotRepository(data);
        this.flugzeugRepository = new InMemoryFlugzeugRepository(data);
        this.wartungRepository = new InMemoryWartungRepository(data);
        this.ausbildungsStatusRepository = new InMemoryAusbildungsStatusRepository(data);
        this.abschlussAnfrageRepository = new InMemoryAbschlussAnfrageRepository(data);
        this.schuelerService = new SchuelerService(schuelerRepository, ausbildungsVertragRepository);
        this.theorieService = new TheorieService(schuelerRepository, kursRepository);
        this.praxisService = new PraxisService(
                schuelerRepository,
                flugRepository,
                pilotRepository,
                flugzeugRepository,
                wartungRepository
        );
        this.pruefungsService = new PruefungsService(
                schuelerRepository,
                pruefungRepository,
                theorieService,
                praxisService
        );
        this.ausbildungsstatusService = new AusbildungsstatusService(
                schuelerRepository,
                ausbildungsStatusRepository,
                ausbildungsVertragRepository
        );
        this.abschlussService = new AbschlussService(
                abschlussAnfrageRepository,
                schuelerRepository,
                ausbildungsstatusService
        );
    }

    static TestContext create() throws IOException {
        Path dataFile = Path.of("backend", "target", "test-data", "flight-school-test-" + UUID.randomUUID() + ".properties")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(dataFile.getParent());
        Files.deleteIfExists(dataFile);
        String previous = System.getProperty("dev.data.file");
        System.setProperty("dev.data.file", dataFile.toString());
        try {
            return new TestContext(dataFile, new DevFlightSchoolData());
        } finally {
            if (previous == null) {
                System.clearProperty("dev.data.file");
            } else {
                System.setProperty("dev.data.file", previous);
            }
        }
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(dataFile);
    }
}
