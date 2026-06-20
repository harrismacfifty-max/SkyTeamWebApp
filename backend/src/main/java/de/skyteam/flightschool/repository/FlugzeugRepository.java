package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Flugzeug;
import java.util.List;
import java.util.Optional;

public interface FlugzeugRepository {
    List<Flugzeug> findAll();

    Optional<Flugzeug> findById(String id);
}

