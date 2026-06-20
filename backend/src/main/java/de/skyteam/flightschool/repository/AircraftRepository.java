package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Aircraft;
import java.util.List;
import java.util.Optional;

public interface AircraftRepository {
    List<Aircraft> findAll();

    Optional<Aircraft> findById(long id);

    Aircraft create(Aircraft aircraft);
}


