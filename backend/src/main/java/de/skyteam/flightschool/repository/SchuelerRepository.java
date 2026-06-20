package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.Schueler;
import java.util.List;
import java.util.Optional;

public interface SchuelerRepository {
    List<Schueler> findAll();

    Optional<Schueler> findById(String id);

    String nextId();

    Schueler save(Schueler schueler);

    boolean deleteById(String id);
}
