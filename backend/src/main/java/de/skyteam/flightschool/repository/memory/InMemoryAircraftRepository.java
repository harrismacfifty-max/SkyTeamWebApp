package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Aircraft;
import de.skyteam.flightschool.repository.AircraftRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryAircraftRepository implements AircraftRepository {
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Aircraft> aircraft = new ConcurrentHashMap<>();

    public InMemoryAircraftRepository() {
        create(new Aircraft(0, "D-ESKY", "Cessna 172", "Verfuegbar", 1240));
        create(new Aircraft(0, "D-ETEA", "Piper PA-28", "Wartung geplant", 980));
    }

    @Override
    public List<Aircraft> findAll() {
        return aircraft.values().stream()
                .sorted(Comparator.comparing(Aircraft::registration))
                .toList();
    }

    @Override
    public Optional<Aircraft> findById(long id) {
        return Optional.ofNullable(aircraft.get(id));
    }

    @Override
    public Aircraft create(Aircraft aircraftToCreate) {
        long id = ids.incrementAndGet();
        Aircraft saved = new Aircraft(
                id,
                aircraftToCreate.registration(),
                aircraftToCreate.model(),
                aircraftToCreate.status(),
                aircraftToCreate.totalHours()
        );
        aircraft.put(id, saved);
        return saved;
    }
}


