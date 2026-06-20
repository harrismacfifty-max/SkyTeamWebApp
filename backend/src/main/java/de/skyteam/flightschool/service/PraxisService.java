package de.skyteam.flightschool.service;

import de.skyteam.flightschool.dto.PraxisBuchungRequest;
import de.skyteam.flightschool.dto.PraxisFortschrittDto;
import de.skyteam.flightschool.dto.PraxisStornierungRequest;
import de.skyteam.flightschool.dto.PraxisStornierungResponse;
import de.skyteam.flightschool.error.BusinessConflictException;
import de.skyteam.flightschool.error.NotFoundException;
import de.skyteam.flightschool.error.ValidationException;
import de.skyteam.flightschool.model.Flug;
import de.skyteam.flightschool.model.Flugzeug;
import de.skyteam.flightschool.model.Pilot;
import de.skyteam.flightschool.model.Wartung;
import de.skyteam.flightschool.repository.FlugRepository;
import de.skyteam.flightschool.repository.FlugzeugRepository;
import de.skyteam.flightschool.repository.PilotRepository;
import de.skyteam.flightschool.repository.SchuelerRepository;
import de.skyteam.flightschool.repository.WartungRepository;
import java.util.List;
import java.util.Locale;

public final class PraxisService {
    private final SchuelerRepository schuelerRepository;
    private final FlugRepository flugRepository;
    private final PilotRepository pilotRepository;
    private final FlugzeugRepository flugzeugRepository;
    private final WartungRepository wartungRepository;

    public PraxisService(
            SchuelerRepository schuelerRepository,
            FlugRepository flugRepository,
            PilotRepository pilotRepository,
            FlugzeugRepository flugzeugRepository,
            WartungRepository wartungRepository
    ) {
        this.schuelerRepository = schuelerRepository;
        this.flugRepository = flugRepository;
        this.pilotRepository = pilotRepository;
        this.flugzeugRepository = flugzeugRepository;
        this.wartungRepository = wartungRepository;
    }

    public List<Flug> fluege(String schuelerId) {
        requireSchueler(schuelerId);
        return flugRepository.findBySchueler(schuelerId);
    }

    public double flugstunden(String schuelerId) {
        requireSchueler(schuelerId);
        return flugRepository.countFlugstunden(schuelerId);
    }

    public PraxisFortschrittDto fortschritt(String schuelerId) {
        double stunden = flugstunden(schuelerId);
        return new PraxisFortschrittDto(
                schuelerId,
                stunden,
                TrainingRules.MINDEST_FLUGSTUNDEN,
                mindestflugstundenErreicht(schuelerId)
        );
    }

    public Flug bucheFlugstunde(PraxisBuchungRequest request) {
        validateRequest(request);
        requireSchueler(request.schuelerId());
        pruefeFluglehrerVerfuegbar(request.fluglehrer());
        pruefeFlugzeugVerfuegbar(request.flugzeugId());
        return flugRepository.createPraxisFlug(request);
    }

    public PraxisStornierungResponse storniereFlugstunde(PraxisStornierungRequest request) {
        if (request == null) {
            throw new ValidationException("Praxisstornierung ist erforderlich.");
        }
        requireSchueler(request.schuelerId());
        if (request.flugId() == null || request.flugId().isBlank()) {
            throw new ValidationException("flugId ist erforderlich.");
        }
        boolean storniert = flugRepository.stornierePraxisFlug(request.schuelerId(), request.flugId());
        if (!storniert) {
            throw new NotFoundException("Praxisflugstunde wurde nicht gefunden.");
        }
        return new PraxisStornierungResponse(
                request.schuelerId(),
                request.flugId(),
                true,
                "Praxisflugstunde wurde storniert."
        );
    }

    public boolean mindestflugstundenErreicht(String schuelerId) {
        return flugstunden(schuelerId) >= TrainingRules.MINDEST_FLUGSTUNDEN;
    }

    public boolean praxisPruefungFreigeschaltet(String schuelerId) {
        return mindestflugstundenErreicht(schuelerId);
    }

