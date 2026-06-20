package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Pilot;
import java.util.List;
import java.util.Optional;

public interface PilotRepository {
    List<Pilot> findAll();

    Optional<Pilot> findById(String id);
}

