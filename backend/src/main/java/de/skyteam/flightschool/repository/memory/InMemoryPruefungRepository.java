package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.repository.PruefungRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class InMemoryPruefungRepository implements PruefungRepository {
    private final DevFlightSchoolData data;

    public InMemoryPruefungRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Pruefung> findBySchueler(String id) {
        return data.pruefungen.values().stream()
                .filter(pruefung -> pruefung.schuelerId().equals(id))
                .sorted(Comparator.comparing(Pruefung::datum).thenComparing(Pruefung::id))
                .toList();
    }

    @Override
    public Pruefung createPruefung(PruefungAnmeldungRequest request) {
        String id = data.nextPruefungId();
        Pruefung pruefung = new Pruefung(id, request.schuelerId(), parseDateTime(request.wunschtermin()), request.pruefungsart());
        data.pruefungen.put(id, pruefung);
        data.persist();
        return pruefung;
    }

    @Override
    public void saveErgebnis(PruefungsErgebnisRequest request) {
        Pruefung current = data.pruefungen.get(request.pruefungId());
        if (current == null) {
            return;
        }
        String status = request.bestanden() ? "bestanden" : "nicht bestanden";
        String typ = request.pruefungsart() == null || request.pruefungsart().isBlank()
                ? current.typ() + " - " + status
                : request.pruefungsart().trim() + " - " + status;
        data.pruefungen.put(request.pruefungId(), new Pruefung(
                request.pruefungId(),
                current.schuelerId(),
                parseDateTime(request.datum()),
                typ
        ));
        data.persist();
    }

    @Override
    public boolean hasBestandeneTheorie(String id) {
        return findBySchueler(id).stream()
                .map(pruefung -> pruefung.typ().toLowerCase())
                .anyMatch(typ -> typ.contains("theorie") && typ.contains("bestanden") && !typ.contains("nicht bestanden"));
    }

    @Override
    public boolean hasBestandenePraxis(String id) {
        return findBySchueler(id).stream()
                .map(pruefung -> pruefung.typ().toLowerCase())
                .anyMatch(typ -> (typ.contains("praxis") || typ.contains("praktisch")) && typ.contains("bestanden") && !typ.contains("nicht bestanden"));
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed);
    }
}
