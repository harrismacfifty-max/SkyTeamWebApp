package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.repository.FlugRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class InMemoryFlugRepository implements FlugRepository {
    private final DevFlightSchoolData data;

    public InMemoryFlugRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Flug> findBySchueler(String id) {
        return data.fluege.values().stream()
                .filter(flug -> flug.schuelerId().equals(id))
                .sorted(Comparator.comparing(Flug::datum).thenComparing(Flug::id))
                .toList();
    }

    @Override
    public Flug createPraxisFlug(PraxisBuchungRequest request) {
        String id = data.nextFlugId();
        LocalDateTime start = parseDateTime(request.termin());
        LocalDateTime end = start.plusMinutes(request.dauerMinuten());
        Flug flug = new Flug(
                id,
                request.schuelerId(),
                request.flugzeugId(),
                start,
                start,
                end,
                "EDDV",
                "EDDV",
                request.ausbildungsinhalt()
        );
        data.fluege.put(id, flug);
        data.addFlugStunden(request.schuelerId(), request.dauerMinuten() / 60.0);
        data.persist();
        return flug;
    }

    @Override
    public boolean stornierePraxisFlug(String schuelerId, String flugId) {
        Flug flug = data.fluege.get(flugId);
        if (flug == null || !flug.schuelerId().equals(schuelerId)) {
            return false;
        }
        data.fluege.remove(flugId);
        double hours = Math.max(0.0, java.time.Duration.between(flug.startzeit(), flug.endzeit()).toMinutes() / 60.0);
        data.addFlugStunden(schuelerId, -hours);
        data.persist();
        return true;
    }

    @Override
    public double countFlugstunden(String id) {
        return data.schueler.containsKey(id) ? data.schueler.get(id).flugStunden() : 0.0;
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
