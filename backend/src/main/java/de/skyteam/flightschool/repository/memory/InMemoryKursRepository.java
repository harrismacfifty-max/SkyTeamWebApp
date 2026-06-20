package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.repository.KursRepository;
import java.util.Comparator;
import java.util.List;

public final class InMemoryKursRepository implements KursRepository {
    private final DevFlightSchoolData data;

    public InMemoryKursRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public List<Kurs> findBySchueler(String id) {
        return data.kurse.values().stream()
                .filter(kurs -> kurs.schuelerId().equals(id))
                .sorted(Comparator.comparing(Kurs::tag).thenComparing(Kurs::id))
                .toList();
    }

    @Override
    public Kurs createTheorieKurs(TheorieBuchungRequest request) {
        String id = data.nextKursId();
        Kurs kurs = new Kurs(id, request.schuelerId(), request.thema(), request.dozent(), request.termin());
        data.kurse.put(id, kurs);
        data.addTheorieStunden(request.schuelerId(), request.dauerMinuten() / 60.0);
        data.persist();
        return kurs;
    }

    @Override
    public double countTheorieStunden(String id) {
        return data.schueler.containsKey(id) ? data.schueler.get(id).theorieStunden() : 0.0;
    }
}
