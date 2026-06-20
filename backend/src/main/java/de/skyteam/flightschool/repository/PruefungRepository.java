package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.model.Pruefung;
import java.util.List;

public interface PruefungRepository {
    List<Pruefung> findBySchueler(String id);

    Pruefung createPruefung(PruefungAnmeldungRequest request);

    void saveErgebnis(PruefungsErgebnisRequest request);

    boolean hasBestandeneTheorie(String id);

    boolean hasBestandenePraxis(String id);
}

