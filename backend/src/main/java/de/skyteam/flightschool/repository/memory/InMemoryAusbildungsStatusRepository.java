package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.AusbildungsStatus;
import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.repository.AusbildungsStatusRepository;
import java.util.Optional;

public final class InMemoryAusbildungsStatusRepository implements AusbildungsStatusRepository {
    private final DevFlightSchoolData data;

    public InMemoryAusbildungsStatusRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public Optional<AusbildungsStatus> findBySchuelerId(String id) {
        Schueler schueler = data.schueler.get(id);
        if (schueler == null) {
            return Optional.empty();
        }
        AusbildungsVertrag vertrag = data.vertraege.get(schueler.ausbildungsVertragId());
        boolean theorieBestanden = hasBestandeneTheorie(id);
        boolean praxisBestanden = hasBestandenePraxis(id);
        return Optional.of(new AusbildungsStatus(
                schueler.id(),
                schueler.ausbildungsVertragId(),
                vertrag == null ? null : vertrag.status(),
                schueler.theorieStunden(),
                schueler.flugStunden(),
                theorieBestanden,
                praxisBestanden,
                schueler.theorieStunden() >= 120.0 && schueler.flugStunden() >= 45.0 && theorieBestanden
        ));
    }

    private boolean hasBestandeneTheorie(String id) {
        return data.pruefungen.values().stream()
                .filter(pruefung -> pruefung.schuelerId().equals(id))
                .map(pruefung -> pruefung.typ().toLowerCase())
                .anyMatch(typ -> typ.contains("theorie") && typ.contains("bestanden") && !typ.contains("nicht bestanden"));
    }

    private boolean hasBestandenePraxis(String id) {
        return data.pruefungen.values().stream()
                .filter(pruefung -> pruefung.schuelerId().equals(id))
                .map(pruefung -> pruefung.typ().toLowerCase())
                .anyMatch(typ -> (typ.contains("praxis") || typ.contains("praktisch")) && typ.contains("bestanden") && !typ.contains("nicht bestanden"));
    }
}

