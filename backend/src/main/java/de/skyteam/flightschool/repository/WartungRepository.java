package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Wartung;
import java.util.List;
import java.util.Optional;

public interface WartungRepository {
    List<Wartung> findAll();

    List<Wartung> findByFlugzeug(String flugzeugId);

    Optional<Wartung> findById(String id);
}

