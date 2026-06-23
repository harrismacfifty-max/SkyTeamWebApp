package de.skyteam.flightschool.repository;

import de.skyteam.flightschool.model.AbschlussAnfrage;
import java.util.List;
import java.util.Optional;

public interface AbschlussAnfrageRepository {
    Optional<AbschlussAnfrage> findById(String id);

    Optional<AbschlussAnfrage> findLatestBySchuelerId(String schuelerId);

    List<AbschlussAnfrage> findAll();

    List<AbschlussAnfrage> findOffene();

    String nextId();

    AbschlussAnfrage save(AbschlussAnfrage anfrage);
}
