package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.AbschlussAnfrage;
import de.skyteam.flightschool.model.AbschlussAnfrageStatus;
import de.skyteam.flightschool.repository.AbschlussAnfrageRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class InMemoryAbschlussAnfrageRepository implements AbschlussAnfrageRepository {
    private final DevFlightSchoolData data;

    public InMemoryAbschlussAnfrageRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public Optional<AbschlussAnfrage> findById(String id) {
        return Optional.ofNullable(data.abschlussAnfragen.get(id));
    }

    @Override
    public Optional<AbschlussAnfrage> findLatestBySchuelerId(String schuelerId) {
        return data.abschlussAnfragen.values().stream()
                .filter(anfrage -> anfrage.schuelerId().equals(schuelerId))
                .max(Comparator.comparing(AbschlussAnfrage::angefragtAm, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @Override
    public List<AbschlussAnfrage> findAll() {
        return data.abschlussAnfragen.values().stream()
                .sorted(Comparator.comparing(AbschlussAnfrage::angefragtAm, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @Override
    public List<AbschlussAnfrage> findOffene() {
        return data.abschlussAnfragen.values().stream()
                .filter(this::isOffen)
                .sorted(Comparator.comparing(AbschlussAnfrage::angefragtAm, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    @Override
    public String nextId() {
        return data.nextAbschlussAnfrageId();
    }

    @Override
    public AbschlussAnfrage save(AbschlussAnfrage anfrage) {
        data.abschlussAnfragen.put(anfrage.id(), anfrage);
        data.persist();
        return anfrage;
    }

    private boolean isOffen(AbschlussAnfrage anfrage) {
        return anfrage.status() == AbschlussAnfrageStatus.ANGEFRAGT
                || anfrage.status() == AbschlussAnfrageStatus.IN_PRUEFUNG;
    }
}
