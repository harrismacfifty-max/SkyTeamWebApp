package de.skyteam.flightschool.repository.memory;

import de.skyteam.flightschool.model.AusbildungsVertrag;
import de.skyteam.flightschool.model.Schueler;
import de.skyteam.flightschool.repository.AusbildungsVertragRepository;
import java.util.Optional;

public final class InMemoryAusbildungsVertragRepository implements AusbildungsVertragRepository {
    private final DevFlightSchoolData data;

    public InMemoryAusbildungsVertragRepository(DevFlightSchoolData data) {
        this.data = data;
    }

    @Override
    public Optional<AusbildungsVertrag> findBySchuelerId(String id) {
        return Optional.ofNullable(data.schueler.get(id))
                .map(Schueler::ausbildungsVertragId)
                .map(data.vertraege::get);
    }

    @Override
    public String nextId() {
        return data.nextAusbildungsVertragId();
    }

    @Override
    public AusbildungsVertrag save(AusbildungsVertrag vertrag) {
        data.vertraege.put(vertrag.id(), vertrag);
        data.persist();
        return vertrag;
    }

    @Override
    public void updateStatus(String id, String status) {
        AusbildungsVertrag current = data.vertraege.get(id);
        if (current == null) {
            return;
        }
        data.vertraege.put(id, new AusbildungsVertrag(
                current.id(),
                current.schuleId(),
                current.startzeit(),
                current.endzeit(),
                status,
                current.notiz()
        ));
        data.persist();
    }
}