    public void pruefePraxisPruefungFreigeschaltet(String schuelerId) {
        double stunden = flugstunden(schuelerId);
        if (stunden < TrainingRules.MINDEST_FLUGSTUNDEN) {
            throw new BusinessConflictException(
                    "Praxispruefung ist noch nicht freigeschaltet: mindestens "
                            + TrainingRules.MINDEST_FLUGSTUNDEN
                            + " Flugstunden erforderlich, aktuell "
                            + stunden
                            + "."
            );
        }
    }

    public void pruefeFluglehrerVerfuegbar(String pilotId) {
        Pilot pilot = pilotRepository.findById(pilotId)
                .orElseThrow(() -> new NotFoundException("Fluglehrer wurde nicht gefunden."));
        if (!isJa(pilot.lehrer())) {
            throw new BusinessConflictException("Pilot " + pilotId + " ist nicht als Fluglehrer gekennzeichnet.");
        }
        if (!isJa(pilot.verfuegbar())) {
            throw new BusinessConflictException("Fluglehrer " + pilotId + " ist nicht verfuegbar.");
        }
    }

    public void pruefeFlugzeugVerfuegbar(String flugzeugId) {
        Flugzeug flugzeug = flugzeugRepository.findById(flugzeugId)
                .orElseThrow(() -> new NotFoundException("Flugzeug wurde nicht gefunden."));
        if (!isJa(flugzeug.verfuegbarkeit())) {
            throw new BusinessConflictException("Flugzeug " + flugzeugId + " ist nicht verfuegbar.");
        }
        String status = normalize(flugzeug.status());
        if (status.contains("wartung") || status.contains("ausser_dienst") || status.contains("gesperrt")) {
            throw new BusinessConflictException("Flugzeug " + flugzeugId + " ist wegen Status '" + flugzeug.status() + "' nicht buchbar.");
        }
        List<Wartung> wartungen = wartungRepository.findByFlugzeug(flugzeugId);
        if (wartungen.stream().anyMatch(this::istWartungsrelevant)) {
            throw new BusinessConflictException("Flugzeug " + flugzeugId + " gilt aktuell als wartungsrelevant.");
        }
    }

    private void requireSchueler(String schuelerId) {
        if (schuelerId == null || schuelerId.isBlank()) {
            throw new ValidationException("schuelerId ist erforderlich.");
        }
        schuelerRepository.findById(schuelerId)
                .orElseThrow(() -> new NotFoundException("Schueler wurde nicht gefunden."));
    }

    private static void validateRequest(PraxisBuchungRequest request) {
        if (request == null) {
            throw new ValidationException("Praxisbuchung ist erforderlich.");
        }
        if (request.fluglehrer() == null || request.fluglehrer().isBlank()) {
            throw new ValidationException("fluglehrer ist erforderlich.");
        }
        if (request.flugzeugId() == null || request.flugzeugId().isBlank()) {
            throw new ValidationException("flugzeugId ist erforderlich.");
        }
        if (request.termin() == null || request.termin().isBlank()) {
            throw new ValidationException("termin ist erforderlich.");
        }
        if (request.ausbildungsinhalt() == null || request.ausbildungsinhalt().isBlank()) {
            throw new ValidationException("ausbildungsinhalt ist erforderlich.");
        }
        if (request.dauerMinuten() <= 0) {
            throw new ValidationException("dauerMinuten muss groesser als 0 sein.");
        }
    }

    private boolean istWartungsrelevant(Wartung wartung) {
        String typ = normalize(wartung.typ());
        String notiz = normalize(wartung.notiz());
        return typ.contains("offen")
                || typ.contains("faellig")
                || typ.contains("kritisch")
                || notiz.contains("offen")
                || notiz.contains("faellig")
                || notiz.contains("kritisch");
    }

    private static boolean isJa(String value) {
        return "ja".equals(normalize(value)) || "true".equals(normalize(value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace("ß", "ss");
    }
}
