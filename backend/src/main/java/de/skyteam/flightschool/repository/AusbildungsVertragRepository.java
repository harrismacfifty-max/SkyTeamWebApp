package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.AusbildungsVertrag;
import java.util.Optional;

public interface AusbildungsVertragRepository {
    Optional<AusbildungsVertrag> findBySchuelerId(String id);

    String nextId();

    AusbildungsVertrag save(AusbildungsVertrag vertrag);

    void updateStatus(String id, String status);
}
