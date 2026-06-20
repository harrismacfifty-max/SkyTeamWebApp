package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Pilot;
import de.skyteam.flightschool.repository.PilotRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InMemoryPilotRepository implements PilotRepository {
    private final DevFlightSchoolData data;

    public InMemoryPilotRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Pilot> findAll() {
        return data.piloten.values().stream()
                .sorted(Comparator.comparing(Pilot::name).thenComparing(Pilot::vorname))
                .toList();
    }

    @Override
    public Optional<Pilot> findById(String id) {
        return Optional.ofNullable(data.piloten.get(id));
    }
}

