package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Wartung;
import de.skyteam.flightschool.repository.WartungRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InMemoryWartungRepository implements WartungRepository {
    private final DevFlightSchoolData data;

    public InMemoryWartungRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Wartung> findAll() {
        return data.wartungen.values().stream()
                .sorted(Comparator.comparing(Wartung::datum).thenComparing(Wartung::id))
                .toList();
    }

    @Override
    public List<Wartung> findByFlugzeug(String flugzeugId) {
        return data.wartungen.values().stream()
                .filter(wartung -> flugzeugId.equals(wartung.flugzeugId()))
                .sorted(Comparator.comparing(Wartung::datum).thenComparing(Wartung::id))
                .toList();
    }

    @Override
    public Optional<Wartung> findById(String id) {
        return Optional.ofNullable(data.wartungen.get(id));
    }
}

