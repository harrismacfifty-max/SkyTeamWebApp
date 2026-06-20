package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.model.Flug;
import java.util.List;

public interface FlugRepository {
    List<Flug> findBySchueler(String id);

    Flug createPraxisFlug(PraxisBuchungRequest request);

    boolean stornierePraxisFlug(String schuelerId, String flugId);

    double countFlugstunden(String id);
}
