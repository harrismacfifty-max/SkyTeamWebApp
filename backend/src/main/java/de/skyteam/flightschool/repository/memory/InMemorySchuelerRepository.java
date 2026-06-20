package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InMemorySchuelerRepository implements SchuelerRepository {
    private final DevFlightSchoolData data;

    public InMemorySchuelerRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Schueler> findAll() {
        return data.schueler.values().stream()
                .sorted(Comparator.comparing(Schueler::name).thenComparing(Schueler::vorname))
                .toList();
    }

    @Override
    public Optional<Schueler> findById(String id) {
        return Optional.ofNullable(data.schueler.get(id));
    }

    @Override
    public String nextId() {
        return data.nextSchuelerId();
    }

    @Override
    public Schueler save(Schueler schueler) {
        data.schueler.put(schueler.id(), schueler);
        data.persist();
        return schueler;
    }

    @Override
    public boolean deleteById(String id) {
        Schueler removed = data.schueler.remove(id);
        if (removed == null) {
            return false;
        }
        data.kurse.entrySet().removeIf(entry -> entry.getValue().schuelerId().equals(id));
        data.fluege.entrySet().removeIf(entry -> entry.getValue().schuelerId().equals(id));
        data.pruefungen.entrySet().removeIf(entry -> entry.getValue().schuelerId().equals(id));
        data.vertraege.remove(removed.ausbildungsVertragId());
        data.persist();
        return true;
    }
}
