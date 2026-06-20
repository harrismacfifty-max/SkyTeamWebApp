package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.PruefungAnmeldungRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisRequest;
import de.skyteam.flightschool.dto.PruefungsErgebnisStatusDto;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.Pruefung;
import de.skyteam.flightschool.repository.PruefungRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import java.util.List;

public final class PruefungsService {
    private final SchuelerRepository schuelerRepository;
    private final PruefungRepository pruefungRepository;
    private final TheorieService theorieService;
    private final PraxisService praxisService;

    public PruefungsService(
            SchuelerRepository schuelerRepository,
            PruefungRepository pruefungRepository,
            TheorieService theorieService,
            PraxisService praxisService
    ) {
        this.schuelerRepository = schuelerRepository;
        this.pruefungRepository = pruefungRepository;
        this.theorieService = theorieService;
        this.praxisService = praxisService;
    }

    public List<Pruefung> pruefungen(String schuelerId) {
        requireSchueler(schuelerId);
        return pruefungRepository.findBySchueler(schuelerId);
    }

    public Pruefung meldeTheoriePruefungAn(PruefungAnmeldungRequest request) {
        validateAnmeldung(request);
        requireSchueler(request.schuelerId());
        theorieService.pruefeTheoriePruefungFreigeschaltet(request.schuelerId());
        return pruefungRepository.createPruefung(normalizePruefungsart(request, "Theoriepruefung"));
    }

    public Pruefung meldePraxisPruefungAn(PruefungAnmeldungRequest request) {
        validateAnmeldung(request);
        requireSchueler(request.schuelerId());
        praxisService.pruefePraxisPruefungFreigeschaltet(request.schuelerId());
        return pruefungRepository.createPruefung(normalizePruefungsart(request, "Praxispruefung"));
    }

    public PruefungsErgebnisStatusDto speichereErgebnis(PruefungsErgebnisRequest request) {
        validateErgebnis(request);
        PruefungsErgebnisRequest effectiveRequest = request.bestanden()
                ? request
                : new PruefungsErgebnisRequest(
                        request.pruefungId(),
                        request.schuelerId(),
                        markiereWiederholungsbedarf(request.pruefungsart()),
                        request.datum(),
                        false,
                        request.ergebnisText(),
                        request.notizen()
                );
        pruefungRepository.saveErgebnis(effectiveRequest);
        boolean wiederholungsbedarf = !request.bestanden();
        return new PruefungsErgebnisStatusDto(
                request.pruefungId(),
                request.bestanden(),
                wiederholungsbedarf,
                wiederholungsbedarf
                        ? "Pruefung nicht bestanden; Wiederholungsbedarf wurde markiert."
                        : "Pruefung bestanden."
        );
    }

    public boolean theorieBestanden(String schuelerId) {
        requireSchueler(schuelerId);
        return pruefungRepository.hasBestandeneTheorie(schuelerId);
    }

    public boolean praxisBestanden(String schuelerId) {
        requireSchueler(schuelerId);
        return pruefungRepository.hasBestandenePraxis(schuelerId);
    }

    private void requireSchueler(String schuelerId) {
        if (schuelerId == null || schuelerId.isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        schuelerRepository.findById(schuelerId)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
    }

    private static void validateAnmeldung(PruefungAnmeldungRequest request) {
        if (request == null) {
            throw new ValidationException("Pruefungsanmeldung ist erforderlich.");
        }
        if (request.wunschtermin() == null || request.wunschtermin().isBlank()) {
            throw new ValidationException("wunschtermin ist erforderlich.");
        }
    }

    private void validateErgebnis(PruefungsErgebnisRequest request) {
        if (request == null) {
            throw new ValidationException("Pruefungsergebnis ist erforderlich.");
        }
        if (request.pruefungId() == null || request.pruefungId().isBlank()) {
            throw new ValidationException("pruefungId ist erforderlich.");
        }
        if (request.schuelerId() == null || request.schuelerId().isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        if (request.datum() == null || request.datum().isBlank()) {
            throw new ValidationException("datum ist erforderlich.");
        }
        boolean exists = pruefungRepository.findBySchueler(request.schuelerId()).stream()
                .anyMatch(pruefung -> pruefung.id().equals(request.pruefungId()));
        if (!exists) {
            throw new NotFoundException("Pruefung wurde fuer diesen Schueler nicht gefunden.");
        }
    }

    private static PruefungAnmeldungRequest normalizePruefungsart(PruefungAnmeldungRequest request, String fallback) {
        String pruefungsart = request.pruefungsart();
        if (pruefungsart == null || pruefungsart.isBlank()) {
            pruefungsart = fallback;
        }
        return new PruefungAnmeldungRequest(
                request.schuelerId(),
                pruefungsart,
                request.wunschtermin(),
                request.pruefer(),
                request.bemerkung()
        );
    }

    private static String markiereWiederholungsbedarf(String pruefungsart) {
        String base = pruefungsart == null || pruefungsart.isBlank() ? "Pruefung" : pruefungsart.trim();
        if (base.toLowerCase().contains("wiederholungsbedarf")) {
            return base;
        }
        return base + " - Wiederholungsbedarf";
    }
}
