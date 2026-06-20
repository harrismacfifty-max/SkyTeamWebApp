package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.TheorieBuchungRequest;
import de.skyteam.flightschool.dto.TheorieFortschrittDto;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.Kurs;
import de.skyteam.flightschool.repository.KursRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.util.List;

public final class TheorieService {
    private final SchuelerRepository schuelerRepository;
    private final KursRepository kursRepository;

    public TheorieService(SchuelerRepository schuelerRepository, KursRepository kursRepository) {
        this.schuelerRepository = schuelerRepository;
        this.kursRepository = kursRepository;
    }

    public List<Kurs> kurse(String schuelerId) {
        requireSchueler(schuelerId);
        return kursRepository.findBySchueler(schuelerId);
    }

    public double theoriestunden(String schuelerId) {
        requireSchueler(schuelerId);
        return kursRepository.countTheorieStunden(schuelerId);
    }

    public TheorieFortschrittDto fortschritt(String schuelerId) {
        double stunden = theoriestunden(schuelerId);
        return new TheorieFortschrittDto(
                schuelerId,
                stunden,
                TrainingRules.MINDEST_THEORIESTUNDEN,
                mindestanzahlErreicht(schuelerId)
        );
    }

    public Kurs bucheTheoriekurs(TheorieBuchungRequest request) {
        validateRequest(request);
        requireSchueler(request.schuelerId());
        return kursRepository.createTheorieKurs(request);
    }

    public boolean mindestanzahlErreicht(String schuelerId) {
        return theoriestunden(schuelerId) >= TrainingRules.MINDEST_THEORIESTUNDEN;
    }

    public boolean theoriePruefungFreigeschaltet(String schuelerId) {
        return mindestanzahlErreicht(schuelerId);
    }

    public void pruefeTheoriePruefungFreigeschaltet(String schuelerId) {
        double stunden = theoriestunden(schuelerId);
        if (stunden < TrainingRules.MINDEST_THEORIESTUNDEN) {
            throw new BusinessConflictException(
                    "Theoriepruefung ist noch nicht freigeschaltet: mindestens "
                            + TrainingRules.MINDEST_THEORIESTUNDEN
                            + " Theoriestunden erforderlich, aktuell "
                            + stunden
                            + "."
            );
        }
    }

    private void requireSchueler(String schuelerId) {
        if (schuelerId == null || schuelerId.isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        schuelerRepository.findById(schuelerId)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
    }

    private static void validateRequest(TheorieBuchungRequest request) {
        if (request == null) {
            throw new ValidationException("Theoriebuchung ist erforderlich.");
        }
        if (request.thema() == null || request.thema().isBlank()) {
            throw new ValidationException("thema ist erforderlich.");
        }
        if (request.termin() == null || request.termin().isBlank()) {
            throw new ValidationException("termin ist erforderlich.");
        }
        if (request.dozent() == null || request.dozent().isBlank()) {
            throw new ValidationException("dozent ist erforderlich.");
        }
        if (request.dauerMinuten() <= 0) {
            throw new ValidationException("dauerMinuten muss groesser als 0 sein.");
        }
    }
}

