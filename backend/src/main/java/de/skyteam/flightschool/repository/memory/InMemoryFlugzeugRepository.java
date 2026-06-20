package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Flugzeug;
import de.skyteam.flightschool.repository.FlugzeugRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InMemoryFlugzeugRepository implements FlugzeugRepository {
    private final DevFlightSchoolData data;

    public InMemoryFlugzeugRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Flugzeug> findAll() {
        return data.flugzeuge.values().stream()
                .sorted(Comparator.comparing(Flugzeug::id))
                .toList();
    }

    @Override
    public Optional<Flugzeug> findById(String id) {
        return Optional.ofNullable(data.flugzeuge.get(id));
    }
}

