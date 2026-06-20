package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.model.Kurs;
import java.util.List;

public interface KursRepository {
    List<Kurs> findBySchueler(String id);

    Kurs createTheorieKurs(TheorieBuchungRequest request);

    double countTheorieStunden(String id);
}

