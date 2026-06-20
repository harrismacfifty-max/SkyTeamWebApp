package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.AusbildungsStatus;
import java.util.Optional;

public interface AusbildungsStatusRepository {
    Optional<AusbildungsStatus> findBySchuelerId(String id);
}

